package com.alextdev.mermaidvisualizer.lang.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidCompletionContributorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private fun completionsAt(text: String): List<String> {
        myFixture.configureByText("test.mmd", text)
        val result = myFixture.completeBasic()
        return result?.map { it.lookupString } ?: emptyList()
    }

    // ── Diagram type completion ────────────────────────────────────────

    fun testDiagramTypeInEmptyFile() {
        val completions = completionsAt("<caret>")
        assertTrue("Expected flowchart", completions.contains("flowchart"))
        assertTrue("Expected sequenceDiagram", completions.contains("sequenceDiagram"))
        assertTrue("Expected classDiagram", completions.contains("classDiagram"))
        assertTrue("Expected erDiagram", completions.contains("erDiagram"))
        assertTrue("Expected gantt", completions.contains("gantt"))
        assertTrue("Expected pie", completions.contains("pie"))
        assertTrue("Expected gitGraph", completions.contains("gitGraph"))
    }

    fun testDiagramTypeWithPrefix() {
        // "flow" prefix matches only "flowchart", so completeBasic() auto-inserts it (returns null)
        myFixture.configureByText("test.mmd", "flow<caret>")
        val result = myFixture.completeBasic()
        if (result == null) {
            // Single match was auto-completed — verify the document contains "flowchart"
            assertTrue("Expected auto-completed flowchart", myFixture.editor.document.text.contains("flowchart"))
        } else {
            assertTrue("Expected flowchart in list", result.any { it.lookupString == "flowchart" })
        }
    }

    fun testDiagramTypeAfterComment() {
        val completions = completionsAt("%% comment\n<caret>")
        assertTrue("Expected flowchart", completions.contains("flowchart"))
        assertTrue("Expected sequenceDiagram", completions.contains("sequenceDiagram"))
    }

    fun testNoDiagramTypeInsideBody() {
        val completions = completionsAt("flowchart LR\n    <caret>")
        assertFalse("Should not offer sequenceDiagram inside body", completions.contains("sequenceDiagram"))
        assertFalse("Should not offer gantt inside body", completions.contains("gantt"))
        assertFalse("Should not offer erDiagram inside body", completions.contains("erDiagram"))
    }

    fun testDiagramTypeAfterPreviousDiagram() {
        // After a complete diagram, the parser still considers subsequent lines as part of the body.
        // Diagram types are offered at positions outside any diagram body (e.g. start of file).
        val completions = completionsAt("flowchart LR\n    A --> B\n\n<caret>")
        // Caret is still in flowchart body — keyword completion should be available, not diagram types
        assertFalse("Should not offer diagram types inside body", completions.contains("sequenceDiagram"))
        assertTrue("Should have flowchart keywords", completions.contains("subgraph") || completions.contains("style"))
    }

    // ── Keyword completion ─────────────────────────────────────────────

    fun testFlowchartKeywords() {
        val completions = completionsAt("flowchart LR\n    <caret>")
        assertTrue("Expected subgraph", completions.contains("subgraph"))
        assertTrue("Expected style", completions.contains("style"))
        assertTrue("Expected classDef", completions.contains("classDef"))
        assertTrue("Expected direction", completions.contains("direction"))
    }

    fun testSequenceKeywords() {
        val completions = completionsAt("sequenceDiagram\n    <caret>")
        assertTrue("Expected participant", completions.contains("participant"))
        assertTrue("Expected actor", completions.contains("actor"))
        assertTrue("Expected loop", completions.contains("loop"))
        assertTrue("Expected alt", completions.contains("alt"))
        assertTrue("Expected note", completions.contains("note"))
    }

    fun testNoSequenceKeywordsInFlowchart() {
        val completions = completionsAt("flowchart LR\n    <caret>")
        assertFalse("Should not offer participant", completions.contains("participant"))
        assertFalse("Should not offer actor", completions.contains("actor"))
        assertFalse("Should not offer loop", completions.contains("loop"))
    }

    fun testNoFlowchartKeywordsInSequence() {
        val completions = completionsAt("sequenceDiagram\n    <caret>")
        assertFalse("Should not offer subgraph", completions.contains("subgraph"))
        assertFalse("Should not offer classDef", completions.contains("classDef"))
    }

    fun testGanttKeywords() {
        val completions = completionsAt("gantt\n    <caret>")
        assertTrue("Expected title", completions.contains("title"))
        assertTrue("Expected section", completions.contains("section"))
        assertTrue("Expected dateFormat", completions.contains("dateFormat"))
        assertTrue("Expected axisFormat", completions.contains("axisFormat"))
    }

    fun testGitGraphKeywords() {
        val completions = completionsAt("gitGraph\n    <caret>")
        assertTrue("Expected commit", completions.contains("commit"))
        assertTrue("Expected branch", completions.contains("branch"))
        assertTrue("Expected checkout", completions.contains("checkout"))
        assertTrue("Expected merge", completions.contains("merge"))
    }

    fun testPieKeywords() {
        val completions = completionsAt("pie\n    <caret>")
        assertTrue("Expected title", completions.contains("title"))
        assertTrue("Expected showData", completions.contains("showData"))
    }

    fun testClassDiagramKeywords() {
        val completions = completionsAt("classDiagram\n    <caret>")
        assertTrue("Expected namespace", completions.contains("namespace"))
        assertTrue("Expected annotation", completions.contains("annotation"))
        assertTrue("Expected class", completions.contains("class"))
    }

    fun testKeywordsInsideBlock() {
        val completions = completionsAt("flowchart LR\n    subgraph test\n        <caret>\n    end")
        assertTrue("Expected subgraph (nested)", completions.contains("subgraph"))
        assertTrue("Expected style", completions.contains("style"))
    }

    fun testEndKeywordInsideBlock() {
        val completions = completionsAt("flowchart LR\n    subgraph test\n        A --> B\n    <caret>\n    end")
        assertTrue("Expected end", completions.contains("end"))
    }

    fun testSharedKeywords() {
        val completions = completionsAt("flowchart LR\n    <caret>")
        assertTrue("Expected accTitle", completions.contains("accTitle"))
        assertTrue("Expected accDescr", completions.contains("accDescr"))
    }

    // ── Flowchart direction completion ─────────────────────────────────

    fun testFlowchartDirectionCompletion() {
        val completions = completionsAt("flowchart <caret>")
        assertTrue("Expected LR", completions.contains("LR"))
        assertTrue("Expected RL", completions.contains("RL"))
        assertTrue("Expected TD", completions.contains("TD"))
        assertTrue("Expected TB", completions.contains("TB"))
        assertTrue("Expected BT", completions.contains("BT"))
    }

    fun testNoDirectionInSequence() {
        val completions = completionsAt("sequenceDiagram\n    <caret>")
        assertFalse("Should not offer LR", completions.contains("LR"))
        assertFalse("Should not offer TD", completions.contains("TD"))
    }

    // ── Node name completion ───────────────────────────────────────────

    fun testNodeNameInFlowchart() {
        val completions = completionsAt("flowchart LR\n    Alpha --> Beta\n    <caret>")
        assertTrue("Expected Alpha", completions.contains("Alpha"))
        assertTrue("Expected Beta", completions.contains("Beta"))
    }

    fun testNodeNameWithPrefix() {
        val completions = completionsAt("flowchart LR\n    Alpha --> Beta\n    A<caret>")
        assertTrue("Expected Alpha", completions.contains("Alpha"))
    }

    fun testNodeNameAfterArrow() {
        val completions = completionsAt("flowchart LR\n    A --> B\n    A --> <caret>")
        assertTrue("Expected A", completions.contains("A"))
        assertTrue("Expected B", completions.contains("B"))
    }

    fun testNodeNameNotDuplicated() {
        val completions = completionsAt("flowchart LR\n    A --> B\n    A --> B\n    <caret>")
        val aCount = completions.count { it == "A" }
        val bCount = completions.count { it == "B" }
        assertEquals("A should appear once", 1, aCount)
        assertEquals("B should appear once", 1, bCount)
    }

    fun testNodeNamesFromMultipleStatements() {
        val completions = completionsAt("flowchart LR\n    A --> B\n    C --> D\n    <caret>")
        assertTrue("Expected A", completions.contains("A"))
        assertTrue("Expected B", completions.contains("B"))
        assertTrue("Expected C", completions.contains("C"))
        assertTrue("Expected D", completions.contains("D"))
    }

    fun testNodeNameInSubgraph() {
        val completions = completionsAt(
            "flowchart LR\n    A --> B\n    subgraph test\n        <caret>\n    end"
        )
        assertTrue("Expected A from outer scope", completions.contains("A"))
        assertTrue("Expected B from outer scope", completions.contains("B"))
    }

    fun testNodeNameScopedToDiagram() {
        val completions = completionsAt(
            "flowchart LR\n    NodeA --> NodeB\n\nsequenceDiagram\n    <caret>"
        )
        // Nodes from flowchart should NOT appear in sequence diagram
        assertFalse("Should not offer NodeA from other diagram", completions.contains("NodeA"))
        assertFalse("Should not offer NodeB from other diagram", completions.contains("NodeB"))
    }

    // ── Negative / edge cases ──────────────────────────────────────────

    fun testCompletionInEmptyBody() {
        val completions = completionsAt("flowchart LR\n<caret>")
        assertFalse("Should have completions", completions.isEmpty())
    }

    fun testCompletionWithPartialKeyword() {
        // "sub" prefix matches only "subgraph", so completeBasic() may auto-insert it
        myFixture.configureByText("test.mmd", "flowchart LR\n    sub<caret>")
        val result = myFixture.completeBasic()
        if (result == null) {
            assertTrue("Expected auto-completed subgraph", myFixture.editor.document.text.contains("subgraph"))
        } else {
            assertTrue("Expected subgraph in list", result.any { it.lookupString == "subgraph" })
        }
    }

    fun testC4Keywords() {
        val completions = completionsAt("C4Context\n    <caret>")
        assertTrue("Expected Person", completions.contains("Person"))
        assertTrue("Expected System", completions.contains("System"))
        assertTrue("Expected Rel", completions.contains("Rel"))
    }

    fun testStateDiagramKeywords() {
        val completions = completionsAt("stateDiagram-v2\n    <caret>")
        assertTrue("Expected state", completions.contains("state"))
    }

    fun testMindmapKeywords() {
        val completions = completionsAt("mindmap\n    <caret>")
        assertTrue("Expected root", completions.contains("root"))
    }

    // ── Arrow completion ───────────────────────────────────────────────

    fun testArrowsInFlowchartAfterIdentifier() {
        val completions = completionsAt("flowchart LR\n    A <caret>")
        assertTrue("Expected -->", completions.contains("-->"))
        assertTrue("Expected ==>", completions.contains("==>"))
    }

    fun testArrowsInSequenceAfterIdentifier() {
        val completions = completionsAt("sequenceDiagram\n    participant Alice\n    Alice <caret>")
        assertTrue("Expected ->>", completions.contains("->>"))
        assertTrue("Expected -->>", completions.contains("-->>"))
    }

    fun testNoArrowsAtLineStart() {
        val completions = completionsAt("flowchart LR\n    <caret>")
        assertFalse("Should not offer --> at line start", completions.contains("-->"))
        assertFalse("Should not offer ==> at line start", completions.contains("==>"))
    }

    // ── Graph keyword (flowchart alias) ────────────────────────────────

    fun testGraphKeywordsMatchFlowchart() {
        val completions = completionsAt("graph TD\n    <caret>")
        assertTrue("Expected subgraph in graph", completions.contains("subgraph"))
        assertTrue("Expected style in graph", completions.contains("style"))
    }

    // ── StateDiagram v1 ────────────────────────────────────────────────

    fun testStateDiagramV1Keywords() {
        val completions = completionsAt("stateDiagram\n    <caret>")
        assertTrue("Expected state in v1", completions.contains("state"))
    }

    // ── No completion in strings / comments ────────────────────────────

    fun testNoCompletionInsideDoubleQuotedString() {
        val completions = completionsAt("flowchart LR\n    A[\"My <caret>\"]")
        assertTrue("Should not offer keywords inside string", completions.isEmpty())
    }

    fun testNoCompletionInsideSingleQuotedString() {
        val completions = completionsAt("flowchart LR\n    A['My <caret>']")
        assertTrue("Should not offer keywords inside string", completions.isEmpty())
    }

    fun testNoCompletionInsideComment() {
        val completions = completionsAt("flowchart LR\n    %% some <caret>")
        assertTrue("Should not offer keywords inside comment", completions.isEmpty())
    }

    // ── Smart end keyword filtering ────────────────────────────────────

    fun testNoEndOutsideBlock() {
        val completions = completionsAt("flowchart LR\n    A --> B\n    <caret>")
        assertFalse("Should not offer end outside any block", completions.contains("end"))
    }

    fun testEndInsideSubgraph() {
        val completions = completionsAt("flowchart LR\n    subgraph sg\n        <caret>\n    end")
        assertTrue("Expected end inside subgraph", completions.contains("end"))
    }

    fun testNoEndInStateDiagram() {
        val completions = completionsAt("stateDiagram-v2\n    <caret>")
        assertFalse("Should not offer end in stateDiagram (no blocks)", completions.contains("end"))
    }

    fun testEndInsideSequenceLoop() {
        val completions = completionsAt("sequenceDiagram\n    loop every 5s\n        <caret>\n    end")
        assertTrue("Expected end inside loop", completions.contains("end"))
    }

    // ── Direction after direction keyword ──────────────────────────────

    fun testDirectionAfterDirectionKeyword() {
        val completions = completionsAt(
            "flowchart LR\n    subgraph sg\n        direction <caret>\n    end"
        )
        assertTrue("Expected LR after direction keyword", completions.contains("LR"))
        assertTrue("Expected RL after direction keyword", completions.contains("RL"))
        assertTrue("Expected TD after direction keyword", completions.contains("TD"))
        assertTrue("Expected TB after direction keyword", completions.contains("TB"))
        assertTrue("Expected BT after direction keyword", completions.contains("BT"))
    }
}
