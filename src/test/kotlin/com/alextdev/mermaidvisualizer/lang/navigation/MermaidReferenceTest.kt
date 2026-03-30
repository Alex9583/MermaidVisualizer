package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidReferenceTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private fun findNodeRefAt(text: String, targetName: String, occurrence: Int = 1): MermaidNodeRef? {
        val psi = myFixture.configureByText("test.mmd", text)
        val nodeRefs = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
        var count = 0
        for (ref in nodeRefs) {
            if (ref.name == targetName) {
                count++
                if (count == occurrence) return ref
            }
        }
        return null
    }

    private fun findReferenceAt(text: String, targetName: String, occurrence: Int = 1): PsiReference? {
        val nodeRef = findNodeRefAt(text, targetName, occurrence) ?: return null
        return nodeRef.reference
    }

    // ── Sequence diagram: resolve to participant declaration ─────────────

    fun testResolveToParticipantDeclaration() {
        val ref = findReferenceAt(
            "sequenceDiagram\n    participant Alice\n    Alice ->> Bob: Hello",
            "Alice", 2
        )
        assertNotNull("Usage Alice should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Reference should resolve", resolved)
        assertTrue("Should resolve to MermaidNodeRef", resolved is MermaidNodeRef)
        assertEquals("Alice", (resolved as MermaidNodeRef).name)
    }

    fun testResolveToActorDeclaration() {
        val ref = findReferenceAt(
            "sequenceDiagram\n    actor Bob\n    Alice ->> Bob: Hello",
            "Bob", 2
        )
        assertNotNull("Usage Bob should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Reference should resolve", resolved)
        assertEquals("Bob", (resolved as MermaidNodeRef).name)
    }

    fun testResolveCreateParticipant() {
        val ref = findReferenceAt(
            "sequenceDiagram\n    create participant Charlie\n    Alice ->> Charlie: Hello",
            "Charlie", 2
        )
        assertNotNull("Usage Charlie should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Reference should resolve to create participant declaration", resolved)
        assertEquals("Charlie", (resolved as MermaidNodeRef).name)
    }

    // ── Class diagram: resolve to class declaration ──────────────────────

    fun testResolveToClassDeclaration() {
        val ref = findReferenceAt(
            "classDiagram\n    class Animal\n    class Dog\n    Animal <|-- Dog",
            "Animal", 2
        )
        assertNotNull("Usage Animal should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Reference should resolve", resolved)
        assertEquals("Animal", (resolved as MermaidNodeRef).name)
    }

    // ── Flowchart: resolve to first occurrence ──────────────────────────

    fun testResolveToFirstOccurrenceFlowchart() {
        val ref = findReferenceAt(
            "flowchart LR\n    A --> B\n    B --> C",
            "B", 2
        )
        assertNotNull("Second B should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Reference should resolve", resolved)
        assertEquals("B", (resolved as MermaidNodeRef).name)
    }

    // ── ER diagram: resolve to first occurrence ─────────────────────────

    fun testResolveErEntity() {
        val ref = findReferenceAt(
            "erDiagram\n    CUSTOMER ||--o{ ORDER : places\n    ORDER ||--|{ ITEM : has",
            "ORDER", 2
        )
        assertNotNull("Second ORDER should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Should resolve to first ORDER", resolved)
        assertEquals("ORDER", (resolved as MermaidNodeRef).name)
    }

    // ── State diagram: resolve to first occurrence ──────────────────────

    fun testResolveStateAlias() {
        val ref = findReferenceAt(
            "stateDiagram-v2\n    s1 --> s2\n    s2 --> s1",
            "s1", 2
        )
        assertNotNull(ref)
        val resolved = ref!!.resolve()
        assertNotNull("Should resolve to first s1", resolved)
        assertEquals("s1", (resolved as MermaidNodeRef).name)
    }

    // ── No reference inside brackets (labels) ───────────────────────────

    fun testNoReferenceInsideBrackets() {
        val nodeRef = findNodeRefAt(
            "flowchart LR\n    A[Start] --> B",
            "Start"
        )
        assertNotNull("Start should exist as nodeRef", nodeRef)
        val ref = nodeRef!!.reference
        assertNull("Label inside brackets should not have a reference", ref)
    }

    // ── No reference after colon (message text) ─────────────────────────

    fun testNoReferenceAfterColon() {
        val nodeRef = findNodeRefAt(
            "sequenceDiagram\n    participant Alice\n    Alice ->> Bob: Hello",
            "Hello"
        )
        assertNotNull("Hello should exist as nodeRef", nodeRef)
        val ref = nodeRef!!.reference
        assertNull("Message text after colon should not have a reference", ref)
    }

    // ── Self-resolve on declaration ─────────────────────────────────────

    fun testSelfResolveOnDeclaration() {
        val ref = findReferenceAt(
            "sequenceDiagram\n    participant Alice\n    Alice ->> Bob: Hello",
            "Alice", 1
        )
        assertNotNull("Declaration Alice should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Declaration should resolve to itself", resolved)
        assertEquals("Alice", (resolved as MermaidNodeRef).name)
    }

    // ── Navigation inside nested blocks ─────────────────────────────────

    fun testResolveInNestedSubgraph() {
        val ref = findReferenceAt(
            "flowchart LR\n    A --> B\n    subgraph sub1\n        B --> C\n    end",
            "B", 2
        )
        assertNotNull("B in subgraph should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Should resolve to first B (outside subgraph)", resolved)
        assertEquals("B", (resolved as MermaidNodeRef).name)
    }

    // ── Git graph: branch names ─────────────────────────────────────────

    fun testResolveGitBranch() {
        val ref = findReferenceAt(
            "gitGraph\n    commit\n    branch develop\n    checkout develop",
            "develop", 2
        )
        assertNotNull("Second develop should have a reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Should resolve to first develop", resolved)
        assertEquals("develop", (resolved as MermaidNodeRef).name)
    }
}
