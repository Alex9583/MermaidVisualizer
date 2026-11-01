package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.MermaidRenderErrorCategory
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `text size limit produces message naming the setting`() {
        val error = MermaidRenderError(
            "Diagram text size exceeds the limit (118048 > 100000 characters). Increase \"Max text size\" in Settings > Tools > Mermaid.",
            line = null, column = null,
        )
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("118048"), "Message should contain the actual size")
        assertTrue(enriched.message.contains("100000"), "Message should contain the limit")
        assertTrue(enriched.message.contains("Max text size"), "Message should name the setting")
        assertTrue(enriched.tooltip.contains("Settings &gt; Tools &gt; Mermaid"), "Tooltip should point to the settings page")
        assertFalse(enriched.tooltip.contains("autocompletion"), "Syntax hint is misleading for a size limit")
        assertFalse(enriched.message.contains("rendering failed"), "Should not fall through to the generic wrapper")
    }

    @Test
    fun `edge limit produces message naming the setting`() {
        val error = MermaidRenderError(
            "Diagram has more than 2000 edges. Increase \"Max edges\" in Settings > Tools > Mermaid.",
            line = null, column = null,
        )
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.message.contains("2000"), "Message should contain the limit")
        assertTrue(enriched.message.contains("Max edges"), "Message should name the setting")
        assertTrue(enriched.tooltip.contains("Settings &gt; Tools &gt; Mermaid"), "Tooltip should point to the settings page")
        assertFalse(enriched.tooltip.contains("autocompletion"), "Syntax hint is misleading for an edge limit")
    }

    @Test
    fun `raw Mermaid edge limit error is recognized`() {
        val raw = "Edge limit exceeded. 500 edges found, but the limit is 500.\n\n" +
            "Initialize mermaid with maxEdges set to a higher number to allow more edges.\n" +
            "You cannot set this config via configuration inside the diagram as it is a secure config.\n" +
            "You have to call mermaid.initialize."
        val enriched = MermaidRenderErrorEnricher.enrich(MermaidRenderError(raw, line = null, column = null))
        assertTrue(enriched.message.contains("500"), "Message should contain the limit")
        assertTrue(enriched.message.contains("Max edges"), "Message should name the setting")
        assertFalse(enriched.message.contains("mermaid.initialize"), "Upstream advice is meaningless from the IDE")
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

    // ── Summary path (errors classified by mermaid-core.js) ─────────────

    private val actionsLine = "Press Alt+Enter to copy the error or report it on GitHub."

    private fun classified(
        raw: String,
        summary: String,
        category: MermaidRenderErrorCategory,
        line: Int? = null,
        column: Int? = null,
    ) = MermaidRenderError(raw, line, column, summary, category)

    @Test
    fun `incomplete error uses the summary and explains that the preview keeps the last diagram`() {
        val error = classified(
            "Parse error on line 2:\n...A -->\n-------^\nExpecting 'AMP', got 'EOF'",
            "Line 4 looks unfinished: expected a node after \"A -->\"",
            MermaidRenderErrorCategory.INCOMPLETE, line = 4,
        )
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertEquals("Line 4 looks unfinished: expected a node after \"A -->\"", enriched.message)
        assertTrue(enriched.tooltip.startsWith("<html><body><b>Line 4 looks unfinished"), enriched.tooltip)
        assertTrue(enriched.tooltip.contains("last valid diagram"), "Tooltip should explain the preview behaviour")
        assertFalse(enriched.tooltip.contains("Expecting"), "An unfinished line does not need the raw parser output")
        assertFalse(enriched.tooltip.contains("autocompletion"), "No syntax lecture while the line is being typed")
        assertTrue(enriched.tooltip.endsWith("<i>$actionsLine</i></body></html>"), enriched.tooltip)
    }

    @Test
    fun `limit error uses the summary and the limit hint`() {
        val raw = "Diagram text size exceeds the limit (118048 > 100000 characters). Increase \"Max text size\" in Settings > Tools > Mermaid."
        val enriched = MermaidRenderErrorEnricher.enrich(classified(raw, raw, MermaidRenderErrorCategory.LIMIT))
        assertEquals(raw, enriched.message)
        assertTrue(enriched.tooltip.contains("cannot be raised from the diagram itself"), "Tooltip should carry the limit hint")
        assertFalse(enriched.tooltip.contains("autocompletion"), "Syntax hint is misleading for a limit")
        assertFalse(enriched.tooltip.contains("Original error"), "The summary already is the raw limit message")
        assertTrue(enriched.tooltip.contains(actionsLine))
    }

    @Test
    fun `syntax error shows the summary, the raw message and the syntax hint`() {
        val raw = "Parse error on line 3:\n...B --> C\n----------^\nExpecting 'SEMI', got 'ARROW'"
        val error = classified(raw, "Syntax error at line 3: unexpected \"-->\"", MermaidRenderErrorCategory.SYNTAX, line = 3, column = 9)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertEquals("Syntax error at line 3: unexpected \"-->\"", enriched.message)
        assertFalse(enriched.message.contains("rendering failed"), "No generic wrapper on the summary path")
        assertTrue(enriched.tooltip.contains("Original error:"))
        assertTrue(enriched.tooltip.contains("Parse error on line 3:<br>...B --&gt; C<br>----------^<br>"), "Raw line breaks are kept: ${enriched.tooltip}")
        assertTrue(enriched.tooltip.contains("autocompletion"), "Tooltip should include the syntax hint")
        assertFalse(enriched.tooltip.contains("may differ from the editor line"), "Raw line equals the annotated line: no note")
        assertTrue(enriched.tooltip.contains(actionsLine))
    }

    @Test
    fun `raw message quoting another line number gets a note`() {
        val error = classified(
            "Parse error on line 2:\n...A -- B\n-----^",
            "Syntax error at line 5: unexpected \"B\"",
            MermaidRenderErrorCategory.SYNTAX, line = 5,
        )
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.tooltip.contains("may differ from the editor line"), "Mermaid line 2 is document line 5: explain it")
    }

    @Test
    fun `raw message without line number gets no note`() {
        val error = classified("Cannot read properties of undefined", "Cannot read properties of undefined", MermaidRenderErrorCategory.GENERIC)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertFalse(enriched.tooltip.contains("may differ from the editor line"))
        assertEquals("Cannot read properties of undefined", enriched.message)
        assertTrue(enriched.tooltip.contains("autocompletion"))
        assertTrue(enriched.tooltip.contains(actionsLine))
    }

    @Test
    fun `unknown diagram lists the diagram types and redacts the diagram text`() {
        val raw = "No diagram type detected matching given configuration for text: flowchar LR\n    Secret --> Data"
        val error = classified(raw, "Unknown diagram type \"flowchar\": check the first line of the diagram", MermaidRenderErrorCategory.UNKNOWN_DIAGRAM)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertEquals("Unknown diagram type \"flowchar\": check the first line of the diagram", enriched.message)
        assertTrue(enriched.tooltip.contains("Available diagram types:"))
        assertTrue(enriched.tooltip.contains("gantt"), "Tooltip should list all types")
        assertTrue(enriched.tooltip.contains("flowchar LR"), "First line of the diagram is kept")
        assertFalse(enriched.tooltip.contains("Secret"), "The rest of the diagram is not quoted")
        assertTrue(enriched.tooltip.contains(actionsLine))
    }

    @Test
    fun `summary tooltip uses the given intention shortcut`() {
        val error = classified("x", "Line 1 looks unfinished", MermaidRenderErrorCategory.INCOMPLETE, line = 1)
        val enriched = MermaidRenderErrorEnricher.enrich(error, "\u2325\u23CE")
        assertTrue(enriched.tooltip.contains("Press \u2325\u23CE to copy the error"), enriched.tooltip)
        assertFalse(enriched.tooltip.contains("Alt+Enter"))
        val blankShortcut = MermaidRenderErrorEnricher.enrich(error, " ")
        assertTrue(blankShortcut.tooltip.contains(actionsLine), "A blank shortcut falls back to Alt+Enter")
    }

    @Test
    fun `summary and raw are escaped in the tooltip`() {
        val error = classified("Error: <img src=x>", "Unexpected <script>", MermaidRenderErrorCategory.SYNTAX)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertEquals("Unexpected <script>", enriched.message, "The annotation message is plain text")
        assertFalse(enriched.tooltip.contains("<script>"))
        assertFalse(enriched.tooltip.contains("<img"))
        assertTrue(enriched.tooltip.contains("Unexpected &lt;script&gt;"))
        assertTrue(enriched.tooltip.contains("Error: &lt;img src=x&gt;"))
    }

    @Test
    fun `space runs of the raw message are preserved`() {
        val error = classified("...A    --> B\n--------^", "Syntax error", MermaidRenderErrorCategory.SYNTAX)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.tooltip.contains("...A&nbsp;&nbsp;&nbsp; --&gt; B"), enriched.tooltip)
    }

    @Test
    fun `long raw message is capped in the tooltip`() {
        val raw = "Expecting " + "'TOKEN', ".repeat(1000)
        val error = classified(raw, "Syntax error at line 2: unexpected \"x\"", MermaidRenderErrorCategory.SYNTAX, line = 2)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertTrue(enriched.tooltip.length < 2_500, "Tooltip length ${enriched.tooltip.length}")
        assertTrue(enriched.tooltip.contains("\u2026"), "A capped raw message ends with an ellipsis")
    }

    @Test
    fun `blank summary keeps the legacy path`() {
        val error = MermaidRenderError("Something went wrong", null, null, summary = "   ", category = MermaidRenderErrorCategory.SYNTAX)
        val enriched = MermaidRenderErrorEnricher.enrich(error)
        assertEquals("Mermaid rendering failed: Something went wrong", enriched.message)
        assertFalse(enriched.tooltip.contains(actionsLine), "Legacy tooltips are unchanged")
    }

    @Test
    fun `legacy path ignores the intention shortcut`() {
        val error = MermaidRenderError("Parse error on line 3: Unexpected token", line = 3, column = null)
        assertEquals(MermaidRenderErrorEnricher.enrich(error), MermaidRenderErrorEnricher.enrich(error, "Ctrl+1"))
    }
}
