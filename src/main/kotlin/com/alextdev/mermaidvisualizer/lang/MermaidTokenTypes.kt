package com.alextdev.mermaidvisualizer.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MermaidTokenType(debugName: String) : IElementType(debugName, MermaidLanguage)

val MERMAID_FILE = IFileElementType(MermaidLanguage)

object MermaidTokenTypes {
    @JvmField val COMMENT = MermaidTokenType("COMMENT")
    @JvmField val DIRECTIVE = MermaidTokenType("DIRECTIVE")
    @JvmField val DIAGRAM_TYPE = MermaidTokenType("DIAGRAM_TYPE")
    @JvmField val KEYWORD = MermaidTokenType("KEYWORD")
    @JvmField val STRING_DOUBLE = MermaidTokenType("STRING_DOUBLE")
    @JvmField val STRING_SINGLE = MermaidTokenType("STRING_SINGLE")
    @JvmField val ARROW = MermaidTokenType("ARROW")
    @JvmField val NUMBER = MermaidTokenType("NUMBER")
    @JvmField val BRACKET_OPEN = MermaidTokenType("BRACKET_OPEN")
    @JvmField val BRACKET_CLOSE = MermaidTokenType("BRACKET_CLOSE")
    @JvmField val COLON = MermaidTokenType("COLON")
    @JvmField val PIPE = MermaidTokenType("PIPE")
    @JvmField val SEMICOLON = MermaidTokenType("SEMICOLON")
    @JvmField val COMMA = MermaidTokenType("COMMA")
    @JvmField val IDENTIFIER = MermaidTokenType("IDENTIFIER")
    @JvmField val END_KW = MermaidTokenType("END_KW")
}

object MermaidTokenSets {
    @JvmField val COMMENTS = TokenSet.create(MermaidTokenTypes.COMMENT)
    @JvmField val WHITE_SPACES = TokenSet.create(com.intellij.psi.TokenType.WHITE_SPACE)
    @JvmField val STRINGS = TokenSet.create(MermaidTokenTypes.STRING_DOUBLE, MermaidTokenTypes.STRING_SINGLE)
}
