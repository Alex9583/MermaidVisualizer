package com.alextdev.mermaidvisualizer.markdown

import com.alextdev.mermaidvisualizer.copyErrorToClipboardFromBase64
import com.alextdev.mermaidvisualizer.copyPngToClipboard
import com.alextdev.mermaidvisualizer.copySvgToClipboard
import com.alextdev.mermaidvisualizer.openDiagramInNewTabFromBase64
import com.alextdev.mermaidvisualizer.openExternalLink
import com.alextdev.mermaidvisualizer.reportErrorOnGitHubFromBase64
import com.alextdev.mermaidvisualizer.saveDiagramToFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.intellij.plugins.markdown.ui.preview.BrowserPipe

internal const val TAG_COPY_SVG = "mermaid/copy-svg"
internal const val TAG_COPY_PNG = "mermaid/copy-png"
internal const val TAG_SAVE = "mermaid/save"
internal const val TAG_OPEN_LINK = "mermaid/open-link"
internal const val TAG_OPEN_IN_TAB = "mermaid/open-in-tab"
internal const val TAG_COPY_TEXT = "mermaid/copy-text"
internal const val TAG_REPORT_ERROR = "mermaid/report-error"

/**
 * Subscribes to BrowserPipe messages sent from JS export toolbar buttons, the error banner
 * buttons and the diagram link handler in `mermaid-render.js`. The tag constants ([TAG_COPY_SVG],
 * [TAG_COPY_PNG], [TAG_SAVE], [TAG_OPEN_LINK], [TAG_OPEN_IN_TAB], [TAG_COPY_TEXT],
 * [TAG_REPORT_ERROR]) must match the strings used in `pipe.post()` calls in that JS file.
 *
 * Arbitrary text travels base64 UTF-8 encoded: the diagram source ([TAG_OPEN_IN_TAB]), the raw
 * Mermaid error ([TAG_COPY_TEXT]) and the JSON error report `{raw, summary, category, line, column}`
 * ([TAG_REPORT_ERROR]).
 *
 * The [disposed] flag guards against processing messages after the editor is closed.
 * Note: BrowserPipe does not expose an unsubscribe API, so handlers remain registered
 * but short-circuit via the disposed check.
 */
internal class MermaidMarkdownExportHandler(
    private val project: Project?,
    browserPipe: BrowserPipe,
) {
    @Volatile
    private var disposed = false

    init {
        browserPipe.subscribe(TAG_COPY_SVG, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) copySvgToClipboard(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_COPY_PNG, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) copyPngToClipboard(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_SAVE, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) saveDiagramToFile(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_OPEN_IN_TAB, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) openDiagramInNewTabFromBase64(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_COPY_TEXT, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) copyErrorToClipboardFromBase64(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_REPORT_ERROR, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) reportErrorOnGitHubFromBase64(data, project)
                }
                return true
            }
        })
        browserPipe.subscribe(TAG_OPEN_LINK, object : BrowserPipe.Handler {
            override fun processMessageReceived(data: String): Boolean {
                if (disposed) return false
                openExternalLink(data) // schedules on the EDT itself, no invokeLater needed
                return true
            }
        })
    }

    fun dispose() {
        disposed = true
    }
}
