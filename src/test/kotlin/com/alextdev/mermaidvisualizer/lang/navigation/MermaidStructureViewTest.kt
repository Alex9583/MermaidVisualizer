package com.alextdev.mermaidvisualizer.lang.navigation

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidStructureViewTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private fun getStructureRoot(text: String): StructureViewTreeElement {
        val psi = myFixture.configureByText("test.mmd", text)
        val factory = MermaidStructureViewFactory()
        val builder = factory.getStructureViewBuilder(psi)
        assertNotNull("Structure view builder should be created for MermaidFile", builder)
        val model = (builder as com.intellij.ide.structureView.TreeBasedStructureViewBuilder)
            .createStructureViewModel(null)
        return model.root
    }

    private fun collectPresentableTexts(element: StructureViewTreeElement): List<String> {
        val result = mutableListOf<String>()
        val text = element.presentation.presentableText
        if (text != null) result.add(text)
        for (child in element.children) {
            result.addAll(collectPresentableTexts(child as StructureViewTreeElement))
        }
        return result
    }

    // ── Flowchart ───────────────────────────────────────────────────────

    fun testFlowchartNodes() {
        val root = getStructureRoot("flowchart LR\n    A --> B\n    B --> C")
        val texts = collectPresentableTexts(root)
        // Single-diagram optimization: nodes are direct children
        assertTrue("Should contain A", texts.contains("A"))
        assertTrue("Should contain B", texts.contains("B"))
        assertTrue("Should contain C", texts.contains("C"))
    }

    fun testFlowchartWithSubgraph() {
        val root = getStructureRoot(
            "flowchart LR\n    subgraph sub1\n        A --> B\n    end\n    C --> D"
        )
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain subgraph", texts.any { it.startsWith("subgraph") })
        assertTrue("Should contain A", texts.contains("A"))
        assertTrue("Should contain C", texts.contains("C"))
    }

    fun testNestedSubgraphs() {
        val root = getStructureRoot(
            "flowchart LR\n" +
            "    subgraph outer\n" +
            "        subgraph inner\n" +
            "            A --> B\n" +
            "        end\n" +
            "    end"
        )
        val texts = collectPresentableTexts(root)
        val subgraphCount = texts.count { it.startsWith("subgraph") }
        assertEquals("Should have 2 subgraphs", 2, subgraphCount)
    }

    // ── Sequence diagram ────────────────────────────────────────────────

    fun testSequenceParticipants() {
        val root = getStructureRoot(
            "sequenceDiagram\n    participant Alice\n    actor Bob\n    Alice ->> Bob: Hello"
        )
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain Alice", texts.contains("Alice"))
        assertTrue("Should contain Bob", texts.contains("Bob"))
    }

    fun testSequenceWithBlocks() {
        val root = getStructureRoot(
            "sequenceDiagram\n    participant Alice\n    loop Every minute\n        Alice ->> Bob: ping\n    end"
        )
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain Alice", texts.contains("Alice"))
        assertTrue("Should contain loop block", texts.any { it.startsWith("loop") })
    }

    // ── Class diagram ───────────────────────────────────────────────────

    fun testClassDiagram() {
        val root = getStructureRoot(
            "classDiagram\n    class Animal\n    class Dog\n    Animal <|-- Dog"
        )
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain Animal", texts.contains("Animal"))
        assertTrue("Should contain Dog", texts.contains("Dog"))
    }

    // ── ER diagram ──────────────────────────────────────────────────────

    fun testErDiagram() {
        val root = getStructureRoot(
            "erDiagram\n    CUSTOMER ||--o{ ORDER : places"
        )
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain CUSTOMER", texts.contains("CUSTOMER"))
        assertTrue("Should contain ORDER", texts.contains("ORDER"))
        // "places" is after colon — should NOT appear
        assertFalse("Should not contain places (after colon)", texts.contains("places"))
    }

    // ── Single-diagram optimization ─────────────────────────────────────

    fun testSingleDiagramSkipsDiagramNode() {
        val root = getStructureRoot("flowchart LR\n    A --> B")
        val children = root.children
        // With single diagram, nodes should be direct children (no diagram wrapper)
        assertTrue(
            "Direct children should contain node refs, not diagram wrapper",
            children.any { (it as StructureViewTreeElement).presentation.presentableText == "A" }
        )
    }

    // ── Labels excluded from structure ──────────────────────────────────

    fun testLabelsExcludedFromStructure() {
        val root = getStructureRoot("flowchart LR\n    A[Start] --> B[End]")
        val texts = collectPresentableTexts(root)
        assertTrue("Should contain A", texts.contains("A"))
        assertTrue("Should contain B", texts.contains("B"))
        assertFalse("Should not contain Start (label)", texts.contains("Start"))
        assertFalse("Should not contain End (label)", texts.contains("End"))
    }

    // ── Non-MermaidFile returns null ─────────────────────────────────────

    fun testNonMermaidFileReturnsNull() {
        val psi = myFixture.configureByText("test.txt", "hello world")
        val factory = MermaidStructureViewFactory()
        assertNull("Should return null for non-Mermaid files", factory.getStructureViewBuilder(psi))
    }
}
