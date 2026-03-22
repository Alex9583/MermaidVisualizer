package com.alextdev.mermaidvisualizer.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

@JvmField val MERMAID_COMMENT_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)

@JvmField val MERMAID_DIRECTIVE_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_DIRECTIVE", DefaultLanguageHighlighterColors.METADATA)

@JvmField val MERMAID_DIAGRAM_TYPE_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_DIAGRAM_TYPE", DefaultLanguageHighlighterColors.KEYWORD)

@JvmField val MERMAID_KEYWORD_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

@JvmField val MERMAID_STRING_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_STRING", DefaultLanguageHighlighterColors.STRING)

@JvmField val MERMAID_ARROW_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_ARROW", DefaultLanguageHighlighterColors.MARKUP_TAG)

@JvmField val MERMAID_NUMBER_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

@JvmField val MERMAID_BRACES_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_BRACES", DefaultLanguageHighlighterColors.PARENTHESES)

@JvmField val MERMAID_IDENTIFIER_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_IDENTIFIER", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

@JvmField val MERMAID_PUNCTUATION_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_PUNCTUATION", DefaultLanguageHighlighterColors.OPERATION_SIGN)

@JvmField val MERMAID_BAD_CHAR_KEY: TextAttributesKey =
    TextAttributesKey.createTextAttributesKey("MERMAID_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

private val COMMENT_KEYS = arrayOf(MERMAID_COMMENT_KEY)
private val DIRECTIVE_KEYS = arrayOf(MERMAID_DIRECTIVE_KEY)
private val DIAGRAM_TYPE_KEYS = arrayOf(MERMAID_DIAGRAM_TYPE_KEY)
private val KEYWORD_KEYS = arrayOf(MERMAID_KEYWORD_KEY)
private val STRING_KEYS = arrayOf(MERMAID_STRING_KEY)
private val ARROW_KEYS = arrayOf(MERMAID_ARROW_KEY)
private val NUMBER_KEYS = arrayOf(MERMAID_NUMBER_KEY)
private val BRACES_KEYS = arrayOf(MERMAID_BRACES_KEY)
private val IDENTIFIER_KEYS = arrayOf(MERMAID_IDENTIFIER_KEY)
private val PUNCTUATION_KEYS = arrayOf(MERMAID_PUNCTUATION_KEY)
private val BAD_CHAR_KEYS = arrayOf(MERMAID_BAD_CHAR_KEY)
private val EMPTY_KEYS = emptyArray<TextAttributesKey>()

class MermaidSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = MermaidLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when (tokenType) {
            MermaidTokenTypes.COMMENT -> COMMENT_KEYS
            MermaidTokenTypes.DIRECTIVE -> DIRECTIVE_KEYS
            MermaidTokenTypes.DIAGRAM_TYPE -> DIAGRAM_TYPE_KEYS
            MermaidTokenTypes.KEYWORD, MermaidTokenTypes.END_KW -> KEYWORD_KEYS
            MermaidTokenTypes.STRING_DOUBLE, MermaidTokenTypes.STRING_SINGLE -> STRING_KEYS
            MermaidTokenTypes.ARROW -> ARROW_KEYS
            MermaidTokenTypes.NUMBER -> NUMBER_KEYS
            MermaidTokenTypes.BRACKET_OPEN, MermaidTokenTypes.BRACKET_CLOSE -> BRACES_KEYS
            MermaidTokenTypes.COLON, MermaidTokenTypes.SEMICOLON, MermaidTokenTypes.COMMA -> PUNCTUATION_KEYS
            MermaidTokenTypes.PIPE -> BRACES_KEYS
            MermaidTokenTypes.IDENTIFIER -> IDENTIFIER_KEYS
            TokenType.BAD_CHARACTER -> BAD_CHAR_KEYS
            else -> EMPTY_KEYS
        }
    }
}