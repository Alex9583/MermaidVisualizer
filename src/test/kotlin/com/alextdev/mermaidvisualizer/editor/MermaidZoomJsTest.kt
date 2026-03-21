package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
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

    @Test
    fun `zoom js exposes initMermaidZoom`() {
        assertTrue(jsContent.contains("__initMermaidZoom"), "Should expose __initMermaidZoom function")
    }

    @Test
    fun `zoom js creates mermaid-zoom-viewport`() {
        assertTrue(jsContent.contains("mermaid-zoom-viewport"), "Should create mermaid-zoom-viewport element")
    }

    @Test
    fun `zoom js creates mermaid-zoom-content`() {
        assertTrue(jsContent.contains("mermaid-zoom-content"), "Should create mermaid-zoom-content element")
    }

    @Test
    fun `zoom js uses ResizeObserver`() {
        assertTrue(jsContent.contains("ResizeObserver"), "Should use ResizeObserver for responsive fit")
    }

    @Test
    fun `zoom js handles wheel events`() {
        assertTrue(jsContent.contains("wheel"), "Should handle wheel events for zoom")
    }

    @Test
    fun `zoom js handles mouse events for pan`() {
        assertTrue(jsContent.contains("mousedown"), "Should handle mousedown for pan start")
        assertTrue(jsContent.contains("mousemove"), "Should handle mousemove for panning")
        assertTrue(jsContent.contains("mouseup"), "Should handle mouseup for pan end")
    }

    @Test
    fun `zoom js has computeFitScale`() {
        assertTrue(jsContent.contains("computeFitScale"), "Should have computeFitScale function")
    }

    @Test
    fun `zoom js applies CSS transform`() {
        assertTrue(jsContent.contains("transform"), "Should apply CSS transform for zoom/pan")
    }

    @Test
    fun `zoom js clamps scale to min and max`() {
        assertTrue(jsContent.contains("0.1"), "Should have minimum scale of 0.1")
        assertTrue(jsContent.contains("5.0"), "Should have maximum scale of 5.0")
    }

    @Test
    fun `zoom js creates toolbar divider`() {
        assertTrue(jsContent.contains("mermaid-toolbar-divider"), "Should create toolbar divider element")
    }

    @Test
    fun `zoom js creates zoom label`() {
        assertTrue(jsContent.contains("mermaid-zoom-label"), "Should create zoom label element")
    }

    @Test
    fun `zoom js handles double-click`() {
        assertTrue(jsContent.contains("dblclick"), "Should handle double-click for fit/actual toggle")
    }

    @Test
    fun `zoom js handles keyboard shortcuts`() {
        assertTrue(jsContent.contains("keydown"), "Should handle keyboard shortcuts")
    }

    @Test
    fun `zoom js uses WeakMap for state`() {
        assertTrue(jsContent.contains("WeakMap"), "Should use WeakMap for per-shadowRoot state")
    }

    @Test
    fun `zoom js uses clientWidth for transform-stable dimensions`() {
        assertTrue(jsContent.contains("svg.clientWidth"), "Should use clientWidth for dimensions unaffected by CSS transforms")
        assertTrue(jsContent.contains("svg.clientHeight"), "Should use clientHeight for dimensions unaffected by CSS transforms")
    }

    @Test
    fun `zoom js reads SVG viewBox for natural dimensions`() {
        assertTrue(jsContent.contains("viewBox"), "Should read SVG viewBox as fallback for natural dimensions")
    }

    @Test
    fun `zoom js uses SVG natural dimensions for fit`() {
        assertTrue(jsContent.contains("getAttribute('width')"), "Should read SVG natural width from attributes")
        assertTrue(jsContent.contains("getAttribute('height')"), "Should read SVG natural height from attributes")
    }

    @Test
    fun `zoom js supports constrainSvg option`() {
        assertTrue(jsContent.contains("constrainSvg"), "Should support constrainSvg option")
        assertTrue(jsContent.contains("unconstrained"), "Should add 'unconstrained' class when constrainSvg is false")
    }

    @Test
    fun `zoom js always allows panning`() {
        assertTrue(jsContent.contains("can-pan"), "Should set can-pan class for grab cursor")
        assertTrue(jsContent.contains("mousedown"), "Should handle mousedown for pan start")
    }

    @Test
    fun `zoom js uses passive wheel listener when modifier required`() {
        assertTrue(jsContent.contains("passive: wheelRequiresModifier"), "Should use passive wheel listener when wheelRequiresModifier is true")
    }

}
