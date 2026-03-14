package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.*
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
        assertTrue(jsContent.contains("mermaid.initialize"), "Should call mermaid.initialize")
    }

    @Test
    fun `standalone js has UTF-8 aware base64 decoding`() {
        assertTrue(jsContent.contains("base64ToUtf8"), "Should use base64ToUtf8 for UTF-8 aware base64 decoding")
        assertTrue(jsContent.contains("TextDecoder"), "Should use TextDecoder for UTF-8 decoding")
    }

    @Test
    fun `standalone css contains dark-theme`() {
        assertTrue(cssContent.contains(".dark-theme"), "Should contain .dark-theme selector")
    }

    @Test
    fun `standalone js uses Shadow DOM for isolation`() {
        assertTrue(jsContent.contains("attachShadow"), "Should use Shadow DOM for style isolation")
    }

    @Test
    fun `standalone js uses strict security level`() {
        assertTrue(jsContent.contains("securityLevel"), "Should configure securityLevel")
        assertTrue(jsContent.contains("'strict'"), "Should use strict security level")
    }

    @Test
    fun `standalone js configures maxTextSize`() {
        assertTrue(jsContent.contains("maxTextSize"), "Should configure maxTextSize for large diagrams")
    }

    @Test
    fun `standalone js has error handling`() {
        assertTrue(jsContent.contains("showError"), "Should have error display function")
        assertTrue(jsContent.contains("mermaid-error"), "Should have error CSS class")
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
    fun `standalone js extractSvg uses XMLSerializer`() {
        assertTrue(jsContent.contains("XMLSerializer"), "Should use XMLSerializer to serialize SVG")
    }

    @Test
    fun `standalone js extractPng uses canvas toDataURL`() {
        assertTrue(jsContent.contains("toDataURL"), "Should use canvas.toDataURL for PNG export")
    }

    @Test
    fun `standalone js extractSvg sets xmlns for standalone SVG`() {
        assertTrue(jsContent.contains("xmlns"), "Should set xmlns attribute for standalone SVG usage")
    }

    @Test
    fun `standalone js extractPng handles dark theme background`() {
        assertTrue(jsContent.contains("dark-theme") && jsContent.contains("fillRect"),
            "Should fill background based on dark/light theme")
    }

    // --- Export toolbar tests ---

    @Test
    fun `standalone js has createExportToolbar function`() {
        assertTrue(jsContent.contains("createExportToolbar"), "Should have createExportToolbar function")
    }

    @Test
    fun `standalone js has icon constants`() {
        assertTrue(jsContent.contains("ICON_COPY"), "Should have ICON_COPY constant")
        assertTrue(jsContent.contains("ICON_IMAGE"), "Should have ICON_IMAGE constant")
        assertTrue(jsContent.contains("ICON_SAVE"), "Should have ICON_SAVE constant")
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
    fun `standalone js has toolbar CSS classes`() {
        assertTrue(jsContent.contains("mermaid-export-toolbar"), "Should reference mermaid-export-toolbar CSS class")
        assertTrue(jsContent.contains("mermaid-export-btn"), "Should reference mermaid-export-btn CSS class")
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
    fun `standalone js extractPng has try-catch in img onload`() {
        assertTrue(jsContent.contains("img.onload"), "Should have img.onload handler")
        val onloadIndex = jsContent.indexOf("img.onload")
        val onerrorIndex = jsContent.indexOf("img.onerror", onloadIndex)
        assertTrue(onerrorIndex > onloadIndex, "Should have img.onerror after img.onload")
        val onloadBlock = jsContent.substring(onloadIndex, onerrorIndex)
        assertTrue(onloadBlock.contains("catch"), "img.onload should contain try-catch for PNG extraction errors")
    }

    @Test
    fun `standalone js img onerror logs error`() {
        assertTrue(jsContent.contains("SVG image load error"), "img.onerror should log an error message")
    }

    @Test
    fun `standalone js export handlers have error handling`() {
        assertTrue(jsContent.contains("Copy SVG failed"), "Copy SVG handler should have error logging")
        assertTrue(jsContent.contains("Copy SVG: no SVG found"), "Copy SVG should log when no SVG found")
    }

    @Test
    fun `standalone js extractPng checks canvas context`() {
        assertTrue(jsContent.contains("Failed to get 2D canvas context"), "Should check canvas.getContext result")
    }

    @Test
    fun `standalone js Save sends error payload when no SVG found`() {
        assertTrue(jsContent.contains("__saveBridge(JSON.stringify"),
            "Save button should send error payload to Kotlin when no SVG found")
    }

    // --- Zoom integration tests ---

    @Test
    fun `standalone js calls initMermaidZoom`() {
        assertTrue(jsContent.contains("__initMermaidZoom"), "Should call __initMermaidZoom for zoom support")
    }

    @Test
    fun `standalone js passes fitMode width`() {
        assertTrue(jsContent.contains("fitMode: 'width'"), "Should pass fitMode 'width' for standalone editor")
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

    @Test
    fun `standalone js extractPng uses viewBox for dimensions`() {
        assertTrue(jsContent.contains("viewBox"), "extractPng should use SVG viewBox for natural dimensions")
    }

    // --- Settings config integration tests ---

    @Test
    fun `standalone js references __MERMAID_CONFIG`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG"), "Should read window.__MERMAID_CONFIG for settings")
    }

    @Test
    fun `standalone js reads config fields in initMermaid`() {
        assertTrue(jsContent.contains("cfg.theme"), "initMermaid should read cfg.theme")
        assertTrue(jsContent.contains("cfg.maxTextSize"), "initMermaid should read cfg.maxTextSize")
        assertTrue(jsContent.contains("cfg.look"), "initMermaid should read cfg.look")
        assertTrue(jsContent.contains("cfg.fontFamily"), "initMermaid should read cfg.fontFamily")
    }

    @Test
    fun `standalone js falls back when config is absent`() {
        assertTrue(jsContent.contains("window.__MERMAID_CONFIG || {}"), "Should fallback to empty object when config is absent")
    }
}
