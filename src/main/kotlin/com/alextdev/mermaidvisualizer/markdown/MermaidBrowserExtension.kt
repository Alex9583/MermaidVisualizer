package com.alextdev.mermaidvisualizer.markdown

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension.Priority
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.PreviewStaticServer
import org.intellij.plugins.markdown.ui.preview.ResourceProvider

private val LOG = Logger.getInstance("MermaidVisualizer")

private const val RES_MERMAID_JS = "mermaid.min.js"
private const val RES_ZOOM_JS = "mermaid-zoom.js"
private const val RES_RENDER_JS = "mermaid-render.js"
private const val RES_PREVIEW_CSS = "mermaid-preview.css"
private const val RES_SHADOW_CSS = "mermaid-shadow.css"
private const val RES_SHADOW_CSS_INIT_JS = "mermaid-shadow-css-init.js"

private val RESOURCE_NAMES = setOf(
    RES_MERMAID_JS, RES_ZOOM_JS, RES_RENDER_JS,
    RES_PREVIEW_CSS, RES_SHADOW_CSS, RES_SHADOW_CSS_INIT_JS,
)

private val resourceCache = mutableMapOf<String, ResourceProvider.Resource>()
private val failedResources = mutableSetOf<String>()

internal class MermaidBrowserExtension(
    private val exportHandler: MermaidMarkdownExportHandler? = null,
) : MarkdownBrowserPreviewExtension, ResourceProvider {

    private var serverRegistration: Disposable? = null

    init {
        try {
            serverRegistration = PreviewStaticServer.instance.registerResourceProvider(this)
        } catch (e: Exception) {
            LOG.error(
                "Failed to register Mermaid resource provider with Markdown preview server. " +
                "Mermaid diagrams will not render. This may indicate an incompatible Markdown plugin version.",
                e
            )
        }
    }

    override val priority: Priority
        get() = Priority.AFTER_ALL

    override val scripts: List<String>
        get() = try {
            listOf(
                PreviewStaticServer.getStaticUrl(this, RES_MERMAID_JS),
                PreviewStaticServer.getStaticUrl(this, RES_SHADOW_CSS_INIT_JS),
                PreviewStaticServer.getStaticUrl(this, RES_ZOOM_JS),
                PreviewStaticServer.getStaticUrl(this, RES_RENDER_JS),
            )
        } catch (e: Exception) {
            LOG.error("Failed to generate script URLs for Mermaid preview", e)
            emptyList()
        }

    override val styles: List<String>
        get() = try {
            listOf(
                PreviewStaticServer.getStaticUrl(this, RES_PREVIEW_CSS),
            )
        } catch (e: Exception) {
            LOG.error("Failed to generate style URLs for Mermaid preview", e)
            emptyList()
        }

    override val resourceProvider: ResourceProvider
        get() = this

    private fun extractResourceName(resourceName: String): String? {
        val lastSegment = resourceName.substringAfterLast('/')
        return if (lastSegment in RESOURCE_NAMES) lastSegment else null
    }

    override fun canProvide(resourceName: String): Boolean {
        return extractResourceName(resourceName) != null
    }

    override fun loadResource(resourceName: String): ResourceProvider.Resource? {
        val resolved = extractResourceName(resourceName) ?: return null
        return synchronized(resourceCache) {
            if (resolved in failedResources) return null
            resourceCache[resolved] ?: run {
                try {
                    val bytes = loadResourceBytes(resolved)
                    if (bytes == null) {
                        LOG.error("Resource not found: $resolved — plugin installation may be corrupted")
                        failedResources.add(resolved)
                        return null
                    }
                    val mimeType = when {
                        resolved.endsWith(".js") -> "application/javascript"
                        resolved.endsWith(".css") -> "text/css"
                        else -> "application/octet-stream"
                    }
                    ResourceProvider.Resource(bytes, mimeType).also { resourceCache[resolved] = it }
                } catch (e: Exception) {
                    LOG.error("Failed to load resource: $resolved", e)
                    failedResources.add(resolved)
                    return null
                }
            }
        }
    }

    private fun loadResourceBytes(name: String): ByteArray? {
        if (name == RES_SHADOW_CSS_INIT_JS) {
            return buildShadowCssInitScript()
        }
        return javaClass.classLoader.getResourceAsStream("web/$name")?.use { it.readBytes() }
    }

    private fun buildShadowCssInitScript(): ByteArray? {
        val cssBytes = javaClass.classLoader.getResourceAsStream("web/mermaid-shadow.css")
            ?.use { it.readBytes() } ?: return null
        val cssText = cssBytes.toString(Charsets.UTF_8)
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("\$", "\\\$")
        return ("window.__MERMAID_SHADOW_CSS=`" + cssText + "`;").toByteArray(Charsets.UTF_8)
    }

    override fun dispose() {
        exportHandler?.dispose()
        serverRegistration?.dispose()
        serverRegistration = null
    }

    class Provider : MarkdownBrowserPreviewExtension.Provider {
        override fun createBrowserExtension(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension {
            var exportHandler: MermaidMarkdownExportHandler? = null
            try {
                val pipe = panel.getBrowserPipe()
                val project = try {
                    panel.getProject()
                } catch (e: Exception) {
                    LOG.debug("Could not obtain project from MarkdownHtmlPanel", e)
                    null
                }
                if (pipe != null) {
                    exportHandler = MermaidMarkdownExportHandler(project, pipe)
                }
            } catch (e: Exception) {
                LOG.warn("BrowserPipe not available, export from Markdown preview disabled", e)
            }
            return MermaidBrowserExtension(exportHandler)
        }
    }
}