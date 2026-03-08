package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidPreviewFileEditorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    private fun withEditor(block: (MermaidPreviewFileEditor) -> Unit) {
        val psiFile = myFixture.configureByFile("simple.mmd")
        val editor = MermaidPreviewFileEditor(project, psiFile.virtualFile)
        Disposer.register(testRootDisposable, editor)
        block(editor)
    }

    fun testEditorName() = withEditor { editor ->
        assertEquals("Mermaid Preview", editor.name)
    }

    fun testIsNotModified() = withEditor { editor ->
        assertFalse(editor.isModified)
    }

    fun testIsValidForValidFile() = withEditor { editor ->
        assertTrue(editor.isValid)
    }

    fun testComponentIsNotNull() = withEditor { editor ->
        assertNotNull(editor.component)
    }

    fun testGetFileReturnsOriginalFile() = withEditor { editor ->
        val psiFile = myFixture.configureByFile("simple.mmd")
        assertEquals(psiFile.virtualFile, editor.file)
    }
}
