package com.alextdev.mermaidvisualizer.lang

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * Regression net over every fenced ```mermaid block of `all-diagrams.md`: each block is parsed
 * as a standalone `.mmd` file and must produce no [PsiErrorElement]. Failures are collected so
 * one run reports every offending block.
 */
class MermaidCorpusTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    /** Block indexes (0-based, in file order) known to fail before the lexer rework; justified inline. */
    private val knownBaselineErrors: Set<Int> = emptySet()

    private fun corpusBlocks(): List<String> {
        val markdown = File(testDataPath, "all-diagrams.md").readText()
        return Regex("```mermaid\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(markdown)
            .map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun testEveryCorpusBlockParsesWithoutErrors() {
        val blocks = corpusBlocks()
        assertTrue("Expected the corpus to contain fenced mermaid blocks", blocks.isNotEmpty())
        val failures = mutableListOf<String>()
        blocks.forEachIndexed { index, source ->
            val psiFile = myFixture.configureByText("block$index.mmd", source)
            val errors = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)
            val firstLine = source.lineSequence().first { it.isNotBlank() }.trim()
            if (errors.isNotEmpty() && index !in knownBaselineErrors) {
                failures.add("block $index (\"$firstLine\"): ${errors.map { "'${it.text}' -> ${it.errorDescription}" }}")
            }
        }
        assertTrue("Corpus blocks with parse errors:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    /** Lexer invariant: nothing unmatched, and no arrow/metadata character ever ends up inside an identifier. */
    fun testEveryCorpusBlockLexesCleanly() {
        val forbiddenInIdentifier = Regex("[@<>=~*&]|--|->|<-")
        val failures = mutableListOf<String>()
        corpusBlocks().forEachIndexed { index, source ->
            val firstLine = source.lineSequence().first { it.isNotBlank() }.trim()
            val lexer = MermaidLexer()
            lexer.start(source)
            while (lexer.tokenType != null) {
                val type = lexer.tokenType
                val text = lexer.tokenText
                if (type == TokenType.BAD_CHARACTER) {
                    failures.add("block $index (\"$firstLine\"): BAD_CHARACTER '$text'")
                } else if (type == MermaidTokenTypes.IDENTIFIER && forbiddenInIdentifier.containsMatchIn(text)) {
                    failures.add("block $index (\"$firstLine\"): arrow/metadata char inside IDENTIFIER '$text'")
                }
                lexer.advance()
            }
        }
        assertTrue("Corpus lexer invariant violations:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
