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

internal class MermaidBrowserExtension : MarkdownBrowserPreviewExtension, ResourceProvider {

    private var serverRegistration: Disposable? = null

    init {
        serverRegistration = PreviewStaticServer.instance.registerResourceProvider(this)
    }

    override val scripts: List<String> = listOf(
        PreviewStaticServer.getStaticUrl(this, "mermaid.min.js"),
        PreviewStaticServer.getStaticUrl(this, "mermaid-render.js"),
    )

    override val styles: List<String> = listOf(
        PreviewStaticServer.getStaticUrl(this, "mermaid-preview.css"),
    )

    override val resourceProvider: ResourceProvider
        get() = this

    override val priority: Priority
        get() = Priority.AFTER_ALL

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
            resourceCache[resolved] ?: run {
                val bytes = javaClass.classLoader.getResourceAsStream("web/$resolved")?.use { it.readBytes() }
                if (bytes == null) {
                    LOG.error("Resource not found: web/$resolved — plugin installation may be corrupted")
                    return null
                }
                val mimeType = when {
                    resolved.endsWith(".js") -> "application/javascript"
                    resolved.endsWith(".css") -> "text/css"
                    else -> "application/octet-stream"
                }
                ResourceProvider.Resource(bytes, mimeType).also { resourceCache[resolved] = it }
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
