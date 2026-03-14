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
    fun `render js has extractSvgFromContainer function`() {
        assertTrue(jsContent.contains("extractSvgFromContainer"), "Should have extractSvgFromContainer function")
    }

    @Test
    fun `render js has extractPngFromContainer function`() {
        assertTrue(jsContent.contains("extractPngFromContainer"), "Should have extractPngFromContainer function")
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
    fun `render js extractPng has try-catch in img onload`() {
        assertTrue(jsContent.contains("img.onload"), "Should have img.onload handler")
        val onloadIndex = jsContent.indexOf("img.onload")
        val onerrorIndex = jsContent.indexOf("img.onerror", onloadIndex)
        assertTrue(onerrorIndex > onloadIndex, "Should have img.onerror after img.onload")
        val onloadBlock = jsContent.substring(onloadIndex, onerrorIndex)
        assertTrue(onloadBlock.contains("catch"), "img.onload should contain try-catch for PNG extraction errors")
    }

    @Test
    fun `render js img onerror logs error`() {
        assertTrue(jsContent.contains("SVG image load error"), "img.onerror should log an error message")
    }

    @Test
    fun `render js export handlers have error handling`() {
        assertTrue(jsContent.contains("Copy SVG failed"), "Copy SVG handler should have error logging")
        assertTrue(jsContent.contains("Copy SVG: no SVG found"), "Copy SVG should log when no SVG found")
    }

    @Test
    fun `render js extractPng checks canvas context`() {
        assertTrue(jsContent.contains("Failed to get 2D canvas context"), "Should check canvas.getContext result")
    }

    @Test
    fun `render js Save sends error payload when no SVG found`() {
        assertTrue(jsContent.contains("pipe.post('mermaid/save', JSON.stringify"),
            "Save button should send error payload to Kotlin when no SVG found")
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

    @Test
    fun `render js extractPng uses viewBox for dimensions`() {
        assertTrue(jsContent.contains("viewBox"), "extractPngFromContainer should use SVG viewBox for natural dimensions")
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

    // --- Settings config integration tests ---

    @Test
    fun `render js references __MERMAID_CONFIG`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG"), "Should read window.__MERMAID_CONFIG for settings")
    }

    @Test
    fun `render js reads config fields in initMermaid`() {
        assertTrue(jsContent.contains("cfg.theme"), "initMermaid should read cfg.theme")
        assertTrue(jsContent.contains("cfg.maxTextSize"), "initMermaid should read cfg.maxTextSize")
        assertTrue(jsContent.contains("cfg.look"), "initMermaid should read cfg.look")
        assertTrue(jsContent.contains("cfg.fontFamily"), "initMermaid should read cfg.fontFamily")
    }

    @Test
    fun `render js falls back when config is absent`() {
        assertTrue(jsContent.contains("window.__MERMAID_CONFIG || {}"), "Should fallback to empty object when config is absent")
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
