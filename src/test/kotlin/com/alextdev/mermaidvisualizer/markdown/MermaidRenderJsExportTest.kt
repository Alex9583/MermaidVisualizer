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
    fun `render js applies custom background color per diagram container`() {
        assertTrue(jsContent.contains("cfg.backgroundColor"), "Should read cfg.backgroundColor")
        assertTrue(
            jsContent.contains("container.style.background = cfg.backgroundColor || ''"),
            "Background should be scoped to the diagram container and cleared when no override is set"
        )
    }

    @Test
    fun `render js passes maxHeightPercent from config to inline zoom`() {
        assertTrue(jsContent.contains("maxHeightPercent"), "Should pass maxHeightPercent to the zoom init")
        assertTrue(
            jsContent.contains("(window.__MERMAID_CONFIG && window.__MERMAID_CONFIG.maxHeightPercent) || 60"),
            "Max preview height should be read from __MERMAID_CONFIG with a default fallback"
        )
    }

    @Test
    fun `render js renders through the shared pipeline`() {
        assertTrue(jsContent.contains("function createHost(isDark)"), "The context specifics should be gathered in a host strategy")
        assertTrue(jsContent.contains("const result = await core.renderInto(container, source, { source: source }, createHost(isDark));"),
            "Rendering, limits, banner and classification belong to the core pipeline")
        assertTrue(jsContent.contains("if (result.ok) rememberRender(source, result.svg);"), "Successful renders should be remembered")
        assertFalse(jsContent.contains("mermaid.render("), "mermaid.render() is called by the pipeline only")
        assertFalse(jsContent.contains("detectTextSizeExceeded"), "The text size fallback is detected by the pipeline")
    }

    // --- Error banner tests ---

    @Test
    fun `render js leaves error display to the pipeline`() {
        assertFalse(jsContent.contains("showErrorBanner") || jsContent.contains("classifyRenderError"),
            "Classification and the banner belong to the pipeline")
        assertTrue(jsContent.contains("core.showRenderError(container, 'Mermaid failed to load. Please try reopening the preview.', null, null, createHost(isDark));"),
            "The load failure should use the same banner")
    }

    @Test
    fun `render js restores the last valid rendering under the banner`() {
        assertTrue(jsContent.contains("const lastGoodRenders = [];"), "Should remember the last successful renderings")
        assertTrue(jsContent.contains("rememberRender(source, result.svg);"), "Successful renders should be remembered with their source")
        assertTrue(jsContent.contains("previousRendering: lastGoodRender"),
            "The pipeline asks the host for the rendering to put back under the banner, looked up by the failing source")
        assertTrue(jsContent.contains("core.looksLikeSameDiagram(lastGoodRenders[i].source, source)"),
            "A rendering is reused only for an edit of the diagram it came from, wherever the block moved")
        assertTrue(jsContent.contains("core.looksLikeSameDiagram(entry.source, source)"),
            "Remembering a rendering replaces the entry of the same diagram, so typing does not grow the cache")
        assertTrue(jsContent.contains("const MAX_REMEMBERED_RENDERS = 16;"), "The cache should be bounded")
        assertFalse(jsContent.contains("locateSlot") || jsContent.contains("lastGoodSlotCount"),
            "Block positions must not identify diagrams: a block inserted above shifts them")
        val reInitIndex = jsContent.indexOf("async function reInitAndRenderAll")
        assertTrue(jsContent.indexOf("lastGoodRenders.length = 0;", reInitIndex) > reInitIndex,
            "Theme/config re-renders should drop renderings made with the previous colors")
    }

    @Test
    fun `render js posts banner actions as base64 on their tags`() {
        assertTrue(jsContent.contains("post(tag, core.utf8ToBase64(text));"), "postText should encode arbitrary text as base64 UTF-8")
        assertTrue(jsContent.contains("copyText: function (text) { postText('mermaid/copy-text', text); }"),
            "Copy error should post the base64 raw message on mermaid/copy-text")
        assertTrue(jsContent.contains("reportError: function (json) { postText('mermaid/report-error', json); }"),
            "Report should post the base64 JSON payload on mermaid/report-error")
        assertTrue(jsContent.contains("errorActions: errorActions"), "The banner actions should be given to the pipeline through the host")
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
        assertTrue(jsContent.contains("mermaid/open-in-tab"), "Should use mermaid/open-in-tab tag")
        assertTrue(jsContent.contains("mermaid/copy-text"), "Should use mermaid/copy-text tag")
        assertTrue(jsContent.contains("mermaid/report-error"), "Should use mermaid/report-error tag")
    }

    @Test
    fun `render js open in tab sends base64 source from data-source attribute`() {
        assertTrue(jsContent.contains("openInTab: function () { postText('mermaid/open-in-tab', container.getAttribute(ATTR_SOURCE) || ''); }"),
            "Open in new tab should post the base64 source kept on the container")
    }

    @Test
    fun `render js Save sends payload via messagePipe`() {
        assertTrue(jsContent.contains("save: function (payload) { post('mermaid/save', payload); }"),
            "Save button should send its payload to Kotlin on mermaid/save")
        assertTrue(jsContent.contains("if (pipe) pipe.post(tag, payload);"), "Posts are dropped silently when the pipe is absent")
    }

    // --- Zoom integration tests ---

    @Nested
    inner class ZoomIntegration {

        @Test
        fun `configures inline zoom through the host`() {
            assertTrue(jsContent.contains("zoomOptions: zoomOptions"), "The zoom options are given to the pipeline through the host")
            assertFalse(jsContent.contains("__initMermaidZoom"), "Zoom is initialized by the pipeline")
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
        fun `error overlay sits below the export toolbar and above the zoom viewport`() {
            val overlay = shadowCssContent.substringAfter(".mermaid-error-overlay {").substringBefore("}")
            assertTrue(overlay.contains("position: absolute;"), "The overlay should not push the diagram")
            assertTrue(overlay.contains("top: 38px;"), "The overlay should start below the 28px toolbar buttons")
            assertTrue(overlay.contains("z-index: 11;"), "The overlay should be above the viewport and the toolbar (z-index 10)")
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

    @Test
    fun `render js routes link clicks to the open-link tag`() {
        assertTrue(jsContent.contains("post('mermaid/open-link', url);") && jsContent.contains("openLink: openLink,"),
            "Link clicks should reach Kotlin on mermaid/open-link through the host")
        assertFalse(jsContent.contains("installLinkHandler"), "The link handler is installed by the pipeline")
    }
}