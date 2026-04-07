package com.alextdev.mermaidvisualizer.markdown

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidRenderJsExportTest {

    private lateinit var jsContent: String
    private lateinit var shadowCssContent: String

    @BeforeAll
    fun loadResources() {
        val jsStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-render.js")
        ) { "web/mermaid-render.js should be on the classpath" }
        jsContent = jsStream.use { it.reader(Charsets.UTF_8).readText() }

        val shadowCssStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-shadow.css")
        ) { "web/mermaid-shadow.css should be on the classpath" }
        shadowCssContent = shadowCssStream.use { it.reader(Charsets.UTF_8).readText() }
    }

    // --- Export delegation tests ---

    @Test
    fun `render js delegates SVG extraction to core`() {
        assertTrue(jsContent.contains("extractSvg"), "Should delegate SVG extraction to core")
    }

    @Test
    fun `render js delegates PNG extraction to core`() {
        assertTrue(jsContent.contains("extractPng"), "Should delegate PNG extraction to core")
    }

    @Test
    fun `render js has createExportToolbar function`() {
        assertTrue(jsContent.contains("createExportToolbar"), "Should have createExportToolbar function")
    }

    @Test
    fun `render js has messagePipe guard`() {
        assertTrue(jsContent.contains("__IntelliJTools"), "Should check for __IntelliJTools")
        assertTrue(jsContent.contains("messagePipe"), "Should check for messagePipe")
    }

    @Test
    fun `render js uses correct message tags`() {
        assertTrue(jsContent.contains("mermaid/copy-svg"), "Should use mermaid/copy-svg tag")
        assertTrue(jsContent.contains("mermaid/copy-png"), "Should use mermaid/copy-png tag")
        assertTrue(jsContent.contains("mermaid/save"), "Should use mermaid/save tag")
    }

    @Test
    fun `render js Save sends payload via messagePipe`() {
        assertTrue(
            jsContent.contains("pipe.post('mermaid/save'"),
            "Save button should send payload to Kotlin via messagePipe"
        )
    }

    // --- Zoom integration tests ---

    @Nested
    inner class ZoomIntegration {

        @Test
        fun `calls initMermaidZoom`() {
            assertTrue(jsContent.contains("__initMermaidZoom"), "Should call __initMermaidZoom for zoom support")
        }

        @Test
        fun `passes layoutMode inline`() {
            assertTrue(
                jsContent.contains("layoutMode: 'inline'"),
                "Should pass layoutMode 'inline' for Markdown preview"
            )
        }

        @Test
        fun `passes wheelRequiresModifier true`() {
            assertTrue(
                jsContent.contains("wheelRequiresModifier: true"),
                "Should pass wheelRequiresModifier true for Markdown"
            )
        }

        @Test
        fun `does not pass fixedFitScale`() {
            assertFalse(
                jsContent.contains("fixedFitScale"),
                "Should not pass fixedFitScale — inline mode computes fit dynamically"
            )
        }

        @Test
        fun `does not pass constrainSvg`() {
            assertFalse(
                jsContent.contains("constrainSvg"),
                "Should not pass constrainSvg — inline mode uses standard SVG constraints"
            )
        }
    }

    // --- Shadow CSS tests ---

    @Nested
    inner class ShadowCssStyles {

        @Test
        fun `toolbar uses host hover`() {
            assertTrue(
                shadowCssContent.contains(":host(:hover)"),
                "Shadow CSS should use :host(:hover) for toolbar visibility"
            )
        }

        @Test
        fun `has zoom viewport styles`() {
            assertTrue(
                shadowCssContent.contains(".mermaid-zoom-viewport"),
                "Shadow CSS should style mermaid-zoom-viewport"
            )
        }

        @Test
        fun `has zoom content styles`() {
            assertTrue(
                shadowCssContent.contains(".mermaid-zoom-content"),
                "Shadow CSS should style mermaid-zoom-content"
            )
        }

        @Test
        fun `has toolbar divider`() {
            assertTrue(
                shadowCssContent.contains(".mermaid-toolbar-divider"),
                "Shadow CSS should style mermaid-toolbar-divider"
            )
        }

        @Test
        fun `has zoom label`() {
            assertTrue(
                shadowCssContent.contains(".mermaid-zoom-label"),
                "Shadow CSS should style mermaid-zoom-label"
            )
        }

        @Test
        fun `has tooltip on hover`() {
            assertTrue(
                shadowCssContent.contains(".mermaid-export-btn::after"),
                "Shadow CSS should have tooltip pseudo-element"
            )
            assertTrue(shadowCssContent.contains("attr(title)"), "Tooltip should use attr(title) for content")
            assertTrue(
                shadowCssContent.contains(".mermaid-export-btn:hover::after"),
                "Tooltip should appear on hover"
            )
            assertTrue(
                shadowCssContent.contains("transition-delay: 1s"),
                "Tooltip should have 1s hover delay"
            )
        }
    }

    // --- Settings config integration tests ---

    @Test
    fun `render js references __MERMAID_CONFIG`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG"), "Should read window.__MERMAID_CONFIG for settings")
    }

    @Test
    fun `render js subscribes to config update via messagePipe`() {
        assertTrue(
            jsContent.contains("mermaid/config"),
            "Should subscribe to mermaid/config tag for live settings refresh"
        )
    }

    @Test
    fun `render js has reInitAndRenderAll function`() {
        assertTrue(
            jsContent.contains("reInitAndRenderAll"),
            "Should have reInitAndRenderAll for config and theme changes"
        )
    }
}