package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidZoomJsTest {

    private lateinit var jsContent: String

    @BeforeAll
    fun loadResources() {
        val jsStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-zoom.js")
        ) { "web/mermaid-zoom.js should be on the classpath" }
        jsContent = jsStream.use { it.reader(Charsets.UTF_8).readText() }
    }

    // ====== Shared utilities ======

    @Nested
    inner class SharedUtilities {

        @Test
        fun `exposes initMermaidZoom`() {
            assertTrue(jsContent.contains("__initMermaidZoom"), "Should expose __initMermaidZoom function")
        }

        @Test
        fun `uses WeakMap for state`() {
            assertTrue(jsContent.contains("WeakMap"), "Should use WeakMap for per-shadowRoot state")
        }

        @Test
        fun `clamps scale to min and max`() {
            assertTrue(jsContent.contains("0.1"), "Should have minimum scale of 0.1")
            assertTrue(jsContent.contains("5.0"), "Should have maximum scale of 5.0")
        }

        @Test
        fun `has getSvgIntrinsicSize`() {
            assertTrue(
                jsContent.contains("getSvgIntrinsicSize"),
                "Should have getSvgIntrinsicSize function for reading immutable viewBox dimensions"
            )
        }

        @Test
        fun `reads SVG viewBox for intrinsic dimensions`() {
            assertTrue(jsContent.contains("viewBox"), "Should read SVG viewBox for dimensions")
        }

        @Test
        fun `buildZoomControls is mode-agnostic via actions`() {
            assertTrue(
                jsContent.contains("actions.zoomIn"),
                "buildZoomControls should delegate to actions.zoomIn callback"
            )
            assertTrue(
                jsContent.contains("actions.fitToWindow"),
                "buildZoomControls should delegate to actions.fitToWindow callback"
            )
        }

        @Test
        fun `creates toolbar divider`() {
            assertTrue(jsContent.contains("mermaid-toolbar-divider"), "Should create toolbar divider element")
        }

        @Test
        fun `creates zoom label`() {
            assertTrue(jsContent.contains("mermaid-zoom-label"), "Should create zoom label element")
        }

        @Test
        fun `uses ResizeObserver`() {
            assertTrue(jsContent.contains("ResizeObserver"), "Should use ResizeObserver for responsive layout")
        }

        @Test
        fun `handles wheel events`() {
            assertTrue(jsContent.contains("wheel"), "Should handle wheel events for zoom")
        }

        @Test
        fun `handles mouse events for pan`() {
            assertTrue(jsContent.contains("mousedown"), "Should handle mousedown for pan start")
            assertTrue(jsContent.contains("mousemove"), "Should handle mousemove for panning")
            assertTrue(jsContent.contains("mouseup"), "Should handle mouseup for pan end")
        }

        @Test
        fun `handles double-click`() {
            assertTrue(jsContent.contains("dblclick"), "Should handle double-click for fit/actual toggle")
        }

        @Test
        fun `applies CSS transform for zoom and pan`() {
            assertTrue(jsContent.contains("transform"), "Should apply CSS transform for zoom/pan")
        }

        @Test
        fun `creates mermaid-zoom-viewport`() {
            assertTrue(jsContent.contains("mermaid-zoom-viewport"), "Should create mermaid-zoom-viewport element")
        }

        @Test
        fun `creates mermaid-zoom-content`() {
            assertTrue(jsContent.contains("mermaid-zoom-content"), "Should create mermaid-zoom-content element")
        }

        @Test
        fun `has computeFitScale`() {
            assertTrue(jsContent.contains("computeFitScale"), "Should have computeFitScale function")
        }

        @Test
        fun `reads SVG attributes for natural dimensions`() {
            assertTrue(jsContent.contains("getAttribute('width')"), "Should read SVG natural width from attributes")
            assertTrue(jsContent.contains("getAttribute('height')"), "Should read SVG natural height from attributes")
        }

        @Test
        fun `uses passive wheel listener when modifier required`() {
            assertTrue(
                jsContent.contains("passive: state.wheelRequiresModifier"),
                "Should use passive wheel listener based on state.wheelRequiresModifier"
            )
        }
    }

    // ====== Standalone mode ======

    @Nested
    inner class StandaloneMode {

        @Test
        fun `has initStandaloneZoom function`() {
            assertTrue(jsContent.contains("initStandaloneZoom"), "Should have initStandaloneZoom function")
        }

        @Test
        fun `uses clientWidth for transform-stable dimensions`() {
            assertTrue(
                jsContent.contains("svg.clientWidth"),
                "Standalone should use clientWidth for dimensions unaffected by CSS transforms"
            )
            assertTrue(
                jsContent.contains("svg.clientHeight"),
                "Standalone should use clientHeight for dimensions unaffected by CSS transforms"
            )
        }

        @Test
        fun `supports constrainSvg option`() {
            assertTrue(jsContent.contains("constrainSvg"), "Should support constrainSvg option")
            assertTrue(
                jsContent.contains("unconstrained"),
                "Should add 'unconstrained' class when constrainSvg is false"
            )
        }

        @Test
        fun `always allows panning`() {
            assertTrue(jsContent.contains("can-pan"), "Should set can-pan class for grab cursor")
        }

        @Test
        fun `handles keyboard shortcuts`() {
            assertTrue(jsContent.contains("keydown"), "Should handle keyboard shortcuts")
        }
    }

    // ====== Inline mode ======

    @Nested
    inner class InlineMode {

        @Test
        fun `has initInlineZoom function`() {
            assertTrue(jsContent.contains("initInlineZoom"), "Should have initInlineZoom function")
        }

        @Test
        fun `supports layoutMode inline`() {
            assertTrue(
                jsContent.contains("layoutMode"),
                "Should support layoutMode option for mode dispatch"
            )
        }

        @Test
        fun `dispatches to inline mode based on layoutMode`() {
            assertTrue(
                jsContent.contains("options.layoutMode === 'inline'"),
                "Public API should dispatch to initInlineZoom when layoutMode is 'inline'"
            )
        }

        @Test
        fun `computes explicit viewport height from SVG dimensions`() {
            assertTrue(
                jsContent.contains("computeViewportHeight"),
                "Inline mode should compute explicit viewport height from SVG proportions"
            )
        }

        @Test
        fun `sets viewport height in pixels`() {
            assertTrue(
                jsContent.contains("viewport.style.height"),
                "Inline mode should set explicit pixel height on viewport"
            )
        }

        @Test
        fun `uses fitMode fit`() {
            assertTrue(
                jsContent.contains("fitMode: 'fit'"),
                "Inline mode should use fitMode 'fit' to fit diagram within bounded viewport"
            )
        }

        @Test
        fun `has safe fallback for unreadable SVG dimensions`() {
            assertTrue(
                jsContent.contains("if (!intrinsic) return"),
                "Inline mode should abort initialization when SVG dimensions are unreadable"
            )
        }

        @Test
        fun `reuses same viewport class as standalone`() {
            assertTrue(
                jsContent.contains("wrapInZoomViewport"),
                "Inline mode should reuse the same wrapInZoomViewport as standalone"
            )
        }

        @Test
        fun `calls fitToWindow on init`() {
            // Verify inline mode starts in fit mode (unlike standalone which starts in manual)
            assertTrue(
                jsContent.contains("fitToWindow(state)"),
                "Inline mode should call fitToWindow for initial fit-to-width"
            )
        }

        @Test
        fun `has dedicated ResizeObserver`() {
            assertTrue(
                jsContent.contains("setupInlineResizeObserver"),
                "Should have dedicated ResizeObserver that recomputes viewport height"
            )
        }

        @Test
        fun `caps viewport height relative to window`() {
            assertTrue(
                jsContent.contains("window.innerHeight"),
                "Inline mode should cap viewport height relative to window height"
            )
        }

        @Test
        fun `expands viewport when page has available space`() {
            assertTrue(
                jsContent.contains("expandIfSpaceAvailable"),
                "Inline mode should expand viewport when page has available space (diagram-only)"
            )
        }

        @Test
        fun `ResizeObserver only reacts to width changes`() {
            assertTrue(
                jsContent.contains("lastWidth"),
                "Inline ResizeObserver should track width to avoid infinite loops from height changes"
            )
        }
    }
}