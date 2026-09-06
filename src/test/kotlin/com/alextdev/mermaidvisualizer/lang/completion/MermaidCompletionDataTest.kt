package com.alextdev.mermaidvisualizer.lang.completion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidCompletionDataTest {

    @Test
    fun testAllDiagramTypesPresent() {
        val expectedKeywords = setOf(
            "flowchart", "graph", "sequenceDiagram", "classDiagram", "erDiagram",
            "stateDiagram-v2", "stateDiagram", "gantt", "pie", "gitGraph",
            "mindmap", "timeline", "journey", "sankey-beta", "xychart-beta",
            "quadrantChart", "requirementDiagram",
            "C4Context", "C4Container", "C4Component", "C4Dynamic", "C4Deployment",
            "zenuml", "kanban", "block-beta", "packet-beta",
            "architecture-beta", "venn-beta", "ishikawa-beta",
            "wardley-beta", "treeView-beta", "treemap-beta",
            "eventmodeling", "radar-beta", "cynefin-beta",
            "railroad-beta", "railroad-ebnf-beta", "railroad-abnf-beta",
            "railroad-peg-beta", "swimlane-beta",
        )
        val actual = MermaidDiagramKind.entries.map { it.keyword }.toSet()
        assertEquals(expectedKeywords, actual)
    }

    @Test
    fun testFlowchartKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.FLOWCHART)
        assertTrue(keywords.containsAll(setOf("subgraph", "style", "classDef", "class", "direction", "end")))
        assertFalse(keywords.contains("participant"))
        assertFalse(keywords.contains("loop"))
    }

    @Test
    fun testSequenceKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.SEQUENCE)
        assertTrue(keywords.containsAll(setOf("participant", "actor", "loop", "alt", "else", "opt", "note")))
        assertFalse(keywords.contains("subgraph"))
        assertFalse(keywords.contains("classDef"))
    }

    @Test
    fun testWardleyKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.WARDLEY)
        assertTrue(keywords.containsAll(setOf("component", "pipeline", "evolve", "evolution", "title", "size")))
    }

    @Test
    fun testSharedKeywordsInAllContexts() {
        for (kind in MermaidDiagramKind.entries) {
            val keywords = MermaidCompletionData.keywordsFor(kind)
            assertTrue(keywords.contains("accTitle"), "accTitle missing for $kind")
            assertTrue(keywords.contains("accDescr"), "accDescr missing for $kind")
        }
    }

    @Test
    fun testArrowSetsNonEmpty() {
        val typesWithArrows = setOf(
            MermaidDiagramKind.FLOWCHART, MermaidDiagramKind.GRAPH,
            MermaidDiagramKind.SEQUENCE, MermaidDiagramKind.CLASS,
            MermaidDiagramKind.ER, MermaidDiagramKind.STATE, MermaidDiagramKind.STATE_V1,
            MermaidDiagramKind.CYNEFIN, MermaidDiagramKind.SWIMLANE,
        )
        for (kind in typesWithArrows) {
            val arrows = MermaidCompletionData.arrowsFor(kind)
            assertTrue(arrows.isNotEmpty(), "Arrows should be non-empty for $kind")
        }
    }

    @Test
    fun testNoOverlapDiagramTypesAndKeywords() {
        val diagramKeywords = MermaidDiagramKind.entries.map { it.keyword }.toSet()
        for (kind in MermaidDiagramKind.entries) {
            val keywords = MermaidCompletionData.keywordsFor(kind)
            val overlap = keywords.intersect(diagramKeywords)
            // "class" is both a keyword and part of "classDiagram" but "class" alone is not a diagram type
            assertTrue(overlap.isEmpty(), "Unexpected overlap for $kind: $overlap")
        }
    }

    @Test
    fun testBlockKeywordsConsistentWithParser() {
        // Mirror of MermaidParserUtil block keyword sets
        assertEquals(setOf("subgraph"), MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.FLOWCHART))
        assertEquals(setOf("subgraph"), MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.GRAPH))
        assertEquals(
            setOf("loop", "alt", "opt", "par", "critical", "break", "rect", "box"),
            MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.SEQUENCE),
        )
        assertTrue(MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.CLASS).isEmpty())
        assertEquals(setOf("subgraph"), MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.SWIMLANE))
        assertEquals(setOf("subgraph"), MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.ER))
        assertTrue(MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.STATE).isEmpty())
        assertTrue(MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.CYNEFIN).isEmpty())
        assertTrue(MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.RAILROAD).isEmpty())
        assertTrue(MermaidCompletionData.blockKeywordsFor(MermaidDiagramKind.WARDLEY).isEmpty())
    }

    @Test
    fun testDirectiveValuesMatchSettings() {
        // Themes should match MermaidTheme enum js values (excluding AUTO which has null)
        val expectedThemes = setOf("default", "dark", "forest", "neutral")
        assertEquals(expectedThemes, MermaidCompletionData.DIRECTIVE_THEME_VALUES.toSet())

        // Looks should match MermaidLook enum js values
        val expectedLooks = setOf("classic", "handDrawn", "neo")
        assertEquals(expectedLooks, MermaidCompletionData.DIRECTIVE_LOOK_VALUES.toSet())
    }

    @Test
    fun testFlowchartDirectionsComplete() {
        val codes = MermaidCompletionData.FLOWCHART_DIRECTIONS.map { it.code }.toSet()
        assertEquals(setOf("LR", "RL", "TD", "TB", "BT"), codes)
    }

    @Test
    fun testGanttKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.GANTT)
        assertTrue(keywords.containsAll(setOf("title", "section", "dateFormat", "axisFormat")))
    }

    @Test
    fun testGitGraphKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.GIT_GRAPH)
        assertTrue(keywords.containsAll(setOf("commit", "branch", "checkout", "merge", "tag")))
    }

    @Test
    fun testGraphAndFlowchartShareKeywords() {
        assertEquals(
            MermaidCompletionData.keywordsFor(MermaidDiagramKind.FLOWCHART),
            MermaidCompletionData.keywordsFor(MermaidDiagramKind.GRAPH),
        )
    }

    @Test
    fun testRadarKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.RADAR)
        assertTrue(keywords.containsAll(setOf(
            "title", "axis", "curve", "showLegend",
            "max", "min", "graticule", "ticks",
        )))
    }

    @Test
    fun testEventModelingKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.EVENT_MODELING)
        assertTrue(keywords.containsAll(setOf(
            "tf", "timeframe", "rf", "resetframe", "data",
            "ui", "pcr", "processor", "cmd", "command",
            "rmo", "readmodel", "evt", "event",
        )))
    }

    @Test
    fun testCynefinKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.CYNEFIN)
        assertTrue(keywords.containsAll(setOf(
            "title", "complex", "complicated", "clear", "chaotic", "confusion",
        )))
        assertFalse(keywords.contains("subgraph"))
    }

    @Test
    fun testSwimlaneKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.SWIMLANE)
        assertTrue(keywords.containsAll(setOf("subgraph", "end")))
        assertFalse(keywords.contains("participant"))
    }

    @Test
    fun testErKeywordsContainSubgraph() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.ER)
        assertTrue(keywords.containsAll(setOf("subgraph", "end")))
        assertFalse(keywords.contains("participant"))
        assertFalse(keywords.contains("classDef"))
    }

    @Test
    fun testArchitectureKeywordsContainExpected() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.ARCHITECTURE)
        assertTrue(keywords.containsAll(setOf(
            "group", "service", "junction", "align", "row", "column",
        )))
    }

    @Test
    fun testRailroadIrKeywordsContainConstructors() {
        val keywords = MermaidCompletionData.keywordsFor(MermaidDiagramKind.RAILROAD)
        assertTrue(keywords.containsAll(setOf(
            "title", "terminal", "nonterminal", "sequence", "choice",
            "optional", "zeroOrMore", "oneOrMore", "special",
        )))
    }

    @Test
    fun testRailroadNotationKeywordsAreTitleOnly() {
        for (kind in setOf(
            MermaidDiagramKind.RAILROAD_EBNF,
            MermaidDiagramKind.RAILROAD_ABNF,
            MermaidDiagramKind.RAILROAD_PEG,
        )) {
            val keywords = MermaidCompletionData.keywordsFor(kind)
            assertTrue(keywords.contains("title"), "title missing for $kind")
            assertFalse(keywords.contains("terminal"),
                "IR constructors should not leak into notation kind $kind")
        }
    }

    @Test
    fun testDividerKeywords() {
        assertEquals(setOf("else", "and"), MermaidCompletionData.dividerKeywordsFor(MermaidDiagramKind.SEQUENCE))
        assertTrue(MermaidCompletionData.dividerKeywordsFor(MermaidDiagramKind.FLOWCHART).isEmpty())
        assertTrue(MermaidCompletionData.dividerKeywordsFor(MermaidDiagramKind.GANTT).isEmpty())
    }
}
