package com.alextdev.mermaidvisualizer.lang.parser

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.psi.TokenType

/**
 * External rules for Grammar-Kit generated parser.
 *
 * Tracks diagram context via [PsiBuilder.putUserData] to dispatch block/divider keywords
 * per diagram type. CLASS, ER, STATE and CONTENT have no `end`-terminated blocks
 * (namespaces and composite states use braces, content diagrams use indentation).
 * GENERIC is a superset used only for null context (error recovery outside any diagram).
 */
@Suppress("unused")
object MermaidParserUtil : GeneratedParserUtilBase() {

    private val DIAGRAM_CONTEXT_KEY = Key.create<DiagramContext>("MERMAID_DIAGRAM_CONTEXT")

    private val FLOWCHART_TYPES = setOf("flowchart", "graph", "swimlane-beta")
    private val STATE_TYPES = setOf("stateDiagram-v2", "stateDiagram")

    private val FLOWCHART_BLOCK_KEYWORDS = setOf("subgraph")
    private val SEQUENCE_BLOCK_KEYWORDS = setOf(
        "loop", "alt", "opt", "par", "critical", "break", "rect", "box"
    )
    private val BLOCK_BETA_BLOCK_KEYWORDS = setOf("block")
    private val GENERIC_BLOCK_KEYWORDS =
        FLOWCHART_BLOCK_KEYWORDS + SEQUENCE_BLOCK_KEYWORDS + BLOCK_BETA_BLOCK_KEYWORDS

    private val SEQUENCE_DIVIDERS = setOf("else", "and")

    private enum class DiagramContext {
        FLOWCHART, SEQUENCE, CLASS, ER, STATE, BLOCK_BETA, CONTENT, GENERIC
    }

    private fun blockKeywordsFor(context: DiagramContext?): Set<String> {
        return when (context) {
            DiagramContext.FLOWCHART -> FLOWCHART_BLOCK_KEYWORDS
            DiagramContext.SEQUENCE -> SEQUENCE_BLOCK_KEYWORDS
            DiagramContext.BLOCK_BETA -> BLOCK_BETA_BLOCK_KEYWORDS
            DiagramContext.CLASS,
            DiagramContext.ER,
            DiagramContext.STATE,
            DiagramContext.CONTENT -> emptySet()
            DiagramContext.GENERIC, null -> GENERIC_BLOCK_KEYWORDS
        }
    }

    private fun dividerKeywordsFor(context: DiagramContext?): Set<String> {
        return when (context) {
            DiagramContext.SEQUENCE -> SEQUENCE_DIVIDERS
            else -> emptySet()
        }
    }

    /**
     * Mermaid only recognizes block/divider keywords at the start of a logical statement
     * (beginning of line, or after a `;` separator). Mid-line occurrences — typically
     * message text such as `A->>B: check and verify` — are plain text.
     */
    private fun isAtStatementStart(builder: PsiBuilder): Boolean {
        var i = -1
        while (true) {
            val type = builder.rawLookup(i) ?: return true
            if (type === MermaidTokenTypes.SEMICOLON) return true
            if (type !== TokenType.WHITE_SPACE) return false
            val start = builder.rawTokenTypeStart(i)
            val end = builder.rawTokenTypeStart(i + 1)
            val ws = builder.originalText.subSequence(start, end)
            for (c in ws) {
                if (c == '\n' || c == '\r') return true
            }
            i--
        }
    }

    private fun consumeDiagramType(
        builder: PsiBuilder,
        context: DiagramContext,
        predicate: (String) -> Boolean
    ): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.DIAGRAM_TYPE) return false
        val text = builder.tokenText ?: return false
        if (!predicate(text)) return false
        builder.putUserData(DIAGRAM_CONTEXT_KEY, context)
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeFlowchartType(builder: PsiBuilder, level: Int): Boolean =
        consumeDiagramType(builder, DiagramContext.FLOWCHART) { it in FLOWCHART_TYPES }

    @JvmStatic
    fun consumeSequenceType(builder: PsiBuilder, level: Int): Boolean =
        consumeDiagramType(builder, DiagramContext.SEQUENCE) { it == "sequenceDiagram" }

    @JvmStatic
    fun consumeClassType(builder: PsiBuilder, level: Int): Boolean =
        consumeDiagramType(builder, DiagramContext.CLASS) { it == "classDiagram" }

    @JvmStatic
    fun consumeErType(builder: PsiBuilder, level: Int): Boolean =
        consumeDiagramType(builder, DiagramContext.ER) { it == "erDiagram" }

    @JvmStatic
    fun consumeStateType(builder: PsiBuilder, level: Int): Boolean =
        consumeDiagramType(builder, DiagramContext.STATE) { it in STATE_TYPES }

    @JvmStatic
    fun consumeGenericType(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.DIAGRAM_TYPE) return false
        val text = builder.tokenText ?: return false
        val context = if (text == "block-beta") DiagramContext.BLOCK_BETA else DiagramContext.CONTENT
        builder.putUserData(DIAGRAM_CONTEXT_KEY, context)
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        if (text !in blockKeywordsFor(context)) return false
        if (!isAtStatementStart(builder)) return false
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeDividerKeyword(builder: PsiBuilder, level: Int): Boolean {
        val tokenType = builder.tokenType
        if (tokenType !== MermaidTokenTypes.KEYWORD && tokenType !== MermaidTokenTypes.IDENTIFIER) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        if (text !in dividerKeywordsFor(context)) return false
        if (!isAtStatementStart(builder)) return false
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeNonBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        val isStructural = text in blockKeywordsFor(context) || text in dividerKeywordsFor(context)
        if (isStructural && isAtStatementStart(builder)) return false
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun isBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        return text in blockKeywordsFor(builder.getUserData(DIAGRAM_CONTEXT_KEY)) && isAtStatementStart(builder)
    }

    @JvmStatic
    fun isDividerKeyword(builder: PsiBuilder, level: Int): Boolean {
        val tokenType = builder.tokenType
        if (tokenType !== MermaidTokenTypes.KEYWORD && tokenType !== MermaidTokenTypes.IDENTIFIER) return false
        val text = builder.tokenText ?: return false
        return text in dividerKeywordsFor(builder.getUserData(DIAGRAM_CONTEXT_KEY)) && isAtStatementStart(builder)
    }
}