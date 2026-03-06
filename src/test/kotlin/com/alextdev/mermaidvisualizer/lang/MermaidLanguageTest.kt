package com.alextdev.mermaidvisualizer.lang

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
    fun `language is singleton`() {
        assertSame(MermaidLanguage, MermaidLanguage)
    }
}
