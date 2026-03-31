package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidElementType
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.psi.tree.IElementType

object MermaidElementTypeFactory {
    @JvmStatic
    fun createElement(name: String): IElementType = MermaidElementType(name)

    @JvmStatic
    fun createToken(name: String): IElementType = when (name) {
        "COMMENT" -> MermaidTokenTypes.COMMENT
        "DIRECTIVE" -> MermaidTokenTypes.DIRECTIVE
        "DIAGRAM_TYPE" -> MermaidTokenTypes.DIAGRAM_TYPE
        "KEYWORD" -> MermaidTokenTypes.KEYWORD
        "END_KW" -> MermaidTokenTypes.END_KW
        "STRING_DOUBLE" -> MermaidTokenTypes.STRING_DOUBLE
        "STRING_SINGLE" -> MermaidTokenTypes.STRING_SINGLE
        "ARROW" -> MermaidTokenTypes.ARROW
        "NUMBER" -> MermaidTokenTypes.NUMBER
        "BRACKET_OPEN" -> MermaidTokenTypes.BRACKET_OPEN
        "BRACKET_CLOSE" -> MermaidTokenTypes.BRACKET_CLOSE
        "COLON" -> MermaidTokenTypes.COLON
        "PIPE" -> MermaidTokenTypes.PIPE
        "SEMICOLON" -> MermaidTokenTypes.SEMICOLON
        "COMMA" -> MermaidTokenTypes.COMMA
        "IDENTIFIER" -> MermaidTokenTypes.IDENTIFIER
        else -> throw IllegalArgumentException(
            "Unknown token name '$name' in MermaidElementTypeFactory.createToken(). " +
            "Add this token to MermaidTokenTypes and update the when block."
        )
    }
}
