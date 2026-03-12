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
            renderJs.contains("function doThemeChange()") || renderJs.contains("async function doThemeChange()"),
            "mermaid-render.js should define doThemeChange()"
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
        assertTrue(
            renderJs.contains("initMermaid(newTheme)") || renderJs.contains("initMermaid("),
            "doThemeChange should call initMermaid"
        )
        // Verify initMermaid is called inside doThemeChange
        val doThemeChangeStart = renderJs.indexOf("function doThemeChange()")
            .takeIf { it >= 0 }
            ?: renderJs.indexOf("async function doThemeChange()")
        assertTrue(doThemeChangeStart >= 0, "doThemeChange function should exist")
        val doThemeChangeBlock = renderJs.substring(doThemeChangeStart, minOf(doThemeChangeStart + 500, renderJs.length))
        assertTrue(
            doThemeChangeBlock.contains("initMermaid"),
            "doThemeChange should call initMermaid to re-initialize Mermaid with new theme"
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
        val doThemeChangeStart = renderJs.indexOf("async function doThemeChange()")
            .takeIf { it >= 0 }
            ?: renderJs.indexOf("function doThemeChange()")
        assertTrue(doThemeChangeStart >= 0, "doThemeChange should exist")

        // Find the end of doThemeChange — look for the next top-level function
        val restOfCode = renderJs.substring(doThemeChangeStart)
        assertTrue(
            restOfCode.contains("CLASS_DIAGRAM") || restOfCode.contains("mermaid-diagram"),
            "doThemeChange should query rendered diagram containers"
        )
        assertTrue(
            restOfCode.contains("ATTR_PROCESSED") || restOfCode.contains("data-processed"),
            "doThemeChange should use ATTR_PROCESSED to find rendered diagrams"
        )
        assertTrue(
            restOfCode.contains("renderSingleDiagram"),
            "doThemeChange should call renderSingleDiagram to re-render each diagram"
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
