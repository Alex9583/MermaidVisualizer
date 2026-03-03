package com.alextdev.mermaidvisualizer.markdown

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.plugins.markdown.extensions.MarkdownBrowserPreviewExtension.Priority

class MermaidBrowserExtensionTest : BasePlatformTestCase() {

    private lateinit var extension: MermaidBrowserExtension

    override fun setUp() {
        super.setUp()
        extension = MermaidBrowserExtension()
    }

    override fun tearDown() {
        try {
            extension.dispose()
        } finally {
            super.tearDown()
        }
    }

    fun testScriptsContainsTwoUrls() {
        val scripts = extension.scripts
        assertEquals(2, scripts.size)
        assertTrue("First script URL should end with mermaid.min.js", scripts[0].endsWith("mermaid.min.js"))
        assertTrue("Second script URL should end with mermaid-render.js", scripts[1].endsWith("mermaid-render.js"))
    }

    fun testStylesContainsOneUrl() {
        val styles = extension.styles
        assertEquals(1, styles.size)
        assertTrue("Style URL should end with mermaid-preview.css", styles[0].endsWith("mermaid-preview.css"))
    }

    fun testPriorityIsAfterAll() {
        assertEquals(Priority.AFTER_ALL, extension.priority)
    }

    fun testCanProvideReturnsTrueForKnownResources() {
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid.min.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-render.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-preview.css"))
    }

    fun testCanProvideReturnsFalseForUnknownResources() {
        assertFalse(extension.canProvide("http://localhost:63342/markdownPreview/abc123/unknown.js"))
        assertFalse(extension.canProvide("http://localhost:63342/markdownPreview/abc123/style.css"))
        assertFalse(extension.canProvide("something-random"))
    }

    fun testLoadResourceReturnsNonNullForKnownFiles() {
        val jsResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid.min.js")
        assertNotNull("mermaid.min.js should be loadable", jsResource)

        val renderResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-render.js")
        assertNotNull("mermaid-render.js should be loadable", renderResource)

        val cssResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-preview.css")
        assertNotNull("mermaid-preview.css should be loadable", cssResource)
    }

    fun testLoadResourceReturnsNullForUnknownFiles() {
        val resource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/unknown.js")
        assertNull("Unknown resource should return null", resource)
    }

    fun testCanProvideReturnsFalseForPathTraversal() {
        assertFalse(extension.canProvide("http://localhost:63342/../../etc/passwd"))
        assertFalse(extension.canProvide("http://localhost:63342/../web/secret.key"))
    }

    fun testCanProvideReturnsFalseForQueryString() {
        assertFalse(extension.canProvide(
            "http://localhost:63342/markdownPreview/abc123/mermaid.min.js?extra=1"
        ))
    }

    fun testCanProvideReturnsFalseForEdgeCaseInputs() {
        assertFalse(extension.canProvide(""))
        assertFalse(extension.canProvide("/"))
        assertFalse(extension.canProvide("mermaid.min.js/"))
    }

    fun testLoadResourceReturnsCachedInstance() {
        val url = "http://localhost:63342/markdownPreview/abc123/mermaid-render.js"
        val first = extension.loadResource(url)
        val second = extension.loadResource(url)
        assertNotNull(first)
        assertSame("Second call should return cached instance", first, second)
    }

    fun testLoadResourceReturnsCorrectMimeTypeForJs() {
        val resource = extension.loadResource(
            "http://localhost:63342/markdownPreview/abc123/mermaid-render.js"
        )
        assertNotNull(resource)
        assertEquals("application/javascript", resource!!.type)
    }

    fun testLoadResourceReturnsCorrectMimeTypeForCss() {
        val resource = extension.loadResource(
            "http://localhost:63342/markdownPreview/abc123/mermaid-preview.css"
        )
        assertNotNull(resource)
        assertEquals("text/css", resource!!.type)
    }

    fun testResourceProviderReturnsSelf() {
        assertSame(extension, extension.resourceProvider)
    }

    fun testDisposeCanBeCalledTwice() {
        extension.dispose()
        // Should not throw
        extension.dispose()
    }

    fun testProviderCreatesValidInstance() {
        val provider = MermaidBrowserExtension.Provider()
        assertNotNull(provider)
    }
}
