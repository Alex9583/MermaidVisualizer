package com.alextdev.mermaidvisualizer.markdown

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension.Priority
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.PreviewStaticServer
import org.intellij.plugins.markdown.ui.preview.ResourceProvider

private val LOG = Logger.getInstance("MermaidVisualizer")

private val RESOURCE_NAMES = setOf("mermaid.min.js", "mermaid-render.js", "mermaid-preview.css")

private val resourceCache = mutableMapOf<String, ResourceProvider.Resource>()
private val failedResources = mutableSetOf<String>()

internal class MermaidBrowserExtension : MarkdownBrowserPreviewExtension, ResourceProvider {

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
                PreviewStaticServer.getStaticUrl(this, "mermaid.min.js"),
                PreviewStaticServer.getStaticUrl(this, "mermaid-render.js"),
            )
        } catch (e: Exception) {
            LOG.error("Failed to generate script URLs for Mermaid preview", e)
            emptyList()
        }

    override val styles: List<String>
        get() = try {
            listOf(
                PreviewStaticServer.getStaticUrl(this, "mermaid-preview.css"),
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
                    val bytes = javaClass.classLoader.getResourceAsStream("web/$resolved")?.use { it.readBytes() }
                    if (bytes == null) {
                        LOG.error("Resource not found: web/$resolved — plugin installation may be corrupted")
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
                    LOG.error("Failed to load resource: web/$resolved", e)
                    failedResources.add(resolved)
                    return null
                }
            }
        }
    }

    override fun dispose() {
        serverRegistration?.dispose()
        serverRegistration = null
    }

    class Provider : MarkdownBrowserPreviewExtension.Provider {
        override fun createBrowserExtension(panel: MarkdownHtmlPanel): MarkdownBrowserPreviewExtension {
            return MermaidBrowserExtension()
        }
    }
}
