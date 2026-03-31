package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidPsiUtilTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    // ── findDiagramBody ─────────────────────────────────────────────────

    fun testFindDiagramBodyFromStatement() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        val stmt = PsiTreeUtil.findChildOfType(psi, MermaidStatement::class.java)!!
        val body = MermaidPsiUtil.findDiagramBody(stmt)
        assertNotNull("Should find diagram body", body)
    }

    fun testFindDiagramBodyFromSequence() {
        val psi = myFixture.configureByText("test.mmd", "sequenceDiagram\n    Alice ->> Bob: Hello")
        val stmt = PsiTreeUtil.findChildOfType(psi, MermaidStatement::class.java)!!
        val body = MermaidPsiUtil.findDiagramBody(stmt)
        assertNotNull("Should find diagram body", body)
    }

    // ── collectAllIdentifiers ───────────────────────────────────────────

    fun testCollectAllIdentifiers() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B\n    B --> C")
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val ids = MermaidPsiUtil.collectAllIdentifiers(body)
        assertEquals(setOf("A", "B", "C"), ids)
    }

    fun testCollectAllIdentifiersInNestedBlocks() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    subgraph sub1\n        X --> Y\n    end\n    A --> B"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val ids = MermaidPsiUtil.collectAllIdentifiers(body)
        assertTrue("Should find X", ids.contains("X"))
        assertTrue("Should find Y", ids.contains("Y"))
        assertTrue("Should find A", ids.contains("A"))
        assertTrue("Should find B", ids.contains("B"))
    }

    // ── collectDeclaredNodes ────────────────────────────────────────────

    fun testCollectDeclaredParticipantsSequence() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    actor Bob\n    Alice ->> Bob: Hello"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val declared = MermaidPsiUtil.collectDeclaredNodes(body, MermaidDiagramKind.SEQUENCE)
        val names = declared.map { it.name }.toSet()
        assertEquals(setOf("Alice", "Bob"), names)
        assertTrue("Alice keyword is participant", declared.any { it.name == "Alice" && it.keyword == "participant" })
        assertTrue("Bob keyword is actor", declared.any { it.name == "Bob" && it.keyword == "actor" })
    }

    fun testCollectDeclaredNodesEmptyForFlowchart() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val declared = MermaidPsiUtil.collectDeclaredNodes(body, MermaidDiagramKind.FLOWCHART)
        assertTrue("Flowchart should have no explicit declarations", declared.isEmpty())
    }

    fun testCollectDeclaredClassesInClassDiagram() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "classDiagram\n    class Animal\n    class Dog\n    Animal <|-- Dog"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val declared = MermaidPsiUtil.collectDeclaredNodes(body, MermaidDiagramKind.CLASS)
        val names = declared.map { it.name }.toSet()
        assertEquals(setOf("Animal", "Dog"), names)
    }

    // ── collectUsedIdentifiers ──────────────────────────────────────────

    fun testCollectUsedIdentifiersSequence() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Bob: Hello"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val used = MermaidPsiUtil.collectUsedIdentifiers(body, MermaidDiagramKind.SEQUENCE)
        val names = used.map { it.name }.toSet()
        assertTrue("Should find Alice", names.contains("Alice"))
        assertTrue("Should find Bob", names.contains("Bob"))
        assertFalse("Should NOT find Hello (after colon)", names.contains("Hello"))
    }

    fun testCollectUsedIdentifiersSequenceSkipsLabels() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Bob: Hello world"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val used = MermaidPsiUtil.collectUsedIdentifiers(body, MermaidDiagramKind.SEQUENCE)
        val names = used.map { it.name }.toSet()
        assertFalse("Should NOT find Hello", names.contains("Hello"))
        assertFalse("Should NOT find world", names.contains("world"))
    }

    fun testCollectUsedIdentifiersClassRelationship() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "classDiagram\n    class Animal\n    Animal <|-- Dog"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val used = MermaidPsiUtil.collectUsedIdentifiers(body, MermaidDiagramKind.CLASS)
        val names = used.map { it.name }.toSet()
        assertTrue("Should find Animal", names.contains("Animal"))
        assertTrue("Should find Dog", names.contains("Dog"))
    }

    // ── isAfterColon newline reset ─────────────────────────────────────

    fun testIsAfterColonResetsOnNewline() {
        // After "Alice ->> Bob: Hello\n", the newline should reset the colon tracking.
        // Charlie and Dave on the next line should be collected as used identifiers.
        val psi = myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    participant Charlie\n    participant Dave\n    Alice ->> Bob: Hello\n    Charlie ->> Dave: World"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val used = MermaidPsiUtil.collectUsedIdentifiers(body, MermaidDiagramKind.SEQUENCE)
        val names = used.map { it.name }.toSet()
        assertTrue("Should find Charlie (newline resets colon)", names.contains("Charlie"))
        assertTrue("Should find Dave (newline resets colon)", names.contains("Dave"))
        assertFalse("Should NOT find Hello (after colon)", names.contains("Hello"))
        assertFalse("Should NOT find World (after colon)", names.contains("World"))
    }

    fun testIsAfterColonSameLineNotReset() {
        // On the same line after a colon, identifiers should be filtered out
        val psi = myFixture.configureByText(
            "test.mmd",
            "sequenceDiagram\n    participant Alice\n    participant Bob\n    Alice ->> Bob: Greeting"
        )
        val body = PsiTreeUtil.findChildOfType(psi, MermaidDiagramBody::class.java)!!
        val used = MermaidPsiUtil.collectUsedIdentifiers(body, MermaidDiagramKind.SEQUENCE)
        val names = used.map { it.name }.toSet()
        assertFalse("Should NOT find Greeting (after colon, same line)", names.contains("Greeting"))
    }

    // ── isInsideBrackets ─────────────────────────────────────────────────

    fun testIsInsideBracketsFlowchart() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    A[Start] --> B[End]"
        )
        val nodeRefs = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
        val start = nodeRefs.first { it.name == "Start" }
        val a = nodeRefs.first { it.name == "A" }
        assertTrue("Start should be inside brackets", MermaidPsiUtil.isInsideBrackets(start))
        assertFalse("A should not be inside brackets", MermaidPsiUtil.isInsideBrackets(a))
    }

    // ── isNodeName ───────────────────────────────────────────────────────

    fun testIsNodeNameFiltersLabelsAndMessages() {
        val psi = myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    A[Label] --> B"
        )
        val nodeRefs = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
        val label = nodeRefs.first { it.name == "Label" }
        val a = nodeRefs.first { it.name == "A" }
        assertFalse("Label inside brackets is not a node name", MermaidPsiUtil.isNodeName(label))
        assertTrue("A is a node name", MermaidPsiUtil.isNodeName(a))
    }
}
