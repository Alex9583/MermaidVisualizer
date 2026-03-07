package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidEditorProviderTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private val provider = MermaidEditorProvider()

    fun testAcceptsMmdFile() {
        val file = myFixture.configureByFile("simple.mmd")
        assertTrue(provider.accept(project, file.virtualFile))
    }

    fun testAcceptsMermaidFile() {
        val file = myFixture.configureByText("test.mermaid", "flowchart LR\n    A --> B")
        assertTrue(provider.accept(project, file.virtualFile))
    }

    fun testRejectsTxtFile() {
        val file = myFixture.configureByText("test.txt", "hello")
        assertFalse(provider.accept(project, file.virtualFile))
    }

    fun testRejectsMdFile() {
        val file = myFixture.configureByText("test.md", "# Hello")
        assertFalse(provider.accept(project, file.virtualFile))
    }

    fun testEditorTypeId() {
        assertEquals("mermaid-split-editor", provider.editorTypeId)
    }

    fun testPolicyIsHideDefaultEditor() {
        assertEquals(FileEditorPolicy.HIDE_DEFAULT_EDITOR, provider.policy)
    }

    fun testCreateEditorReturnsTextEditorWithPreview() {
        val psiFile = myFixture.configureByFile("simple.mmd")
        val editor = provider.createEditor(project, psiFile.virtualFile)
        try {
            assertInstanceOf(editor, TextEditorWithPreview::class.java)
        } finally {
            editor.dispose()
        }
    }
}
