package com.alextdev.mermaidvisualizer.markdown

import com.alextdev.mermaidvisualizer.MermaidWebResources
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

    fun testScriptsFollowTheSharedLoadOrder() {
        val names = extension.scripts.map { it.substringAfterLast('/') }
        val expected = listOf(MermaidWebResources.MERMAID_JS, MermaidWebResources.SHADOW_CSS_INIT_JS, MermaidWebResources.CONFIG_INIT_JS) +
            MermaidWebResources.MARKDOWN_SCRIPTS
        assertEquals(expected, names)
        assertTrue("Script URLs should be served by the preview server", extension.scripts.all { it.contains("://") })
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
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-zoom.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-core.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-render.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-preview.css"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-shadow.css"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-shadow-css-init.js"))
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js"))
    }

    fun testCanProvideReturnsFalseForUnknownResources() {
        assertFalse(extension.canProvide("http://localhost:63342/markdownPreview/abc123/unknown.js"))
        assertFalse(extension.canProvide("http://localhost:63342/markdownPreview/abc123/style.css"))
        assertFalse(extension.canProvide("something-random"))
    }

    fun testLoadResourceReturnsNonNullForKnownFiles() {
        val jsResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid.min.js")
        assertNotNull("mermaid.min.js should be loadable", jsResource)

        val zoomResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-zoom.js")
        assertNotNull("mermaid-zoom.js should be loadable", zoomResource)

        val coreResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-core.js")
        assertNotNull("mermaid-core.js should be loadable", coreResource)

        val renderResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-render.js")
        assertNotNull("mermaid-render.js should be loadable", renderResource)

        val cssResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-preview.css")
        assertNotNull("mermaid-preview.css should be loadable", cssResource)

        val shadowCssResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-shadow.css")
        assertNotNull("mermaid-shadow.css should be loadable", shadowCssResource)

        val initResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-shadow-css-init.js")
        assertNotNull("mermaid-shadow-css-init.js should be loadable", initResource)

        val configInitResource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js")
        assertNotNull("mermaid-config-init.js should be loadable", configInitResource)
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

    fun testExtensionAcceptsNullParams() {
        val ext = MermaidBrowserExtension(null, null)
        try {
            assertNotNull(ext)
            assertTrue("Extension should provide scripts without pipe or handler", ext.scripts.isNotEmpty())
        } finally {
            ext.dispose()
        }
    }

    fun testCanProvideConfigInitJs() {
        assertTrue(extension.canProvide("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js"))
    }

    fun testLoadResourceConfigInitJsReturnsValidJs() {
        val resource = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js")
        assertNotNull("mermaid-config-init.js should be loadable", resource)
        assertEquals("application/javascript", resource!!.type)
        val content = resource.content.toString(Charsets.UTF_8)
        assertTrue("Config init script should set __MERMAID_CONFIG", content.contains("__MERMAID_CONFIG"))
    }

    fun testLoadResourceConfigInitJsIsNotCached() {
        val first = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js")
        val second = extension.loadResource("http://localhost:63342/markdownPreview/abc123/mermaid-config-init.js")
        assertNotNull(first)
        assertNotNull(second)
        assertNotSame("Config init script should NOT be cached (always fresh)", first, second)
    }
}
