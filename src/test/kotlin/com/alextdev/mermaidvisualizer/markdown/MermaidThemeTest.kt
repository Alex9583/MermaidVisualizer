package com.alextdev.mermaidvisualizer.markdown

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidThemeTest {

    private lateinit var renderJs: String
    private lateinit var previewCss: String

    @BeforeAll
    fun loadResources() {
        val jsStream = javaClass.classLoader.getResourceAsStream("web/mermaid-render.js")
        assertNotNull(jsStream, "web/mermaid-render.js should be on the classpath")
        renderJs = jsStream!!.use { it.readBytes().toString(Charsets.UTF_8) }

        val cssStream = javaClass.classLoader.getResourceAsStream("web/mermaid-preview.css")
        assertNotNull(cssStream, "web/mermaid-preview.css should be on the classpath")
        previewCss = cssStream!!.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun `mermaid render js contains isDarkTheme function`() {
        assertTrue(
            renderJs.contains("function isDarkTheme()"),
            "mermaid-render.js should define isDarkTheme()"
        )
    }

    @Test
    fun `mermaid render js contains dark class detection`() {
        assertTrue(
            renderJs.contains("DARK_CLASSES"),
            "mermaid-render.js should define DARK_CLASSES array"
        )
        assertTrue(renderJs.contains("darcula"), "DARK_CLASSES should include 'darcula'")
        assertTrue(renderJs.contains("dark-mode"), "DARK_CLASSES should include 'dark-mode'")
    }

    @Test
    fun `mermaid render js contains theme observer setup`() {
        assertTrue(
            renderJs.contains("function setupThemeObserver()"),
            "mermaid-render.js should define setupThemeObserver()"
        )
        assertTrue(
            renderJs.contains("function onThemeChange()"),
            "mermaid-render.js should define onThemeChange()"
        )
        assertTrue(
            renderJs.contains("async function reInitAndRenderAll("),
            "mermaid-render.js should define reInitAndRenderAll()"
        )
    }

    @Test
    fun `mermaid render js initializes with theme detection`() {
        assertTrue(
            renderJs.contains("isDarkTheme() ? 'dark' : 'default'"),
            "mermaid-render.js should initialize theme based on isDarkTheme()"
        )
    }

    @Test
    fun `mermaid render js re-initializes mermaid on theme change`() {
        val fnStart = renderJs.indexOf("async function reInitAndRenderAll(")
        assertTrue(fnStart >= 0, "reInitAndRenderAll function should exist")
        val fnBlock = renderJs.substring(fnStart, minOf(fnStart + 500, renderJs.length))
        assertTrue(
            fnBlock.contains("initMermaid"),
            "reInitAndRenderAll should call initMermaid to re-initialize Mermaid"
        )
    }

    @Test
    fun `mermaid render js loads shadow css and toggles dark class`() {
        assertTrue(
            renderJs.contains("mermaid-shadow.css"),
            "mermaid-render.js should reference mermaid-shadow.css"
        )
        assertTrue(
            renderJs.contains("classList.toggle"),
            "mermaid-render.js should toggle dark class on host elements"
        )
    }

    @Test
    fun `mermaid render js re-renders diagrams on theme change`() {
        val fnStart = renderJs.indexOf("async function reInitAndRenderAll(")
        assertTrue(fnStart >= 0, "reInitAndRenderAll should exist")

        val restOfCode = renderJs.substring(fnStart)
        assertTrue(
            restOfCode.contains("CLASS_DIAGRAM") || restOfCode.contains("mermaid-diagram"),
            "reInitAndRenderAll should query rendered diagram containers"
        )
        assertTrue(
            restOfCode.contains("ATTR_PROCESSED") || restOfCode.contains("data-processed"),
            "reInitAndRenderAll should use ATTR_PROCESSED to find rendered diagrams"
        )
        assertTrue(
            restOfCode.contains("renderSingleDiagram"),
            "reInitAndRenderAll should call renderSingleDiagram to re-render each diagram"
        )
    }

    @Test
    fun `mermaid preview css has no theme-specific colors`() {
        // Theme colors are handled in JS via shadow DOM styles.
        // The external CSS should only contain layout properties.
        val colorPatterns = listOf(
            Regex("#[0-9a-fA-F]{3,8}\\b"),   // hex colors
            Regex("rgb\\("),                   // rgb()
            Regex("rgba\\("),                  // rgba()
            Regex("hsl\\("),                   // hsl()
        )
        for (pattern in colorPatterns) {
            assertFalse(
                pattern.containsMatchIn(previewCss),
                "mermaid-preview.css should not contain theme colors (found: ${pattern.pattern}), " +
                "colors are managed in JS shadow DOM styles"
            )
        }
    }
}
