package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidElementType
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MermaidElementTypeFactoryTest {

    @ParameterizedTest
    @CsvSource(
        "COMMENT", "DIRECTIVE", "DIAGRAM_TYPE", "KEYWORD", "END_KW",
        "STRING_DOUBLE", "STRING_SINGLE", "ARROW", "NUMBER",
        "BRACKET_OPEN", "BRACKET_CLOSE", "COLON", "PIPE",
        "SEMICOLON", "COMMA", "IDENTIFIER"
    )
    fun testCreateTokenReturnsCanonicalInstance(name: String) {
        val expected = MermaidTokenTypes::class.java.getField(name).get(null)
        assertSame(expected, MermaidElementTypeFactory.createToken(name),
            "createToken('$name') must return the same instance as MermaidTokenTypes.$name")
    }

    @Test
    fun testCreateTokenThrowsForUnknownName() {
        assertThrows(IllegalArgumentException::class.java) {
            MermaidElementTypeFactory.createToken("UNKNOWN_TOKEN")
        }
    }

    @Test
    fun testCreateElementReturnsNewInstance() {
        val element = MermaidElementTypeFactory.createElement("SOME_ELEMENT")
        assertInstanceOf(MermaidElementType::class.java, element)
    }
}