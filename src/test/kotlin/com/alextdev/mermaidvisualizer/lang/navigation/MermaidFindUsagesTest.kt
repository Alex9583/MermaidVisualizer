package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidFindUsagesTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    fun testFindUsagesProviderCanFindNodeRef() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        val nodeRef = PsiTreeUtil.findChildOfType(psi, MermaidNodeRef::class.java)!!
        val provider = MermaidFindUsagesProvider()
        assertTrue(provider.canFindUsagesFor(nodeRef))
    }

    fun testFindUsagesProviderType() {
        val psi = myFixture.configureByText("test.mmd", "sequenceDiagram\n    participant Alice")
        val nodeRef = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
            .first { it.name == "Alice" }
        val provider = MermaidFindUsagesProvider()
        assertEquals("participant", provider.getType(nodeRef))
    }

    fun testFindUsagesProviderTypeFlowchart() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        val nodeRef = PsiTreeUtil.findChildOfType(psi, MermaidNodeRef::class.java)!!
        val provider = MermaidFindUsagesProvider()
        assertEquals("node", provider.getType(nodeRef))
    }

    fun testFindUsagesProviderTypeClass() {
        val psi = myFixture.configureByText("test.mmd", "classDiagram\n    class Animal")
        val nodeRef = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
            .first { it.name == "Animal" }
        val provider = MermaidFindUsagesProvider()
        assertEquals("class", provider.getType(nodeRef))
    }

    fun testFindUsagesProviderTypeEr() {
        val psi = myFixture.configureByText("test.mmd", "erDiagram\n    CUSTOMER ||--o{ ORDER : places")
        val nodeRef = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
            .first { it.name == "CUSTOMER" }
        val provider = MermaidFindUsagesProvider()
        assertEquals("entity", provider.getType(nodeRef))
    }

    fun testFindUsagesProviderDescriptiveName() {
        val psi = myFixture.configureByText("test.mmd", "flowchart LR\n    Alice --> Bob")
        val nodeRef = PsiTreeUtil.findChildrenOfType(psi, MermaidNodeRef::class.java)
            .first { it.name == "Alice" }
        val provider = MermaidFindUsagesProvider()
        assertEquals("Alice", provider.getDescriptiveName(nodeRef))
    }

    fun testWordsScannerRecognizesIdentifiers() {
        val provider = MermaidFindUsagesProvider()
        val scanner = provider.wordsScanner
        assertNotNull("WordsScanner should not be null", scanner)
        assertTrue(scanner is DefaultWordsScanner)
    }
}
