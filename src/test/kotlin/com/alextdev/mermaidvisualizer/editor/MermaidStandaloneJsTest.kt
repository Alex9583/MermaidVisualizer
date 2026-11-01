package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertFalse
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
        assertTrue(jsContent.contains("core.showError(container, shadowCss, 'Failed to initialize Mermaid: ' + e.message, null, false);"),
            "The page-load initialization failure should use the plain error box (no diagram to keep yet)")
        assertTrue(jsContent.contains("core.showRenderError(container, 'Failed to initialize Mermaid: ' + e.message, null, null, host);"),
            "A later initialization failure should show the shared banner over the last valid diagram")
    }

    @Test
    fun `standalone js renders through the shared pipeline and reports its result`() {
        assertTrue(jsContent.contains("function createHost(isDark)"), "The context specifics should be gathered in a host strategy")
        assertTrue(jsContent.contains("const result = await core.renderInto(container, context.source, context, host);"),
            "Rendering, limits, banner and classification belong to the core pipeline")
        assertTrue(jsContent.contains("reportRenderResult(result.ok ? { status: 'ok' } : core.toRenderErrorPayload(result.error), generation);"),
            "The pipeline result should reach the editor annotator through the error bridge")
        assertFalse(jsContent.contains("mermaid.render("), "mermaid.render() is called by the pipeline only")
        assertFalse(jsContent.contains("showErrorBanner"), "The banner is shown by the pipeline only")
    }

    @Test
    fun `standalone js delegates rendering limits to the core pipeline`() {
        assertFalse(jsContent.contains("detectTextSizeExceeded"), "The text size fallback is detected by the pipeline")
        assertTrue(jsContent.contains("core.renderInto("), "Rendering goes through the pipeline, which reports limits as errors")
    }

    @Test
    fun `standalone js keeps reported lines relative to the document`() {
        assertTrue(jsContent.contains("const context = core.prepareSource(decoded);"),
            "The trimmed source and the removed leading lines should be passed as classification context")
    }

    @Test
    fun `standalone js wires the error banner buttons to their bridges`() {
        assertTrue(jsContent.contains("return typeof window[name] === 'function';"), "Bridges are looked up by name at call time")
        assertTrue(jsContent.contains("copyText: bridge('__copyTextBridge')"), "Copy error should be guarded by its bridge")
        assertTrue(jsContent.contains("reportError: bridge('__reportErrorBridge')"), "Report should be guarded by its bridge")
        assertTrue(jsContent.contains("errorActions: errorActions"), "The banner actions should be given to the pipeline through the host")
    }

    @Test
    fun `standalone js applies custom background color from config`() {
        assertTrue(jsContent.contains("applyBackground"), "Should have applyBackground function")
        assertTrue(jsContent.contains("document.body.style.background"), "Should set body background from config")
        assertTrue(jsContent.contains("cfg.backgroundColor || ''"), "Should clear inline background when no override is set")
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
        assertTrue(jsContent.contains("core.showRenderError(container, message, null, renderId, createHost(isDark));"),
            "The Kotlin fallback should also keep the last valid diagram")
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
    fun `standalone js wires open in tab bridge into toolbar and guard`() {
        assertTrue(jsContent.contains("const openInTab = bridge('__openInTabBridge');"), "Should look the __openInTabBridge up")
        assertTrue(jsContent.contains("if (!openInTab && !copySvg && !copyPng && !save) return null;"),
            "The no-bridges guard should also consider __openInTabBridge")
        assertTrue(jsContent.contains("openInTab: openInTab ? function () { openInTab(''); } : null,"),
            "The openInTab callback should be passed to core.createExportToolbar only when the bridge exists")
    }

    @Test
    fun `standalone js leaves toolbar and zoom mounting to the pipeline`() {
        assertTrue(jsContent.contains("createToolbar: createToolbar"), "The toolbar factory is given to the pipeline through the host")
        assertTrue(jsContent.contains("zoomOptions: zoomOptions"), "The zoom options are given to the pipeline through the host")
        assertFalse(jsContent.contains("mermaid-export-toolbar"), "The toolbar element is located by the pipeline")
        assertFalse(jsContent.contains("__initMermaidZoom"), "Zoom is initialized by the pipeline")
    }

    @Test
    fun `shadow css has toolbar styling`() {
        assertTrue(shadowCssContent.contains(".mermaid-export-toolbar"), "Shadow CSS should style mermaid-export-toolbar")
        assertTrue(shadowCssContent.contains(".mermaid-export-btn"), "Shadow CSS should style mermaid-export-btn")
    }

    @Test
    fun `shadow css styles the error banner`() {
        assertTrue(shadowCssContent.contains(".mermaid-error-banner {"), "Shadow CSS should style the error banner")
        assertTrue(shadowCssContent.contains(".mermaid-error-overlay {"), "Shadow CSS should style the overlay variant")
        assertTrue(shadowCssContent.contains("position: absolute;"), "The overlay should be absolutely positioned")
        assertTrue(shadowCssContent.contains(".mermaid-error-block {"), "Shadow CSS should style the block variant")
        assertTrue(shadowCssContent.contains(".mermaid-error-btn {"), "Shadow CSS should style the banner buttons")
        assertTrue(shadowCssContent.contains(".mermaid-error-raw[hidden]"), "Collapsed details should be hidden")
        assertTrue(shadowCssContent.contains(".mermaid-error-banner[data-category=\"incomplete\"]"),
            "Incomplete diagrams should get the softer variant")
        assertTrue(shadowCssContent.contains(":host(.mermaid-stale) .mermaid-zoom-viewport"),
            "The stale diagram under the banner should be dimmed")
    }

    @Test
    fun `shadow css defines warning colors for both themes`() {
        val hostBlock = shadowCssContent.substring(shadowCssContent.indexOf(":host {"), shadowCssContent.indexOf(":host(.dark) {"))
        val darkBlock = shadowCssContent.substring(shadowCssContent.indexOf(":host(.dark) {"))
        for (variable in listOf("--mermaid-warn-color:", "--mermaid-warn-bg:", "--mermaid-warn-border:")) {
            assertTrue(hostBlock.contains(variable), "Light theme should define $variable")
            assertTrue(darkBlock.substringBefore("}").contains(variable), "Dark theme should define $variable")
        }
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
    fun `standalone js enables keyboard zoom and scroll sync`() {
        assertTrue(jsContent.contains("enableKeyboard: true"), "Keyboard zoom shortcuts belong to the standalone editor")
        assertTrue(jsContent.contains("scrollSyncCallback: syncScrollToEditor"), "Zoom pan should drive the editor scroll")
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
    fun `standalone js routes link clicks through openLinkBridge`() {
        assertTrue(jsContent.contains("openLink: openLink"), "The link handler target is given to the pipeline through the host")
        assertTrue(jsContent.contains("bridge('__openLinkBridge')"), "Link clicks should route through the __openLinkBridge JCEF bridge")
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
