package com.alextdev.mermaidvisualizer.lang.psi

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.IncorrectOperationException

class MermaidPsiElementFactoryTest : BasePlatformTestCase() {

    fun testCreateNodeRefReturnsCorrectName() {
        val nodeRef = MermaidPsiElementFactory.createNodeRef(project, "Alice")
        assertNotNull("Should create a MermaidNodeRef", nodeRef)
        assertEquals("Alice", nodeRef.name)
    }

    fun testCreateNodeRefWithDifferentName() {
        val nodeRef = MermaidPsiElementFactory.createNodeRef(project, "MyNode")
        assertEquals("MyNode", nodeRef.name)
    }

    fun testCreateNodeRefBlankNameThrows() {
        try {
            MermaidPsiElementFactory.createNodeRef(project, "")
            fail("Should throw IllegalArgumentException for blank name")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    fun testCreateNodeRefWhitespaceNameThrows() {
        try {
            MermaidPsiElementFactory.createNodeRef(project, "A B")
            fail("Should throw IllegalArgumentException for name with whitespace")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }

    fun testSetNameUpdatesElement() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    Alice --> Bob")
        val nodeRefs = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
        val alice = nodeRefs.first { it.name == "Alice" }
        myFixture.openFileInEditor(psi.virtualFile)
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
            alice.setName("Charlie")
        }
        val text = myFixture.editor.document.text
        assertTrue("Document should contain 'Charlie' after setName", text.contains("Charlie"))
        assertFalse("Document should not contain 'Alice' after setName", text.contains("Alice"))
    }

    fun testSetNameWithInvalidNameThrowsIncorrectOperation() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    Alice --> Bob")
        val nodeRefs = com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
        val alice = nodeRefs.first { it.name == "Alice" }
        try {
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) {
                alice.setName("A B")
            }
            fail("Should throw IncorrectOperationException for invalid name")
        } catch (_: IncorrectOperationException) {
            // expected
        }
    }

    fun testRenameViaFixture() {
        myFixture.configureByText(
            "test.mmd",
            "flowchart LR\n    Alice --> Bob\n    Bob --> <caret>Alice"
        )
        myFixture.renameElementAtCaret("Charlie")
        val text = myFixture.editor.document.text
        // Both occurrences of Alice should be renamed to Charlie
        assertFalse("No Alice should remain", text.contains("Alice"))
        val charlieCount = Regex("Charlie").findAll(text).count()
        assertEquals("Charlie should appear twice", 2, charlieCount)
    }
}
