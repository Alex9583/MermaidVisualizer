package com.alextdev.mermaidvisualizer.lang

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.psi.TokenType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidSyntaxHighlighterTest {

    private val highlighter = MermaidSyntaxHighlighter()

    @Test
    fun testCommentHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.COMMENT)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_COMMENT_KEY, keys[0])
    }

    @Test
    fun testDirectiveHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.DIRECTIVE)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_DIRECTIVE_KEY, keys[0])
    }

    @Test
    fun testDiagramTypeHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.DIAGRAM_TYPE)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_DIAGRAM_TYPE_KEY, keys[0])
    }

    @Test
    fun testKeywordHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.KEYWORD)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_KEYWORD_KEY, keys[0])
    }

    @Test
    fun testStringHighlights() {
        val doubleKeys = highlighter.getTokenHighlights(MermaidTokenTypes.STRING_DOUBLE)
        val singleKeys = highlighter.getTokenHighlights(MermaidTokenTypes.STRING_SINGLE)
        assertEquals(MERMAID_STRING_KEY, doubleKeys[0])
        assertEquals(MERMAID_STRING_KEY, singleKeys[0])
    }

    @Test
    fun testArrowHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.ARROW)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_ARROW_KEY, keys[0])
    }

    @Test
    fun testIdentifierHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.IDENTIFIER)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_IDENTIFIER_KEY, keys[0])
    }

    @Test
    fun testNumberHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.NUMBER)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_NUMBER_KEY, keys[0])
    }

    @Test
    fun testBracesHighlight() {
        val openKeys = highlighter.getTokenHighlights(MermaidTokenTypes.BRACKET_OPEN)
        val closeKeys = highlighter.getTokenHighlights(MermaidTokenTypes.BRACKET_CLOSE)
        assertEquals(1, openKeys.size)
        assertEquals(MERMAID_BRACES_KEY, openKeys[0])
        assertEquals(1, closeKeys.size)
        assertEquals(MERMAID_BRACES_KEY, closeKeys[0])
    }

    @Test
    fun testBadCharacterHighlight() {
        val keys = highlighter.getTokenHighlights(TokenType.BAD_CHARACTER)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_BAD_CHAR_KEY, keys[0])
    }

    @Test
    fun testPunctuationHighlight() {
        val colonKeys = highlighter.getTokenHighlights(MermaidTokenTypes.COLON)
        assertEquals(1, colonKeys.size)
        assertEquals(MERMAID_PUNCTUATION_KEY, colonKeys[0])
        val semicolonKeys = highlighter.getTokenHighlights(MermaidTokenTypes.SEMICOLON)
        assertEquals(1, semicolonKeys.size)
        assertEquals(MERMAID_PUNCTUATION_KEY, semicolonKeys[0])
        val commaKeys = highlighter.getTokenHighlights(MermaidTokenTypes.COMMA)
        assertEquals(1, commaKeys.size)
        assertEquals(MERMAID_PUNCTUATION_KEY, commaKeys[0])
    }

    @Test
    fun testPipeHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.PIPE)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_BRACES_KEY, keys[0])
    }

    @Test
    fun testHighlighterFactoryReturnsValidHighlighter() {
        val factory = MermaidSyntaxHighlighterFactory()
        val result = factory.getSyntaxHighlighter(null, null)
        assertInstanceOf(MermaidSyntaxHighlighter::class.java, result)
    }

    @Test
    fun testFallbackColors() {
        assertEquals(DefaultLanguageHighlighterColors.LINE_COMMENT, MERMAID_COMMENT_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.KEYWORD, MERMAID_KEYWORD_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.STRING, MERMAID_STRING_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.MARKUP_TAG, MERMAID_ARROW_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.NUMBER, MERMAID_NUMBER_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.PARENTHESES, MERMAID_BRACES_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.OPERATION_SIGN, MERMAID_PUNCTUATION_KEY.fallbackAttributeKey)
        assertEquals(DefaultLanguageHighlighterColors.INSTANCE_FIELD, MERMAID_IDENTIFIER_KEY.fallbackAttributeKey)
        assertEquals(HighlighterColors.BAD_CHARACTER, MERMAID_BAD_CHAR_KEY.fallbackAttributeKey)
    }

    @Test
    fun testEndKwHighlight() {
        val keys = highlighter.getTokenHighlights(MermaidTokenTypes.END_KW)
        assertEquals(1, keys.size)
        assertEquals(MERMAID_KEYWORD_KEY, keys[0])
    }

    @Test
    fun testHighlightingLexerType() {
        assertTrue(highlighter.getHighlightingLexer() is MermaidLexer)
    }
}
