package com.alextdev.mermaidvisualizer.lang

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidFileTypeTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    fun testMmdExtensionIsRegistered() {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName("test.mmd")
        assertSame(MermaidFileType, fileType)
    }

    fun testMermaidExtensionIsRegistered() {
        val fileType = FileTypeManager.getInstance().getFileTypeByFileName("test.mermaid")
        assertSame(MermaidFileType, fileType)
    }

    fun testFileTypeProperties() {
        assertEquals("Mermaid", MermaidFileType.name)
        assertEquals("mmd", MermaidFileType.defaultExtension)
        assertNotNull(MermaidFileType.icon)
    }

    fun testFileTypeLinkedToLanguage() {
        assertSame(MermaidLanguage, MermaidFileType.language)
    }

    fun testOpenMmdFixtureFile() {
        val psiFile = myFixture.configureByFile("simple.mmd")
        assertSame(MermaidFileType, psiFile.virtualFile.fileType)
    }
}
