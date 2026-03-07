package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
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

private const val DEBOUNCE_MS = 300L

internal class MermaidPreviewFileEditor(
    private val project: Project,
    private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)
    private var debounceJob: Job? = null
    private val panel: MermaidPreviewPanel?
    private val document = FileDocumentManager.getInstance().getDocument(file)
    private val mainComponent: JComponent

    init {
        if (document == null) {
            LOG.warn("Could not obtain document for file: ${file.path}. Preview will not be available.")
        }

        if (MermaidPreviewPanel.isAvailable()) {
            panel = MermaidPreviewPanel(this)
            mainComponent = panel.component
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

        project.messageBus.connect(this).subscribe(LafManagerListener.TOPIC, LafManagerListener {
            scheduleUpdate(forceThemeRefresh = true)
        })

        // Initial render — no debounce needed on first open
        updatePreview()
    }

    private fun scheduleUpdate(forceThemeRefresh: Boolean = false) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            updatePreview(forceThemeRefresh)
        }
    }

    private fun updatePreview(forceThemeRefresh: Boolean = false) {
        val source = document?.text ?: return
        val isDark = EditorColorsManager.getInstance().isDarkEditor
        panel?.render(source, isDark, forceThemeRefresh)
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
        LOG.debug("Disposing MermaidPreviewFileEditor for ${file.path}")
    }
}