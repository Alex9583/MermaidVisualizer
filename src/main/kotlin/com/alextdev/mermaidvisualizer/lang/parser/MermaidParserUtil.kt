package com.alextdev.mermaidvisualizer.lang.parser

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key

/**
 * External rules for Grammar-Kit generated parser.
 *
 * Tracks diagram context via [PsiBuilder.putUserData] to dispatch block/divider keywords
 * per diagram type. ER and STATE have no block keywords (state diagrams use braces, not `end`).
 * GENERIC is a superset used as fallback for unknown diagram types and null context (error recovery).
 */
@Suppress("unused")
object MermaidParserUtil : GeneratedParserUtilBase() {

    private val DIAGRAM_CONTEXT_KEY = Key.create<DiagramContext>("MERMAID_DIAGRAM_CONTEXT")

    private val FLOWCHART_TYPES = setOf("flowchart", "graph")
    private val STATE_TYPES = setOf("stateDiagram-v2", "stateDiagram")

    private val FLOWCHART_BLOCK_KEYWORDS = setOf("subgraph")
    private val SEQUENCE_BLOCK_KEYWORDS = setOf(
        "loop", "alt", "opt", "par", "critical", "break", "rect", "box"
    )
    private val CLASS_BLOCK_KEYWORDS = setOf("namespace")
    private val GENERIC_BLOCK_KEYWORDS =
        FLOWCHART_BLOCK_KEYWORDS + SEQUENCE_BLOCK_KEYWORDS + CLASS_BLOCK_KEYWORDS + setOf("block")

    private val SEQUENCE_DIVIDERS = setOf("else", "and")

    private enum class DiagramContext {
        FLOWCHART, SEQUENCE, CLASS, ER, STATE, GENERIC
    }

    private fun blockKeywordsFor(context: DiagramContext?): Set<String> {
        return when (context) {
            DiagramContext.FLOWCHART -> FLOWCHART_BLOCK_KEYWORDS
            DiagramContext.SEQUENCE -> SEQUENCE_BLOCK_KEYWORDS
            DiagramContext.CLASS -> CLASS_BLOCK_KEYWORDS
            DiagramContext.ER -> emptySet()
            DiagramContext.STATE -> emptySet()
            DiagramContext.GENERIC, null -> GENERIC_BLOCK_KEYWORDS
        }
    }

    private fun dividerKeywordsFor(context: DiagramContext?): Set<String> {
        return when (context) {
            DiagramContext.SEQUENCE -> SEQUENCE_DIVIDERS
            else -> emptySet()
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
    fun consumeBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        if (text !in blockKeywordsFor(context)) return false
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
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeNonBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        if (text in blockKeywordsFor(context)) return false
        if (text in dividerKeywordsFor(context)) return false
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun consumeNonDividerIdentifier(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.IDENTIFIER) return false
        val text = builder.tokenText ?: return false
        val context = builder.getUserData(DIAGRAM_CONTEXT_KEY)
        if (text in dividerKeywordsFor(context)) return false
        builder.advanceLexer()
        return true
    }

    @JvmStatic
    fun isBlockKeyword(builder: PsiBuilder, level: Int): Boolean {
        if (builder.tokenType !== MermaidTokenTypes.KEYWORD) return false
        val text = builder.tokenText ?: return false
        return text in blockKeywordsFor(builder.getUserData(DIAGRAM_CONTEXT_KEY))
    }

    @JvmStatic
    fun isDividerKeyword(builder: PsiBuilder, level: Int): Boolean {
        val tokenType = builder.tokenType
        if (tokenType !== MermaidTokenTypes.KEYWORD && tokenType !== MermaidTokenTypes.IDENTIFIER) return false
        val text = builder.tokenText ?: return false
        return text in dividerKeywordsFor(builder.getUserData(DIAGRAM_CONTEXT_KEY))
    }
}