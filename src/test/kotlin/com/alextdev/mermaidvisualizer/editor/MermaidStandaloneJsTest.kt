package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidStandaloneJsTest {

    private lateinit var jsContent: String
    private lateinit var cssContent: String
    private lateinit var shadowCssContent: String

    @BeforeAll
    fun loadResources() {
        val jsStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-standalone.js")
        ) { "web/mermaid-standalone.js should be on the classpath" }
        jsContent = jsStream.use { it.reader(Charsets.UTF_8).readText() }

        val cssStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-standalone.css")
        ) { "web/mermaid-standalone.css should be on the classpath" }
        cssContent = cssStream.use { it.reader(Charsets.UTF_8).readText() }

        val shadowCssStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-shadow.css")
        ) { "web/mermaid-shadow.css should be on the classpath" }
        shadowCssContent = shadowCssStream.use { it.reader(Charsets.UTF_8).readText() }
    }

    @Test
    fun `standalone js contains renderDiagram`() {
        assertTrue(jsContent.contains("window.renderDiagram"), "Should expose window.renderDiagram")
    }

    @Test
    fun `standalone js contains mermaid initialize`() {
        assertTrue(jsContent.contains("mermaid.initialize"), "Should reference mermaid.initialize in error messages")
    }

    @Test
    fun `standalone js has UTF-8 aware base64 decoding`() {
        assertTrue(jsContent.contains("base64ToUtf8"), "Should use core.base64ToUtf8 for UTF-8 aware base64 decoding")
    }

    @Test
    fun `standalone css contains dark-theme`() {
        assertTrue(cssContent.contains(".dark-theme"), "Should contain .dark-theme selector")
    }

    @Test
    fun `standalone js has error handling`() {
        assertTrue(jsContent.contains("showError"), "Should delegate error display to core.showError")
    }

    @Test
    fun `standalone js exposes scroll sync public contract`() {
        assertTrue(jsContent.contains("window.__scrollPreviewTo"), "Should expose __scrollPreviewTo for Kotlin->JS scroll sync")
        assertTrue(jsContent.contains("__mermaidScrollBridge"), "Should reference __mermaidScrollBridge for JS->Kotlin scroll sync")
    }

    @Test
    fun `standalone js scroll sync has defensive guards`() {
        assertTrue(jsContent.contains("scrollGuardActive"), "Should have scroll guard to prevent feedback loops")
        assertTrue(jsContent.contains("isFinite"), "Should validate fraction parameter")
        assertTrue(jsContent.contains("__scrollZoomTo"), "Should use zoom module for scroll sync")
    }

    @Test
    fun `standalone js has error handling for mermaid initialization`() {
        assertTrue(jsContent.contains("mermaid.initialize failed"), "Should catch and report mermaid.initialize errors")
    }

    @Test
    fun `standalone js exposes showError on window`() {
        assertTrue(jsContent.contains("window.__showError"), "Should expose __showError for Kotlin-side fallback error display")
    }

    @Test
    fun `standalone js has extractSvg function`() {
        assertTrue(jsContent.contains("window.__extractSvg"), "Should expose __extractSvg for SVG export")
    }

    @Test
    fun `standalone js has extractPng function`() {
        assertTrue(jsContent.contains("window.__extractPng"), "Should expose __extractPng for PNG export")
    }

    @Test
    fun `standalone js extractPng handles dark theme`() {
        assertTrue(jsContent.contains("dark-theme"), "Should detect dark-theme for PNG background")
    }

    // --- Export toolbar tests ---

    @Test
    fun `standalone js has createExportToolbar function`() {
        assertTrue(jsContent.contains("createExportToolbar"), "Should delegate to core.createExportToolbar")
    }

    @Test
    fun `shadow css has host hover CSS rule`() {
        assertTrue(shadowCssContent.contains(":host(:hover)"), "Shadow CSS should have :host(:hover) rule for toolbar visibility")
    }

    @Test
    fun `standalone js references bridge functions`() {
        assertTrue(jsContent.contains("__copySvgBridge"), "Should reference __copySvgBridge")
        assertTrue(jsContent.contains("__copyPngBridge"), "Should reference __copyPngBridge")
        assertTrue(jsContent.contains("__saveBridge"), "Should reference __saveBridge")
    }

    @Test
    fun `standalone js references toolbar CSS class for zoom init`() {
        assertTrue(jsContent.contains("mermaid-export-toolbar"), "Should reference mermaid-export-toolbar CSS class for zoom init")
    }

    @Test
    fun `shadow css has toolbar styling`() {
        assertTrue(shadowCssContent.contains(".mermaid-export-toolbar"), "Shadow CSS should style mermaid-export-toolbar")
        assertTrue(shadowCssContent.contains(".mermaid-export-btn"), "Shadow CSS should style mermaid-export-btn")
    }

    @Test
    fun `shadow css uses custom properties for theming`() {
        assertTrue(shadowCssContent.contains(":host(.dark)"), "Shadow CSS should have :host(.dark) for dark theme")
        assertTrue(shadowCssContent.contains("var(--"), "Shadow CSS should use CSS custom properties")
    }

    @Test
    fun `standalone js Save references bridge function`() {
        assertTrue(jsContent.contains("__saveBridge"),
            "Save button should reference __saveBridge for Kotlin bridge")
    }

    // --- Zoom integration tests ---

    @Test
    fun `standalone js calls initMermaidZoom`() {
        assertTrue(jsContent.contains("__initMermaidZoom"), "Should call __initMermaidZoom for zoom support")
    }

    @Test
    fun `standalone js passes fitMode fit`() {
        assertTrue(jsContent.contains("fitMode: 'fit'"), "Should pass fitMode 'fit' for standalone editor (considers both width and height)")
    }

    @Test
    fun `standalone js passes wheelRequiresModifier true`() {
        assertTrue(jsContent.contains("wheelRequiresModifier: true"), "Should pass wheelRequiresModifier true for standalone (Ctrl+wheel to zoom)")
    }

    @Test
    fun `standalone css has height 100vh`() {
        assertTrue(cssContent.contains("height: 100vh"), "Container should have height: 100vh for fit-to-window")
    }

    @Test
    fun `standalone css has overflow hidden`() {
        assertTrue(cssContent.contains("overflow: hidden"), "Container should have overflow: hidden for zoom viewport")
    }

    // --- Settings config integration tests ---

    @Test
    fun `standalone js references __MERMAID_CONFIG`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG"), "Should read window.__MERMAID_CONFIG for settings")
    }

    @Test
    fun `standalone js falls back when config is absent`() {
        assertTrue(jsContent.contains("window.__MERMAID_CONFIG || {}"), "Should fallback to empty object when config is absent")
    }
}
