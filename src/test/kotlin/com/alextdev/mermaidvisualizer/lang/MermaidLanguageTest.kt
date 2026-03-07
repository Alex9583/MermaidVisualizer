package com.alextdev.mermaidvisualizer.lang

import com.intellij.lang.Language
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MermaidLanguageTest {

    @Test
    fun `language ID is Mermaid`() {
        assertEquals("Mermaid", MermaidLanguage.id)
    }

    @Test
    fun `display name is Mermaid`() {
        assertEquals("Mermaid", MermaidLanguage.displayName)
    }

    @Test
    fun `language is registered singleton`() {
        val found = Language.findLanguageByID("Mermaid")
        assertSame(MermaidLanguage, found)
    }
}
