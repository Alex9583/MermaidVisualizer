package com.alextdev.mermaidvisualizer.lang.inspection

import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidUnknownDiagramTypeInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MermaidUnknownDiagramTypeInspection())
    }

    fun testValidFlowchart() {
        myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        myFixture.checkHighlighting()
    }

    fun testValidGraph() {
        myFixture.configureByText("test.mmd", "graph TD\n    A --> B")
        myFixture.checkHighlighting()
    }

    fun testValidSequenceDiagram() {
        myFixture.configureByText("test.mmd", "sequenceDiagram\n    Alice ->> Bob: Hello")
        myFixture.checkHighlighting()
    }

    fun testValidClassDiagram() {
        myFixture.configureByText("test.mmd", "classDiagram\n    Animal <|-- Dog")
        myFixture.checkHighlighting()
    }

    fun testValidGantt() {
        myFixture.configureByText("test.mmd", "gantt\n    title A Gantt Diagram")
        myFixture.checkHighlighting()
    }

    fun testValidPie() {
        myFixture.configureByText("test.mmd", "pie\n    title Pets")
        myFixture.checkHighlighting()
    }

    fun testValidMindmap() {
        myFixture.configureByText("test.mmd", "mindmap\n    root")
        myFixture.checkHighlighting()
    }

    fun testEmptyFile() {
        myFixture.configureByText("test.mmd", "")
        myFixture.checkHighlighting()
    }

    // Test the Levenshtein distance helper directly (no PSI needed)

    fun testSuggestClosestForFlowchar() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("flowchar")
        assertTrue("Should suggest flowchart", suggestions.contains("flowchart"))
        assertEquals("First suggestion should be flowchart", "flowchart", suggestions.first())
    }

    fun testSuggestClosestForSequence() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("seqDiagram")
        assertTrue("Should suggest sequenceDiagram", suggestions.any { it == "sequenceDiagram" })
    }

    fun testSuggestClosestLimitedTo3() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("xyz", 3)
        assertTrue("Should return at most 3", suggestions.size <= 3)
    }

    fun testSuggestClosestForGrap() {
        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest("grap")
        assertEquals("First suggestion should be graph", "graph", suggestions.first())
    }
}
