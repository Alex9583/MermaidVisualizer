package com.alextdev.mermaidvisualizer.markdown

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MermaidResourceLoadingTest {

    @Test
    fun `mermaid min js is loadable and large enough`() {
        val stream = javaClass.classLoader.getResourceAsStream("web/mermaid.min.js")
        assertNotNull(stream, "web/mermaid.min.js should be on the classpath")
        val bytes = stream!!.use { it.readBytes() }
        assertTrue(bytes.size > 100_000, "mermaid.min.js should be >100KB (got ${bytes.size})")
    }

    @Test
    fun `mermaid render js is loadable and contains expected symbols`() {
        val stream = javaClass.classLoader.getResourceAsStream("web/mermaid-render.js")
        assertNotNull(stream, "web/mermaid-render.js should be on the classpath")
        val content = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }
        assertTrue(content.contains("renderDiagrams"), "mermaid-render.js should contain 'renderDiagrams'")
        assertTrue(content.contains("initMermaid"), "mermaid-render.js should contain 'initMermaid'")
    }

    @Test
    fun `mermaid core js is loadable and contains expected symbols`() {
        val stream = javaClass.classLoader.getResourceAsStream("web/mermaid-core.js")
        assertNotNull(stream, "web/mermaid-core.js should be on the classpath")
        val content = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }
        assertTrue(content.contains("__mermaidCore"), "mermaid-core.js should contain '__mermaidCore'")
        assertTrue(content.contains("mermaid.initialize"), "mermaid-core.js should contain 'mermaid.initialize'")
    }

    @Test
    fun `mermaid preview css is loadable and contains expected selector`() {
        val stream = javaClass.classLoader.getResourceAsStream("web/mermaid-preview.css")
        assertNotNull(stream, "web/mermaid-preview.css should be on the classpath")
        val content = stream!!.use { it.readBytes().toString(Charsets.UTF_8) }
        assertTrue(content.contains(".mermaid-diagram"), "mermaid-preview.css should contain '.mermaid-diagram'")
    }
}
