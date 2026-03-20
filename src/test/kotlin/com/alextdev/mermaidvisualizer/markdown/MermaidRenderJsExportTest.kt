package com.alextdev.mermaidvisualizer.markdown

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
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
    fun `shadow css toolbar uses host hover`() {
        assertTrue(shadowCssContent.contains(":host(:hover)"), "Shadow CSS should use :host(:hover) for toolbar visibility")
    }

    @Test
    fun `render js Save sends payload via messagePipe`() {
        assertTrue(jsContent.contains("pipe.post('mermaid/save'"),
            "Save button should send payload to Kotlin via messagePipe")
    }

    // --- Zoom integration tests ---

    @Test
    fun `render js calls initMermaidZoom`() {
        assertTrue(jsContent.contains("__initMermaidZoom"), "Should call __initMermaidZoom for zoom support")
    }

    @Test
    fun `render js passes fitMode width`() {
        assertTrue(jsContent.contains("fitMode: 'width'"), "Should pass fitMode 'width' for Markdown preview")
    }

    @Test
    fun `render js passes wheelRequiresModifier true`() {
        assertTrue(jsContent.contains("wheelRequiresModifier: true"), "Should pass wheelRequiresModifier true for Markdown")
    }

    // --- Shadow CSS zoom styles ---

    @Test
    fun `shadow css has zoom viewport styles`() {
        assertTrue(shadowCssContent.contains(".mermaid-zoom-viewport"), "Shadow CSS should style mermaid-zoom-viewport")
    }

    @Test
    fun `shadow css has zoom content styles`() {
        assertTrue(shadowCssContent.contains(".mermaid-zoom-content"), "Shadow CSS should style mermaid-zoom-content")
    }

    @Test
    fun `shadow css has toolbar divider`() {
        assertTrue(shadowCssContent.contains(".mermaid-toolbar-divider"), "Shadow CSS should style mermaid-toolbar-divider")
    }

    @Test
    fun `shadow css has zoom label`() {
        assertTrue(shadowCssContent.contains(".mermaid-zoom-label"), "Shadow CSS should style mermaid-zoom-label")
    }

    @Test
    fun `shadow css has tooltip on hover`() {
        assertTrue(shadowCssContent.contains(".mermaid-export-btn::after"), "Shadow CSS should have tooltip pseudo-element")
        assertTrue(shadowCssContent.contains("attr(title)"), "Tooltip should use attr(title) for content")
        assertTrue(shadowCssContent.contains(".mermaid-export-btn:hover::after"), "Tooltip should appear on hover")
        assertTrue(shadowCssContent.contains("transition-delay: 1s"), "Tooltip should have 1s hover delay")
    }

    // --- Settings config integration tests ---

    @Test
    fun `render js references __MERMAID_CONFIG`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG"), "Should read window.__MERMAID_CONFIG for settings")
    }

    @Test
    fun `render js subscribes to config update via messagePipe`() {
        assertTrue(jsContent.contains("mermaid/config"), "Should subscribe to mermaid/config tag for live settings refresh")
    }

    @Test
    fun `render js has reInitAndRenderAll function`() {
        assertTrue(jsContent.contains("reInitAndRenderAll"), "Should have reInitAndRenderAll for config and theme changes")
    }
}
