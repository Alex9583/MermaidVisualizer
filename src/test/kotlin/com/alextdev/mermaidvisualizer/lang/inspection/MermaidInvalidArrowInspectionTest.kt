package com.alextdev.mermaidvisualizer.lang.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidInvalidArrowInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MermaidInvalidArrowInspection())
    }

    fun testValidFlowchartArrow() {
        myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        myFixture.checkHighlighting()
    }

    fun testValidSequenceArrow() {
        myFixture.configureByText("test.mmd", "sequenceDiagram\n    Alice ->> Bob: Hello")
        myFixture.checkHighlighting()
    }

    fun testValidClassArrow() {
        myFixture.configureByText("test.mmd", "classDiagram\n    Animal <|-- Dog")
        myFixture.checkHighlighting()
    }

    fun testValidStateArrow() {
        myFixture.configureByText("test.mmd", "stateDiagram-v2\n    s1 --> s2")
        myFixture.checkHighlighting()
    }

    fun testValidLongFlowchartArrow() {
        // Variable-length arrows should be valid in flowchart
        myFixture.configureByText("test.mmd", "flowchart LR\n    A ---> B")
        myFixture.checkHighlighting()
    }

    fun testInvalidSequenceArrowInFlowchart() {
        myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    A <warning descr=\"Arrow '->>' is not valid in flowchart diagrams. Valid: -->, --->, ==>, -.->, --x, --o, <-->, ~~~\">->></warning> B"
        )
        myFixture.checkHighlighting()
    }

    fun testInvalidClassArrowInSequence() {
        myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    Alice <warning descr=\"Arrow '<|--' is not valid in sequenceDiagram diagrams. Valid: ->>, -->>, ->, -->, -x, --x, -), --)\"><|--</warning> Bob"
        )
        myFixture.checkHighlighting()
    }

    fun testNoValidationForGantt() {
        // Gantt has no defined arrows — should not warn on any arrow
        myFixture.configureByText("test.mmd", "gantt\n    title A --> B")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowOneToMany() {
        myFixture.configureByText("test.mmd", "erDiagram\n    CUSTOMER ||--o{ ORDER : places")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowOneToManyMandatory() {
        myFixture.configureByText("test.mmd", "erDiagram\n    CUSTOMER ||--|{ ORDER : contains")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowManyToOneMandatory() {
        myFixture.configureByText("test.mmd", "erDiagram\n    ORDER }|--|| CUSTOMER : belongs")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowOneToOne() {
        myFixture.configureByText("test.mmd", "erDiagram\n    PERSON ||--|| PASSPORT : has")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowManyToMany() {
        myFixture.configureByText("test.mmd", "erDiagram\n    STUDENT }o--o{ COURSE : enrolls")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowManyToManyMandatory() {
        myFixture.configureByText("test.mmd", "erDiagram\n    TEACHER }|--|{ CLASS : teaches")
        myFixture.checkHighlighting()
    }

    fun testValidErArrowManyToOne() {
        myFixture.configureByText("test.mmd", "erDiagram\n    ORDER }o--|| CUSTOMER : belongs")
        myFixture.checkHighlighting()
    }

    fun testQuickFixSuggestsReplacement() {
        myFixture.configureByText("test.mmd", "flowchart LR\n    A ->> B")
        myFixture.enableInspections(MermaidInvalidArrowInspection())
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        val fixTexts = fixes.map { it.text }
        assertTrue("Should suggest -->", fixTexts.any { it.contains("-->") })
    }

    fun testQuickFixOrdersArrowsByDistance() {
        // ->> in flowchart: --> (distance 1) should be first suggestion
        myFixture.configureByText("test.mmd", "flowchart LR\n    A ->> B")
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        assertTrue("First fix should be -->", fixes.first().text.contains("-->"))
    }

    fun testQuickFixOrdersLongArrowByDistance() {
        // -->> in flowchart: both --> and ---> are distance 1,
        // but ---> (length 4) matches -->> (length 4) better than --> (length 3)
        myFixture.configureByText("test.mmd", "flowchart LR\n    A -->> B")
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        val fixTexts = fixes.map { it.text }
        val longArrowIdx = fixTexts.indexOfFirst { it.contains("--->") }
        val shortArrowIdx = fixTexts.indexOfFirst { it.contains("-->") && !it.contains("--->") }
        assertTrue("---> should come before --> for -->>",
            longArrowIdx != -1 && (longArrowIdx < shortArrowIdx || shortArrowIdx == -1))
    }

    fun testQuickFixForClassArrowInSequence() {
        // <|-- (class inheritance) in sequence diagram
        myFixture.configureByText("test.mmd", "sequenceDiagram\n    Alice <|-- Bob")
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        val fixTexts = fixes.map { it.text }
        assertTrue("Should suggest sequence arrows",
            fixTexts.any { it.contains("->>") || it.contains("-->") || it.contains("->") })
    }

    fun testValidLongBidirectionalFlowchartArrow() {
        // Variable-length bidirectional arrows (<----->) should be valid in flowchart
        myFixture.configureByText("test.mmd", "flowchart LR\n    A <-----> B")
        myFixture.checkHighlighting()
    }

    fun testValidVeryLongFlowchartArrow() {
        // Very long arrow (------->) should normalize to ---> and be valid
        myFixture.configureByText("test.mmd", "flowchart LR\n    A -------> B")
        myFixture.checkHighlighting()
    }

    fun testQuickFixAppliesArrowReplacement() {
        myFixture.configureByText("test.mmd", "flowchart LR\n    A ->> B")
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        val arrowFix = fixes.first { it.text.contains("-->") }
        myFixture.launchAction(arrowFix)
        val text = myFixture.editor.document.text
        assertTrue("Document should contain '-->' after fix", text.contains("-->"))
        assertFalse("Document should not contain '->>' after fix", text.contains("->>"))
    }
}
