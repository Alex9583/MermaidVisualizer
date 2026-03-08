package com.alextdev.mermaidvisualizer.lang

import com.intellij.psi.TokenType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidColorSettingsPageTest {

    private val page = MermaidColorSettingsPage()

    @Test
    fun testDisplayNameNotEmpty() {
        assertTrue(page.displayName.isNotEmpty())
    }

    @Test
    fun testIconNotNull() {
        assertNotNull(page.icon)
    }

    @Test
    fun testDemoTextNotEmpty() {
        assertTrue(page.demoText.isNotEmpty())
    }

    @Test
    fun testDescriptorsCoverAllColorKeys() {
        val descriptorKeys = page.attributeDescriptors.map { it.key }.toSet()
        val allKeys = setOf(
            MERMAID_COMMENT_KEY, MERMAID_DIRECTIVE_KEY, MERMAID_DIAGRAM_TYPE_KEY,
            MERMAID_KEYWORD_KEY, MERMAID_STRING_KEY, MERMAID_ARROW_KEY,
            MERMAID_NUMBER_KEY, MERMAID_BRACES_KEY, MERMAID_IDENTIFIER_KEY,
            MERMAID_BAD_CHAR_KEY
        )
        assertEquals(allKeys, descriptorKeys)
    }

    @Test
    fun testDemoTextTokenizesWithoutBadCharacters() {
        val lexer = MermaidLexer()
        lexer.start(page.demoText)
        while (lexer.tokenType != null) {
            assertNotEquals(TokenType.BAD_CHARACTER, lexer.tokenType,
                "BAD_CHARACTER found at offset ${lexer.tokenStart}: '${lexer.tokenText}'")
            lexer.advance()
        }
    }
}
