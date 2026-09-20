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

    // ── New diagram types (Mermaid 11.16.0) ────────────────────────────

    fun testDiagramTypeCompletionIncludesNewTypes() {
        val completions = completionsAt("<caret>")
        assertTrue("Expected cynefin-beta", completions.contains("cynefin-beta"))
        assertTrue("Expected railroad-beta", completions.contains("railroad-beta"))
        assertTrue("Expected railroad-ebnf-beta", completions.contains("railroad-ebnf-beta"))
        assertTrue("Expected swimlane-beta", completions.contains("swimlane-beta"))
    }

    fun testCynefinKeywords() {
        val completions = completionsAt("cynefin-beta\n    <caret>")
        assertTrue("Expected complex", completions.contains("complex"))
        assertTrue("Expected complicated", completions.contains("complicated"))
        assertTrue("Expected clear", completions.contains("clear"))
        assertTrue("Expected chaotic", completions.contains("chaotic"))
        assertTrue("Expected confusion", completions.contains("confusion"))
        assertFalse("Should not offer subgraph", completions.contains("subgraph"))
    }

    fun testSwimlaneKeywords() {
        val completions = completionsAt("swimlane-beta LR\n    <caret>")
        assertTrue("Expected subgraph", completions.contains("subgraph"))
        assertFalse("Should not offer participant", completions.contains("participant"))
    }

    fun testRailroadIrKeywords() {
        val completions = completionsAt("railroad-beta\n    <caret>")
        assertTrue("Expected terminal", completions.contains("terminal"))
        assertTrue("Expected choice", completions.contains("choice"))
        assertTrue("Expected zeroOrMore", completions.contains("zeroOrMore"))
        assertFalse("Should not offer subgraph", completions.contains("subgraph"))
    }

    fun testArchitectureAlignKeyword() {
        val completions = completionsAt("architecture-beta\n    <caret>")
        assertTrue("Expected align", completions.contains("align"))
        assertTrue("Expected group", completions.contains("group"))
        assertTrue("Expected service", completions.contains("service"))
    }

    // ── ER subgraphs (Mermaid 11.17.0) ─────────────────────────────────

    fun testErSubgraphKeyword() {
        val completions = completionsAt("erDiagram\n    <caret>")
        assertTrue("Expected subgraph in erDiagram", completions.contains("subgraph"))
        assertFalse("Should not offer end outside a block", completions.contains("end"))
        assertFalse("Should not offer classDef", completions.contains("classDef"))
        assertFalse("Should not offer participant", completions.contains("participant"))
    }

    fun testEndInsideErSubgraph() {
        val completions = completionsAt("erDiagram\n    subgraph title1\n        <caret>\n    end")
        assertTrue("Expected end inside ER subgraph", completions.contains("end"))
        assertTrue("Expected nested subgraph", completions.contains("subgraph"))
    }

    // ── flowchart-elk (flowchart PSI, ELK layout) ──────────────────────

    fun testFlowchartElkTypeDirectionAndKeywords() {
        assertTrue("Expected flowchart-elk", completionsAt("<caret>").contains("flowchart-elk"))
        val directions = completionsAt("flowchart-elk <caret>")
        assertTrue("Expected LR after flowchart-elk", directions.contains("LR"))
        assertTrue("Expected TD after flowchart-elk", directions.contains("TD"))
        val keywords = completionsAt("flowchart-elk TD\n    <caret>")
        assertTrue("Expected subgraph", keywords.contains("subgraph"))
        assertTrue("Expected classDef", keywords.contains("classDef"))
        assertFalse("Should not offer participant", keywords.contains("participant"))
    }

    // ── Mermaid 12: use case + agentflow ────────────────────────────────

    fun testDiagramTypeCompletionIncludesMermaid12Types() {
        val completions = completionsAt("<caret>")
        assertTrue("Expected usecase-beta", completions.contains("usecase-beta"))
        assertTrue("Expected agentflow-beta", completions.contains("agentflow-beta"))
    }

    fun testUsecaseKeywords() {
        val completions = completionsAt("usecase-beta\n    <caret>")
        assertTrue("Expected actor", completions.contains("actor"))
        assertTrue("Expected systemBoundary", completions.contains("systemBoundary"))
        assertTrue("Expected note", completions.contains("note"))
        assertTrue("Expected json", completions.contains("json"))
        assertFalse("Should not offer end outside a block", completions.contains("end"))
        assertFalse("Should not offer subgraph", completions.contains("subgraph"))
        assertFalse("Should not offer participant", completions.contains("participant"))
    }

    fun testEndInsideSystemBoundary() {
        val completions = completionsAt("usecase-beta\n    systemBoundary Orders\n        <caret>\n    end")
        assertTrue("Expected end inside systemBoundary", completions.contains("end"))
    }

    fun testAgentflowKeywords() {
        val completions = completionsAt("agentflow-beta LR\n    <caret>")
        assertTrue("Expected flow", completions.contains("flow"))
        assertTrue("Expected global", completions.contains("global"))
        assertTrue("Expected connector", completions.contains("connector"))
        assertFalse("Should not offer subgraph", completions.contains("subgraph"))
        assertFalse("Should not offer actor", completions.contains("actor"))
    }

    fun testAgentflowDirectionOnHeaderLine() {
        val completions = completionsAt("agentflow-beta <caret>")
        assertTrue("Expected LR", completions.contains("LR"))
        assertTrue("Expected TB", completions.contains("TB"))
    }

    fun testUsecaseDirectionAfterKeyword() {
        val completions = completionsAt("usecase-beta\n    direction <caret>")
        assertTrue("Expected LR", completions.contains("LR"))
    }

    fun testUsecaseNoDirectionOnHeaderLine() {
        val completions = completionsAt("usecase-beta <caret>")
        assertFalse("usecase-beta takes no header direction", completions.contains("LR"))
    }

    fun testUsecaseArrowsAfterIdentifier() {
        val completions = completionsAt("usecase-beta\n    actor A\n    A <caret>")
        assertTrue("Expected -->", completions.contains("-->"))
        assertTrue("Expected --|>", completions.contains("--|>"))
        assertTrue("Expected ..>", completions.contains("..>"))
        assertFalse("Should not offer ->>", completions.contains("->>"))
    }

    // ── Directive content completion (%%{init: {...}}%%) ───────────────

    fun testDirectiveThemeValues() {
        val completions = completionsAt("%%{init: {'theme': '<caret>'}}%%")
        assertTrue("Expected dark", completions.contains("dark"))
        assertTrue("Expected redux-color", completions.contains("redux-color"))
        assertFalse("Should not offer config keys as a theme value", completions.contains("look"))
        assertFalse("Should not offer diagram types inside a directive", completions.contains("flowchart"))
    }

    fun testDirectiveLayoutValues() {
        val completions = completionsAt("%%{init: {'layout': '<caret>'}}%%")
        assertTrue("Expected dagre", completions.contains("dagre"))
        assertTrue("Expected elk", completions.contains("elk"))
        assertTrue("Expected elk.box", completions.contains("elk.box"))
        assertFalse("Should not offer theme values for layout", completions.contains("dark"))
    }

    fun testDirectiveConfigKeys() {
        val completions = completionsAt("%%{init: {<caret>}}%%")
        assertTrue("Expected theme key", completions.contains("theme"))
        assertTrue("Expected look key", completions.contains("look"))
        assertTrue("Expected layout key", completions.contains("layout"))
        assertFalse("Should not offer values without a key", completions.contains("dark"))
    }

    fun testDirectiveValuesForSecondKey() {
        val completions = completionsAt("%%{init: {'theme': 'dark', 'look': '<caret>'}}%%")
        assertTrue("Expected handDrawn", completions.contains("handDrawn"))
        assertTrue("Expected neo", completions.contains("neo"))
        assertFalse("Should not offer theme values for look", completions.contains("forest"))
    }

    fun testDirectiveValuesAfterClosedNestedObject() {
        // The text before the caret spans several DIRECTIVE tokens (chunks split on `}`)
        val completions = completionsAt("%%{init: {'themeVariables': {'lineColor': '#f00'}, 'layout': '<caret>'}}%%")
        assertTrue("Expected dagre after a nested object", completions.contains("dagre"))
    }

    fun testDirectiveInsideDiagramBody() {
        val completions = completionsAt("flowchart LR\n    A --> B\n    %%{init: {'theme': '<caret>'}}%%")
        assertTrue("Expected dark inside a body directive", completions.contains("dark"))
        assertFalse("Should not offer flowchart keywords inside a directive", completions.contains("subgraph"))
    }

    fun testDirectivePrefixFiltersValues() {
        myFixture.configureByText("test.mmd", "%%{init: {'theme': 'redux-d<caret>'}}%%")
        val items = myFixture.completeBasic()?.map { it.lookupString }
        assertNotNull("Two themes match the prefix redux-d", items)
        assertEquals(setOf("redux-dark", "redux-dark-color"), items!!.toSet())
    }

    fun testNonInitDirectiveOffersNothing() {
        val completions = completionsAt("%%{wrap<caret>}%%")
        assertFalse("wrap directive has no config keys", completions.contains("theme"))
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

    // ── Direction insertion correctness ────────────────────────────────

    fun testDirectionInsertionWithTypedPrefix() {
        myFixture.configureByText("test.mmd", "flowchart L<caret>")
        val items = myFixture.completeBasic()
        assertNotNull("Expected multiple matches for 'L' prefix", items)
        val lrItem = items.first { it.lookupString == "LR" }
        myFixture.lookup.currentItem = lrItem
        myFixture.type('\n')
        assertEquals("flowchart LR", myFixture.editor.document.text.trimEnd())
    }

    fun testDirectionInsertionFromPopupWithPrefix() {
        myFixture.configureByText("test.mmd", "flowchart T<caret>")
        val items = myFixture.completeBasic()
        assertNotNull("Expected multiple matches for 'T' prefix", items)
        val tdItem = items.first { it.lookupString == "TD" }
        myFixture.lookup.currentItem = tdItem
        myFixture.type('\n')
        assertEquals("flowchart TD", myFixture.editor.document.text.trimEnd())
    }

    // ── Glued arrows and `@{ ... }` metadata (lexer text model) ─────────

    fun testNodeNameAfterGluedArrow() {
        val completions = completionsAt(
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice->><caret>"
        )
        assertTrue("Expected Bob", completions.contains("Bob"))
        assertTrue("Expected Alice", completions.contains("Alice"))
    }

    fun testNodeNameBeforeShapeMetadataNotPolluted() {
        val completions = completionsAt("flowchart LR\n    A --> B@{ shape: person }\n    <caret>")
        assertTrue("Expected B", completions.contains("B"))
        assertFalse("Should not offer B@", completions.contains("B@"))
        assertFalse("Should not offer @", completions.contains("@"))
    }
}
