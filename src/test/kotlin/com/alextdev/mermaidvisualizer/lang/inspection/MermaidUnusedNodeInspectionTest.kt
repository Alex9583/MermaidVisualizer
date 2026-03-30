package com.alextdev.mermaidvisualizer.lang.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidUnusedNodeInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MermaidUnusedNodeInspection())
    }

    fun testSequenceAllUsed() {
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Bob: Hello"
        )
        myFixture.checkHighlighting()
    }

    fun testSequenceUnusedParticipant() {
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant <warning descr=\"Node 'Bob' declared as 'participant' but never used\">Bob</warning>\n    Alice ->> Alice: Self message"
        )
        myFixture.checkHighlighting()
    }

    fun testSequenceNoDeclarations() {
        // When no explicit declarations exist, nothing should be flagged
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    Alice ->> Bob: Hello"
        )
        myFixture.checkHighlighting()
    }

    fun testClassUnusedDeclaration() {
        myFixture.configureByText(
            "test.mmd",
            "classDiagram\n    class Animal\n    class <warning descr=\"Node 'Dog' declared as 'class' but never used\">Dog</warning>\n    Animal <|-- Cat"
        )
        myFixture.checkHighlighting()
    }

    fun testClassAllUsed() {
        myFixture.configureByText(
            "test.mmd",
            "classDiagram\n    class Animal\n    class Dog\n    Animal <|-- Dog"
        )
        myFixture.checkHighlighting()
    }

    fun testFlowchartNoFalsePositives() {
        // Flowchart with brackets — no warnings should be produced
        myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    A[Start] --> B[End]"
        )
        myFixture.checkHighlighting()
    }

    fun testSequenceQuickFixRemoves() {
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Alice: Self"
        )
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have a remove fix", fixes.any { it.text.contains("Remove") })
    }

    fun testSequenceQuickFixAppliesCorrectly() {
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Alice: Self"
        )
        val fixes = myFixture.getAllQuickFixes()
        val removeFix = fixes.first { it.text.contains("Remove") }
        myFixture.launchAction(removeFix)
        val text = myFixture.editor.document.text
        assertFalse("Bob declaration should be removed", text.contains("participant Bob"))
        assertTrue("Alice declaration should remain", text.contains("participant Alice"))
        assertTrue("Usage should remain", text.contains("Alice ->> Alice: Self"))
    }

    fun testClassQuickFixAppliesCorrectly() {
        myFixture.configureByText(
            "test.mmd",
            "classDiagram\n    class Animal\n    class Dog\n    Animal <|-- Cat"
        )
        val fixes = myFixture.getAllQuickFixes()
        val removeFix = fixes.first { it.text.contains("Remove") }
        myFixture.launchAction(removeFix)
        val text = myFixture.editor.document.text
        assertFalse("Dog declaration should be removed", text.contains("class Dog"))
        assertTrue("Animal declaration should remain", text.contains("class Animal"))
    }
}
