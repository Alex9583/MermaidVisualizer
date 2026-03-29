package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidRenderErrorEnricherTest {

    @Test
    fun `diagram not found produces enriched message with valid types`() {
        val error = MermaidRenderError("Diagram flowchar not found.", line = null, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("flowchar"), "Message should mention the wrong type")
        assertTrue(enriched.message.contains("flowchart"), "Message should suggest valid types")
        assertTrue(enriched.message.contains("sequenceDiagram"), "Message should include popular types")
        assertTrue(enriched.tooltip.contains("<html>"), "Tooltip should be HTML")
        assertTrue(enriched.tooltip.contains("flowchar"), "Tooltip should mention wrong type")
        assertTrue(enriched.tooltip.contains("gantt"), "Tooltip should list all types")
    }

    @Test
    fun `parse error on line produces enriched message`() {
        val error = MermaidRenderError("Parse error on line 3: Unexpected token", line = 3, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("3"), "Message should contain line number")
        assertTrue(enriched.message.contains("Unexpected token"), "Message should contain detail")
        assertTrue(enriched.tooltip.contains("<html>"), "Tooltip should be HTML")
    }

    @Test
    fun `syntax error on line produces enriched message`() {
        val error = MermaidRenderError("Syntax error on line 5: invalid token", line = 5, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("5"), "Message should contain line number")
    }

    @Test
    fun `expecting got produces enriched message`() {
        val error = MermaidRenderError("Expecting 'NEWLINE', got '->'", line = 2, column = 5)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("NEWLINE"), "Message should contain expected token")
        assertTrue(enriched.message.contains("->"), "Message should contain actual token")
        assertTrue(enriched.tooltip.contains("<html>"), "Tooltip should be HTML")
    }

    @Test
    fun `lexical error produces enriched message`() {
        val error = MermaidRenderError("Lexical error on line 7: Unrecognized text.", line = 7, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("7"), "Message should contain line number")
        assertTrue(enriched.message.contains("Unrecognized text"), "Message should contain detail")
    }

    @Test
    fun `generic error uses fallback`() {
        val error = MermaidRenderError("Something went wrong", line = null, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("Something went wrong"), "Fallback should preserve raw message")
        assertTrue(enriched.tooltip.contains("<html>"), "Tooltip should be HTML")
        assertTrue(enriched.tooltip.contains("autocompletion"), "Tooltip should include hint")
    }

    @Test
    fun `tooltip escapes HTML in raw message`() {
        val error = MermaidRenderError("Error: <script>alert('xss')</script>", line = null, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertFalse(enriched.tooltip.contains("<script>"), "Tooltip should escape HTML tags")
        assertTrue(enriched.tooltip.contains("&lt;script&gt;"), "Tooltip should contain escaped tags")
    }

    @Test
    fun `diagram not found tooltip contains all 29 diagram keywords`() {
        val error = MermaidRenderError("Diagram xyz not found.", line = null, column = null)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        val diagramKeywords = listOf(
            "flowchart", "graph", "sequenceDiagram", "classDiagram", "erDiagram",
            "stateDiagram-v2", "stateDiagram", "gantt", "pie", "gitGraph",
            "mindmap", "timeline", "journey", "sankey-beta", "xychart-beta",
            "quadrantChart", "requirementDiagram",
            "C4Context", "C4Container", "C4Component", "C4Dynamic", "C4Deployment",
            "zenuml", "kanban", "block-beta", "packet-beta", "architecture-beta",
            "venn-beta", "ishikawa-beta",
        )
        for (keyword in diagramKeywords) {
            assertTrue(enriched.tooltip.contains(keyword), "Tooltip should contain '$keyword'")
        }
    }
}
