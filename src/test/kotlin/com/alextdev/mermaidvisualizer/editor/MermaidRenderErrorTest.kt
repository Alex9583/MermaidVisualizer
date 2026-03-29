package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
