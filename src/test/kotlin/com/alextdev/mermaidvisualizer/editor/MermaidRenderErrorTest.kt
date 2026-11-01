package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidRenderErrorTest {

    @Test
    fun `MermaidRenderError stores message`() {
        val error = MermaidRenderError("Parse error on line 3", 3, null)
        assertEquals("Parse error on line 3", error.message)
        assertEquals(3, error.line)
        assertNull(error.column)
    }

    @Test
    fun `MermaidRenderError with line and column`() {
        val error = MermaidRenderError("Error at column 5", 2, 5)
        assertEquals(2, error.line)
        assertEquals(5, error.column)
    }

    @Test
    fun `MermaidRenderError with null line and column`() {
        val error = MermaidRenderError("Unknown error", null, null)
        assertNull(error.line)
        assertNull(error.column)
    }

    @Test
    fun `three-argument constructor defaults to no summary and the generic category`() {
        val error = MermaidRenderError("Something failed", 1, 2)
        assertNull(error.summary)
        assertEquals(MermaidRenderErrorCategory.GENERIC, error.category)
    }

    @Test
    fun `MermaidRenderError stores summary and category`() {
        val error = MermaidRenderError(
            "Parse error on line 2:", 4, 7,
            summary = "Line 4 looks unfinished",
            category = MermaidRenderErrorCategory.INCOMPLETE,
        )
        assertEquals("Line 4 looks unfinished", error.summary)
        assertEquals(MermaidRenderErrorCategory.INCOMPLETE, error.category)
    }

    @Test
    fun `line below 1 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { MermaidRenderError("e", 0, null) }
        assertThrows(IllegalArgumentException::class.java) { MermaidRenderError("e", -3, null) }
    }

    @Test
    fun `column below 1 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { MermaidRenderError("e", 1, 0) }
        assertThrows(IllegalArgumentException::class.java) { MermaidRenderError("e", null, -1) }
    }

    // ── MermaidRenderErrorCategory.fromPayload ──────────────────────────

    @Test
    fun `fromPayload maps every category produced by mermaid-core js`() {
        assertEquals(MermaidRenderErrorCategory.INCOMPLETE, MermaidRenderErrorCategory.fromPayload("incomplete"))
        assertEquals(MermaidRenderErrorCategory.SYNTAX, MermaidRenderErrorCategory.fromPayload("syntax"))
        assertEquals(MermaidRenderErrorCategory.UNKNOWN_DIAGRAM, MermaidRenderErrorCategory.fromPayload("unknown-diagram"))
        assertEquals(MermaidRenderErrorCategory.LIMIT, MermaidRenderErrorCategory.fromPayload("limit"))
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload("generic"))
    }

    @Test
    fun `fromPayload defaults to generic for missing or unknown values`() {
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload(null))
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload(""))
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload("timeout"))
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload("SYNTAX"), "Payload values are case-sensitive")
        assertEquals(MermaidRenderErrorCategory.GENERIC, MermaidRenderErrorCategory.fromPayload("unknown_diagram"))
    }

    @Test
    fun `payload values round-trip and match the JS categories`() {
        for (category in MermaidRenderErrorCategory.entries) {
            assertEquals(category, MermaidRenderErrorCategory.fromPayload(category.payloadValue))
        }
        assertEquals(
            setOf("incomplete", "syntax", "unknown-diagram", "limit", "generic"),
            MermaidRenderErrorCategory.entries.map { it.payloadValue }.toSet(),
        )
    }

    // ── SuggestDiagramTypeFix.suggestClosest ────────────────────────────

    @Test
    fun `suggestClosest returns flowchart for flowchar`() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("flowchar")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("flowchart", suggestions.first())
    }

    @Test
    fun `suggestClosest returns sequenceDiagram for seqDiagram`() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("seqDiagram")
        assertTrue(suggestions.isNotEmpty())
        // sequenceDiagram should be among top suggestions
        assertTrue(suggestions.any { it == "sequenceDiagram" })
    }

    @Test
    fun `suggestClosest returns at most 3 results`() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("xyz", 3)
        assertTrue(suggestions.size <= 3)
    }

    @Test
    fun `suggestClosest is case insensitive for matching`() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("FLOWCHART")
        assertTrue(suggestions.contains("flowchart"))
    }

    @Test
    fun `suggestClosest returns graph first for grap`() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("grap")
        assertTrue(suggestions.isNotEmpty())
        assertEquals("graph", suggestions.first(),
            "graph (distance 1) should be suggested before gantt (distance 4)")
    }

    @Test
    fun `editDistance for arrow pairs`() {
        val ed = MermaidSuggestDiagramTypeFix::editDistance
        // ->> vs flowchart arrows
        assertEquals(1, ed("->>", "-->"))
        assertEquals(2, ed("->>", "--->"))
        assertEquals(2, ed("->>", "==>"))
        // -->> vs flowchart arrows
        assertEquals(1, ed("-->>", "-->"))
        assertEquals(1, ed("-->>", "--->"))
        assertEquals(3, ed("-->>", "==>"))
        // identical
        assertEquals(0, ed("-->", "-->"))
    }
}
