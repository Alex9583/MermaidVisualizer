package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.settings.MERMAID_SETTINGS_TOPIC
import com.alextdev.mermaidvisualizer.settings.MermaidSettings
import com.alextdev.mermaidvisualizer.settings.MermaidSettingsListener
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.application.EDT
import kotlin.time.Duration.Companion.milliseconds
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ide.ui.LafManagerListener
import com.intellij.psi.PsiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants

private val LOG = Logger.getInstance("MermaidPreviewFileEditor")

private const val SCROLL_GUARD_RESET_MS = 100L

private enum class ScrollOrigin { NONE, EDITOR, PREVIEW }

internal class MermaidPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)
    private var debounceJob: Job? = null
    private var debounceMs = service<MermaidSettings>().state.debounceMs
    private val panel: MermaidPreviewPanel?
    private val document = FileDocumentManager.getInstance().getDocument(file)
    private val mainComponent: JComponent
    private var editor: Editor? = null
    private var scrollOrigin = ScrollOrigin.NONE
    private var scrollGuardResetJob: Job? = null
    private var renderGeneration = 0L

    init {
        if (document == null) {
            LOG.warn("Could not obtain document for file: ${file.path}. Preview will not be available.")
        }

        if (MermaidPreviewPanel.isAvailable()) {
            panel = MermaidPreviewPanel(project, this)
            mainComponent = panel.component
            panel.setErrorCallback { handleRenderResult(it) }
        } else {
            panel = null
            mainComponent = JLabel(
                MyMessageBundle.message("editor.jcef.not.supported"),
                SwingConstants.CENTER,
            )
        }

        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                scheduleUpdate()
            }
        }, this)

        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
            scheduleUpdate(forceThemeRefresh = true)
        })
        // Settings changes apply immediately (no debounce) since the user explicitly confirmed via "Apply"
        connection.subscribe(MERMAID_SETTINGS_TOPIC, MermaidSettingsListener {
            debounceMs = service<MermaidSettings>().state.debounceMs
            updatePreview(forceThemeRefresh = true)
        })

        // Initial render — no debounce needed on first open
        updatePreview()
    }

    private fun scheduleUpdate(forceThemeRefresh: Boolean = false) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs.milliseconds)
            updatePreview(forceThemeRefresh)
        }
    }

    private fun updatePreview(forceThemeRefresh: Boolean = false) {
        val source = document?.text ?: return
        val isDark = EditorColorsManager.getInstance().isDarkEditor
        val generation = ++renderGeneration

        // Clear stale render error immediately so the annotation disappears
        // as soon as a new render starts, not when it finishes
        if (file.getMermaidRenderError() != null) {
            file.setMermaidRenderError(null)
            restartAnalyzer()
        }

        panel?.render(source, isDark, forceThemeRefresh, generation)
    }

    private fun handleRenderResult(jsonData: String) {
        val json = try {
            JsonParser.parseString(jsonData).asJsonObject
        } catch (e: JsonParseException) {
            LOG.warn("Failed to parse render result JSON", e)
            return
        } catch (e: IllegalStateException) {
            LOG.warn("Render result is not a JSON object", e)
            return
        }

        val generation = json.get("gen")?.asLong
        if (generation == null) {
            LOG.warn("Render result missing 'gen' field")
            return
        }
        if (generation != renderGeneration) return // stale result

        val status = json.get("status")?.asString
        if (status == "ok") {
            file.setMermaidRenderError(null)
        } else {
            val message = json.get("message")?.asString
                ?: MyMessageBundle.message("render.error.unknown")
            val line = try { json.get("line")?.takeIf { !it.isJsonNull }?.asInt?.takeIf { it >= 1 } } catch (_: NumberFormatException) { null }
            val column = try { json.get("column")?.takeIf { !it.isJsonNull }?.asInt?.takeIf { it >= 1 } } catch (_: NumberFormatException) { null }
            file.setMermaidRenderError(MermaidRenderError(message, line, column))
        }

        restartAnalyzer()
    }

    private fun restartAnalyzer() {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        @Suppress("DEPRECATION")
        DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
    }

    fun attachEditor(editor: Editor) {
        check(this.editor == null) { "Editor already attached" }
        this.editor = editor
        editor.scrollingModel.addVisibleAreaListener({ onEditorScroll() }, this)
        panel?.setScrollCallback { fraction -> onPreviewScroll(fraction) }
    }

    private fun editorScrollableHeight(): Int {
        val ed = editor ?: return 0
        return ed.contentComponent.height - ed.scrollingModel.visibleArea.height
    }

    private fun onEditorScroll() {
        if (scrollOrigin == ScrollOrigin.PREVIEW) return
        if (!file.isValid) return
        val ed = editor ?: return
        val scrollableHeight = editorScrollableHeight()
        if (scrollableHeight <= 0) return
        val fraction = ed.scrollingModel.verticalScrollOffset.toDouble() / scrollableHeight
        scrollOrigin = ScrollOrigin.EDITOR
        panel?.scrollToFraction(fraction)
        scheduleScrollGuardReset()
    }

    private fun onPreviewScroll(fraction: Double) {
        if (scrollOrigin == ScrollOrigin.EDITOR) return
        if (!file.isValid) return
        val ed = editor ?: return
        val scrollableHeight = editorScrollableHeight()
        if (scrollableHeight <= 0) return
        scrollOrigin = ScrollOrigin.PREVIEW
        val targetOffset = (fraction.coerceIn(0.0, 1.0) * scrollableHeight).toInt()
        ed.scrollingModel.scrollVertically(targetOffset)
        scheduleScrollGuardReset()
    }

    private fun scheduleScrollGuardReset() {
        scrollGuardResetJob?.cancel()
        scrollGuardResetJob = scope.launch {
            delay(SCROLL_GUARD_RESET_MS.milliseconds)
            scrollOrigin = ScrollOrigin.NONE
        }
    }

    override fun getComponent(): JComponent = mainComponent

    override fun getPreferredFocusedComponent(): JComponent = component

    override fun getName(): String = MyMessageBundle.message("editor.preview.name")

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getFile(): VirtualFile = file

    override fun dispose() {
        scope.cancel()
        file.setMermaidRenderError(null)
        LOG.debug("Disposing MermaidPreviewFileEditor for ${file.path}")
    }
}