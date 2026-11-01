package com.alextdev.mermaidvisualizer.welcome

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.openExternalLink
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.concurrency.ThreadingAssertions
import org.intellij.plugins.markdown.lang.MarkdownFileType

private val POST_STARTUP_ACTIVITY_EP = ExtensionPointName<Any>("com.intellij.postStartupActivity")

/** Sticky balloon group (plugin.xml): the fallback notification must not vanish before it is read. */
private const val WELCOME_NOTIFICATION_GROUP_ID = "Mermaid Visualizer Welcome"

internal const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/alextdev"
internal const val GITHUB_REPOSITORY_URL = "https://github.com/Alex9583/MermaidVisualizer"
internal const val GITHUB_ISSUES_URL = "https://github.com/Alex9583/MermaidVisualizer/issues"
internal const val GITHUB_CHANGELOG_URL = "https://github.com/Alex9583/MermaidVisualizer/blob/master/CHANGELOG.md"
internal const val MARKETPLACE_REVIEWS_URL = "https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer/reviews"

/**
 * Installed plugin version, or null if it cannot be determined. Read from the descriptor the platform injects
 * into the XML-registered [MermaidWelcomeStartupActivity] (public `PluginAware` API): every descriptor lookup by
 * plugin id (`PluginManagerCore.getPlugin`, `PluginManager.findEnabledPlugin`) is internal API from 2026.2 on.
 */
internal fun currentPluginVersion(): String? =
    POST_STARTUP_ACTIVITY_EP.findExtension(MermaidWelcomeStartupActivity::class.java)?.pluginVersion

/**
 * Opens the welcome / what's new page for [version], listing the changes since [previousShown]
 * (null = the current version only). EDT-only. The page is a Markdown [LightVirtualFile] shown preview-only by
 * the Markdown plugin, whose ```mermaid blocks this plugin renders. Without JCEF the Markdown preview cannot
 * show it, so a sticky notification carries the support and feedback links instead.
 */
internal fun openWelcomePage(
    project: Project,
    version: String,
    previousShown: String?,
    previewSupported: Boolean = JBCefApp.isSupported(),
) {
    ThreadingAssertions.assertEventDispatchThread()
    if (project.isDisposed) return
    if (!previewSupported) {
        notifyWelcomeWithoutPreview(project, version)
        return
    }
    project.service<MermaidWelcomePageService>().open(version, previousShown)
}

/** The read-only Markdown file holding the page; its tab is named after the bundle key `welcome.tab.name`. */
internal fun createWelcomePageFile(content: String): LightVirtualFile =
    LightVirtualFile("${MyMessageBundle.message("welcome.tab.name")}.md", MarkdownFileType.INSTANCE, content).apply {
        isWritable = false
    }

/**
 * Keeps at most one welcome tab per project: opening the page again focuses the existing tab instead of
 * stacking copies (same pattern as `MermaidDiagramTabService`). [TextEditorWithPreview.openPreviewForFile] puts
 * the platform DEFAULT_LAYOUT_FOR_FILE key on the file, so the Markdown editor opens in preview-only layout.
 * EDT-only, no synchronization needed.
 */
@Service(Service.Level.PROJECT)
internal class MermaidWelcomePageService(private val project: Project) {

    private var welcomeFile: LightVirtualFile? = null

    fun open(version: String, previousShown: String?) {
        ThreadingAssertions.assertEventDispatchThread()
        val fileEditorManager = FileEditorManager.getInstance(project)
        val file = welcomeFile?.takeIf { fileEditorManager.isFileOpen(it) }
            ?: createWelcomePageFile(loadWelcomePage(version, previousShown)).also { welcomeFile = it }
        TextEditorWithPreview.openPreviewForFile(project, file)
    }
}

private fun notifyWelcomeWithoutPreview(project: Project, version: String) {
    val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup(WELCOME_NOTIFICATION_GROUP_ID)
        .createNotification(
            MyMessageBundle.message("welcome.notification.title", version),
            MyMessageBundle.message("welcome.notification.content"),
            NotificationType.INFORMATION,
        )
    notification.addAction(linkAction(MyMessageBundle.message("welcome.notification.action.coffee"), BUY_ME_A_COFFEE_URL))
    notification.addAction(linkAction(MyMessageBundle.message("welcome.notification.action.changelog"), GITHUB_CHANGELOG_URL))
    notification.addAction(linkAction(MyMessageBundle.message("welcome.notification.action.star"), GITHUB_REPOSITORY_URL))
    notification.addAction(linkAction(MyMessageBundle.message("welcome.notification.action.rate"), MARKETPLACE_REVIEWS_URL))
    notification.addAction(linkAction(MyMessageBundle.message("welcome.notification.action.issue"), GITHUB_ISSUES_URL))
    notification.notify(project)
}

private fun linkAction(text: String, url: String): NotificationAction =
    NotificationAction.createSimple(text) { openExternalLink(url) }
