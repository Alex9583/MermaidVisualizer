package com.alextdev.mermaidvisualizer.lang

import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidParserDefinitionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    fun testParseMmdFileCreatesMermaidFile() {
        val psiFile = myFixture.configureByFile("simple.mmd")
        assertInstanceOf(psiFile, MermaidFile::class.java)
    }

    fun testCommentTokensSet() {
        val parserDef = MermaidParserDefinition()
        assertTrue(parserDef.commentTokens.contains(MermaidTokenTypes.COMMENT))
    }

    fun testStringLiteralElements() {
        val parserDef = MermaidParserDefinition()
        assertTrue(parserDef.stringLiteralElements.contains(MermaidTokenTypes.STRING_DOUBLE))
        assertTrue(parserDef.stringLiteralElements.contains(MermaidTokenTypes.STRING_SINGLE))
    }

    fun testFileNodeType() {
        val parserDef = MermaidParserDefinition()
        assertEquals(MERMAID_FILE, parserDef.fileNodeType)
    }

    fun testLexerProducesTokensForFixture() {
        myFixture.configureByFile("simple.mmd")
        val parserDef = MermaidParserDefinition()
        val lexer = parserDef.createLexer(project)
        lexer.start(myFixture.file.text)
        val tokens = mutableListOf<IElementType>()
        while (lexer.tokenType != null) {
            tokens.add(lexer.tokenType!!)
            lexer.advance()
        }
        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.contains(MermaidTokenTypes.DIAGRAM_TYPE))
    }
}
