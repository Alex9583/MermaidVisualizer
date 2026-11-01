package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.MermaidWebResources
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidCoreJsTest {

    // The core is split into modules (MermaidWebResources.CORE_SCRIPTS): the content checks below run on their
    // concatenation, in load order, so they describe the core as the browser sees it.
    private lateinit var jsContent: String

    @BeforeAll
    fun loadResources() {
        jsContent = MermaidWebResources.CORE_SCRIPTS.joinToString("\n") { name ->
            val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(MermaidWebResources.path(name))) {
                "${MermaidWebResources.path(name)} should be on the classpath"
            }
            stream.use { it.reader(Charsets.UTF_8).readText() }
        }
    }

    @Test
    fun `core js exposes __mermaidCore on window`() {
        assertTrue(jsContent.contains("window.__mermaidCore"), "Should expose __mermaidCore on window")
    }

    @Test
    fun `core js has utf8ToBase64 and base64ToUtf8`() {
        assertTrue(jsContent.contains("utf8ToBase64"), "Should have utf8ToBase64 function")
        assertTrue(jsContent.contains("base64ToUtf8"), "Should have base64ToUtf8 function")
    }

    @Test
    fun `core js has TextDecoder for UTF-8 decoding`() {
        assertTrue(jsContent.contains("TextDecoder"), "Should use TextDecoder for UTF-8 decoding")
    }

    @Test
    fun `core js has createSvgIcon`() {
        assertTrue(jsContent.contains("createSvgIcon"), "Should have createSvgIcon function")
    }

    @Test
    fun `core js has setShadowContent with attachShadow`() {
        assertTrue(jsContent.contains("setShadowContent"), "Should have setShadowContent function")
        assertTrue(jsContent.contains("attachShadow"), "Should use attachShadow for Shadow DOM")
    }

    @Test
    fun `core js toggles dark class on host elements`() {
        assertTrue(jsContent.contains("classList.toggle"), "Should toggle dark class on host elements")
    }

    @Test
    fun `core js injectSvg uses innerHTML for Mermaid output`() {
        assertTrue(jsContent.contains("injectSvg"), "Should have injectSvg function")
        assertTrue(jsContent.contains("innerHTML"), "Should use innerHTML for sanitized Mermaid SVG output")
    }

    @Test
    fun `core js showError uses mermaid-error class`() {
        assertTrue(jsContent.contains("showError"), "Should have showError function")
        assertTrue(jsContent.contains("mermaid-error"), "Should use mermaid-error CSS class")
    }

    // --- Error banner tests ---

    @Test
    fun `core js exposes the error banner and classification API`() {
        assertTrue(jsContent.contains("showErrorBanner: showErrorBanner"), "showErrorBanner should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("classifyRenderError: classifyRenderError"), "classifyRenderError should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("toRenderErrorPayload: toRenderErrorPayload"), "toRenderErrorPayload should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("prepareSource: prepareSource"), "prepareSource should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("looksLikeSameDiagram: looksLikeSameDiagram"), "looksLikeSameDiagram should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("removeRenderLeftover: removeRenderLeftover"), "removeRenderLeftover should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("clearShadowContent: clearShadowContent"), "clearShadowContent should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("JISON_TOKEN_LABELS: JISON_TOKEN_LABELS"), "JISON_TOKEN_LABELS should be exposed on __mermaidCore")
    }

    @Test
    fun `core js error banner keeps the last valid diagram`() {
        assertTrue(jsContent.contains("function showErrorBanner(container, shadowCss, info, isDark, actions)"),
            "showErrorBanner should follow the (container, shadowCss, info, isDark, actions) contract")
        assertTrue(jsContent.contains("if (hasRenderedDiagram(shadow)) {"),
            "The banner should check for a previously rendered diagram before touching the shadow root")
        assertTrue(jsContent.contains("banner.classList.add('mermaid-error-overlay');"),
            "Over a kept diagram, the banner should be an overlay")
        assertTrue(jsContent.contains("banner.classList.add('mermaid-error-block');"),
            "Without a diagram, the banner should be a regular box")
        assertTrue(jsContent.contains("const CLASS_STALE = 'mermaid-stale';"), "The host should be marked while the diagram is stale")
        assertTrue(jsContent.contains("el.classList.remove(CLASS_STALE);"),
            "A successful render (setShadowContent) should clear the stale marker")
    }

    @Test
    fun `core js error banner has category, summary, details and action buttons`() {
        assertTrue(jsContent.contains("const CLASS_BANNER = 'mermaid-error-banner';"), "Banner should use the mermaid-error-banner class")
        assertTrue(jsContent.contains("banner.setAttribute('data-category', category);"), "Banner should expose its category")
        assertTrue(jsContent.contains("rawPre.className = 'mermaid-error-raw';"), "Raw message should be in pre.mermaid-error-raw")
        assertTrue(jsContent.contains("rawPre.hidden = !detailsOpen;"), "Raw message should be collapsed by default")
        assertTrue(jsContent.contains("'Details'"), "Banner should have a Details toggle")
        assertTrue(jsContent.contains("'Copy error'"), "Banner should have a Copy error button")
        assertTrue(jsContent.contains("'Report on GitHub'"), "Banner should have a Report on GitHub button")
        assertTrue(jsContent.contains("typeof actions.copyText === 'function'"), "Copy error should only appear with a copyText action")
        assertTrue(jsContent.contains("typeof actions.reportError === 'function'"), "Report should only appear with a reportError action")
        assertTrue(jsContent.contains("actions.copyText(raw)"), "Copy error should send the raw message")
        assertTrue(jsContent.contains("btn.className = 'mermaid-error-btn';"), "Banner buttons should use mermaid-error-btn")
    }

    @Test
    fun `core js report payload carries the classification fields`() {
        val reportIndex = jsContent.indexOf("actions.reportError(JSON.stringify({")
        assertTrue(reportIndex > 0, "Report should send a JSON string")
        val payload = jsContent.substring(reportIndex, jsContent.indexOf("}));", reportIndex))
        for (field in listOf("raw:", "summary:", "category:", "line:", "column:")) {
            assertTrue(payload.contains(field), "Report payload should contain $field")
        }
    }

    @Test
    fun `core js classifies errors into the documented categories`() {
        for (category in listOf("'incomplete'", "'syntax'", "'unknown-diagram'", "'limit'", "'generic'")) {
            assertTrue(jsContent.contains(category), "Should produce the $category category")
        }
        assertTrue(jsContent.contains("err.name === 'UnknownDiagramError'"), "Should recognize UnknownDiagramError by name")
        assertTrue(jsContent.contains("err.hash"), "Should prefer the structured Jison err.hash")
        assertTrue(jsContent.contains("const MAX_EXPECTED_ITEMS = 4;"), "Summaries should list at most 4 expected tokens")
    }

    @Test
    fun `core js suppresses Mermaid's own error diagram`() {
        assertTrue(jsContent.contains("suppressErrorRendering: true"),
            "The previews show their own banner: Mermaid should not render its bomb diagram on each failure")
    }

    @Test
    fun `core js initMermaid calls mermaid initialize`() {
        assertTrue(jsContent.contains("initMermaid"), "Should have initMermaid function")
        assertTrue(jsContent.contains("mermaid.initialize"), "Should call mermaid.initialize")
        assertTrue(jsContent.contains("startOnLoad: false"), "Should set startOnLoad: false")
        assertTrue(jsContent.contains("securityLevel"), "Should configure securityLevel")
        assertTrue(jsContent.contains("'strict'"), "Should use strict security level")
    }

    @Test
    fun `core js initMermaid reads config`() {
        assertTrue(jsContent.contains("__MERMAID_CONFIG || {}"), "Should fallback to empty object when config is absent")
        assertTrue(jsContent.contains("cfg.theme"), "initMermaid should read cfg.theme")
        assertTrue(jsContent.contains("cfg.maxTextSize"), "initMermaid should read cfg.maxTextSize")
        assertTrue(jsContent.contains("cfg.maxEdges"), "initMermaid should read cfg.maxEdges")
        assertTrue(jsContent.contains("cfg.look"), "initMermaid should read cfg.look")
        assertTrue(jsContent.contains("cfg.fontFamily"), "initMermaid should read cfg.fontFamily")
    }

    @Test
    fun `core js initMermaid passes the layout engine only when configured`() {
        assertTrue(jsContent.contains("if (cfg.layout) opts.layout = cfg.layout;"),
            "initMermaid should forward cfg.layout to mermaid.initialize and leave Mermaid's default otherwise")
    }

    @Test
    fun `core js initMermaid applies custom line color via themeVariables`() {
        assertTrue(jsContent.contains("cfg.lineColor"), "initMermaid should read cfg.lineColor")
        assertTrue(jsContent.contains("themeVariables"), "Custom line color should be applied via themeVariables")
        assertTrue(jsContent.contains("opts.themeVariables.lineColor"), "Should set themeVariables.lineColor")
    }

    @Test
    fun `core js extractPng honors custom background color`() {
        assertTrue(jsContent.contains("cfg.backgroundColor"), "extractPng should read cfg.backgroundColor")
        assertTrue(
            jsContent.contains("cfg.backgroundColor || (isDarkFn()"),
            "Custom background should take precedence over the theme-derived default"
        )
    }

    @Test
    fun `core js extractSvg uses XMLSerializer and xmlns`() {
        assertTrue(jsContent.contains("extractSvg"), "Should have extractSvg function")
        assertTrue(jsContent.contains("XMLSerializer"), "Should use XMLSerializer to serialize SVG")
        assertTrue(jsContent.contains("xmlns"), "Should set xmlns attribute for standalone SVG usage")
    }

    @Test
    fun `core js extractPng uses canvas and viewBox`() {
        assertTrue(jsContent.contains("extractPng"), "Should have extractPng function")
        assertTrue(jsContent.contains("toDataURL"), "Should use canvas.toDataURL for PNG export")
        assertTrue(jsContent.contains("viewBox"), "Should use SVG viewBox for natural dimensions")
    }

    @Test
    fun `core js extractPng has try-catch in img onload`() {
        assertTrue(jsContent.contains("img.onload"), "Should have img.onload handler")
        val onloadIndex = jsContent.indexOf("img.onload")
        val onerrorIndex = jsContent.indexOf("img.onerror", onloadIndex)
        assertTrue(onerrorIndex > onloadIndex, "Should have img.onerror after img.onload")
        val onloadBlock = jsContent.substring(onloadIndex, onerrorIndex)
        assertTrue(onloadBlock.contains("catch"), "img.onload should contain try-catch for PNG extraction errors")
    }

    @Test
    fun `core js img onerror logs error`() {
        assertTrue(jsContent.contains("SVG image load error"), "img.onerror should log an error message")
    }

    @Test
    fun `core js extractPng checks canvas context`() {
        assertTrue(jsContent.contains("Failed to get 2D canvas context"), "Should check canvas.getContext result")
    }

    @Test
    fun `core js extractPng handles dark background with fillRect`() {
        assertTrue(jsContent.contains("fillRect"), "Should fill background based on dark/light theme")
    }

    @Test
    fun `core js createExportToolbar uses correct CSS classes`() {
        assertTrue(jsContent.contains("createExportToolbar"), "Should have createExportToolbar function")
        assertTrue(jsContent.contains("mermaid-export-toolbar"), "Should use mermaid-export-toolbar CSS class")
        assertTrue(jsContent.contains("mermaid-export-btn"), "Should use mermaid-export-btn CSS class")
    }

    @Test
    fun `core js has icon constants`() {
        assertTrue(jsContent.contains("ICON_COPY"), "Should have ICON_COPY constant")
        assertTrue(jsContent.contains("ICON_IMAGE"), "Should have ICON_IMAGE constant")
        assertTrue(jsContent.contains("ICON_SAVE"), "Should have ICON_SAVE constant")
        assertTrue(jsContent.contains("ICON_OPEN"), "Should have ICON_OPEN constant")
        assertTrue(jsContent.contains("ICON_OPEN: ICON_OPEN"), "ICON_OPEN should be exposed on __mermaidCore")
    }

    @Test
    fun `core js exposes utf8ToBase64 on public API`() {
        assertTrue(
            jsContent.contains("utf8ToBase64: utf8ToBase64"),
            "utf8ToBase64 should be exposed on __mermaidCore (used by the Markdown open-in-tab callback)"
        )
    }

    @Test
    fun `core js toolbar has open in new tab button first`() {
        assertTrue(jsContent.contains("callbacks.openInTab"), "createExportToolbar should support an openInTab callback")
        assertTrue(jsContent.contains("'Open in new tab'"), "Open in new tab button should have its tooltip title")
        val openIndex = jsContent.indexOf("callbacks.openInTab")
        val copySvgIndex = jsContent.indexOf("'Copy SVG'")
        assertTrue(
            openIndex in 1 until copySvgIndex,
            "Open in new tab should be the first button of the export group"
        )
    }

    @Test
    fun `core js export handlers have error handling`() {
        assertTrue(jsContent.contains("Copy SVG failed"), "Copy SVG handler should have error logging")
        assertTrue(jsContent.contains("Copy SVG: no SVG found"), "Copy SVG should log when no SVG found")
    }

    @Test
    fun `core js configures rendering limits for large diagrams`() {
        assertTrue(jsContent.contains("maxTextSize: cfg.maxTextSize ?? DEFAULT_MAX_TEXT_SIZE"),
            "Should configure maxTextSize for large diagrams")
        assertTrue(jsContent.contains("maxEdges: cfg.maxEdges ?? DEFAULT_MAX_EDGES"),
            "Should configure maxEdges: it is a secure key, only mermaid.initialize can raise it")
        assertTrue(jsContent.contains("const DEFAULT_MAX_TEXT_SIZE = 500000;"), "JS fallback should mirror the Kotlin default")
        assertTrue(jsContent.contains("const DEFAULT_MAX_EDGES = 2000;"), "JS fallback should mirror the Kotlin default")
    }

    @Test
    fun `core js detects the max text size fallback diagram`() {
        assertTrue(jsContent.contains("function detectTextSizeExceeded(source, svg)"), "Should have detectTextSizeExceeded")
        assertTrue(jsContent.contains("'Maximum text size in diagram exceeded'"),
            "Should look for the fallback diagram Mermaid renders instead of throwing")
        assertTrue(jsContent.contains("if (size <= limit) return null;"),
            "The marker alone is not enough: the source must actually exceed maxTextSize")
        assertTrue(jsContent.contains("Increase \"Max text size\" in ' + SETTINGS_PATH"), "Error should name the plugin setting")
        assertTrue(jsContent.contains("detectTextSizeExceeded: detectTextSizeExceeded"), "Should be exposed on __mermaidCore")
    }

    @Test
    fun `core js rewrites the edge limit error to name the plugin setting`() {
        assertTrue(jsContent.contains("function describeRenderError(err)"), "Should have describeRenderError")
        assertTrue(jsContent.contains("Edge limit exceeded"), "Should match Mermaid's edge limit error")
        assertTrue(jsContent.contains("Increase \"Max edges\" in ' + SETTINGS_PATH"), "Error should name the plugin setting")
        assertTrue(jsContent.contains("describeRenderError: describeRenderError"), "Should be exposed on __mermaidCore")
        assertTrue(jsContent.contains("info.summary = describeRenderError(err);"),
            "The rewritten (setting-aware) message should be the summary of the limit category")
        assertTrue(jsContent.contains("message: classified.raw,"),
            "parseErrorInfo should report the raw Mermaid message to the Kotlin annotator")
    }

    // --- Link handling tests ---

    @Test
    fun `core js exposes installLinkHandler`() {
        assertTrue(jsContent.contains("installLinkHandler"), "Should have installLinkHandler function")
        assertTrue(
            jsContent.contains("installLinkHandler: installLinkHandler"),
            "installLinkHandler should be exposed on __mermaidCore"
        )
    }

    @Test
    fun `core js link handler installs once per shadow root`() {
        assertTrue(
            jsContent.contains("WeakSet"),
            "Should track handled shadow roots in a WeakSet (roots persist across re-renders)"
        )
    }

    @Test
    fun `core js link handler intercepts anchor clicks`() {
        assertTrue(jsContent.contains("closest('a')"), "Should find the clicked anchor via closest")
        assertTrue(jsContent.contains("preventDefault"), "Should prevent default navigation for anchor clicks")
    }

    @Test
    fun `core js link handler reads raw href attributes first`() {
        assertTrue(
            jsContent.contains("getAttribute('xlink:href')"),
            "Should read the raw xlink:href attribute (SVG anchors, avoids origin resolution)"
        )
    }

    @Test
    fun `core js link handler only forwards http urls`() {
        assertTrue(jsContent.contains("isHttpUrl"), "Should pre-filter URLs to http(s) before calling openFn")
    }

    @Test
    fun `core js link handler guards drag and double clicks`() {
        assertTrue(jsContent.contains("e.detail"), "Should skip the second click of a double-click via e.detail")
        assertTrue(jsContent.contains("Math.hypot"), "Should measure mouse travel to swallow drag-ending clicks")
    }

    @Test
    fun `core js link handler blocks middle clicks`() {
        assertTrue(jsContent.contains("auxclick"), "Should prevent default on auxclick to block middle-click popups")
    }
}
