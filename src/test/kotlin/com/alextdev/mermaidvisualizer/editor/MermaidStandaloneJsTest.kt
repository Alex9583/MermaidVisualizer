package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidStandaloneJsTest {

    private lateinit var jsContent: String
    private lateinit var cssContent: String

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
    fun `standalone js contains atob for base64 decoding`() {
        assertTrue(jsContent.contains("atob"), "Should use atob for base64 decoding")
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
        assertTrue(jsContent.contains("passive"), "Should use passive scroll listener")
    }

    @Test
    fun `standalone js has error handling for mermaid initialization`() {
        assertTrue(jsContent.contains("mermaid.initialize failed"), "Should catch and report mermaid.initialize errors")
    }

    @Test
    fun `standalone js exposes showError on window`() {
        assertTrue(jsContent.contains("window.__showError"), "Should expose __showError for Kotlin-side fallback error display")
    }
}
