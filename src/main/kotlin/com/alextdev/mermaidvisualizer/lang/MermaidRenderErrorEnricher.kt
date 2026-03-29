package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.intellij.openapi.util.text.StringUtil

/**
 * Enriched version of a [MermaidRenderError] with a user-friendly message
 * and an HTML tooltip providing detailed context.
 */
data class EnrichedRenderError(
    val message: String,
    val tooltip: String,
)

/**
 * Categorizes raw Mermaid.js error messages and produces enriched messages
 * with contextual information (valid types, syntax hints, etc.).
 */
object MermaidRenderErrorEnricher {

    private val DIAGRAM_NOT_FOUND = Regex("""Diagram (\S+) not found""", RegexOption.IGNORE_CASE)
    private val PARSE_ERROR_LINE = Regex("""(?:Parse|Syntax) error on line (\d+)""", RegexOption.IGNORE_CASE)
    private val EXPECTING_GOT = Regex("""[Ee]xpecting\s+'([^']+)'.*got\s+'([^']+)'""")
    private val LEXICAL_ERROR = Regex("""Lexical error on line (\d+)""", RegexOption.IGNORE_CASE)

    private val ALL_DIAGRAM_TYPES: String by lazy {
        MermaidDiagramKind.entries
            .map { it.keyword }
            .distinct()
            .joinToString(", ")
    }

    private val POPULAR_DIAGRAM_TYPES: String by lazy {
        listOf("flowchart", "sequenceDiagram", "classDiagram", "erDiagram", "stateDiagram-v2")
            .joinToString(", ")
    }

    fun enrich(error: MermaidRenderError): EnrichedRenderError {
        val raw = error.message

        DIAGRAM_NOT_FOUND.find(raw)?.let { match ->
            val diagramName = match.groupValues[1]
            val message = MyMessageBundle.message(
                "render.error.unknown.diagram", diagramName, POPULAR_DIAGRAM_TYPES,
            )
            val tooltip = buildString {
                append("<html><body>")
                append("<b>").append(StringUtil.escapeXmlEntities(message)).append("</b>")
                append("<br><br>")
                append("<b>").append(MyMessageBundle.message("tooltip.diagram.types.header")).append("</b><br>")
                append("<code>").append(StringUtil.escapeXmlEntities(ALL_DIAGRAM_TYPES)).append("</code>")
                append("<br><br>")
                append("<i>").append(MyMessageBundle.message("tooltip.render.error.original")).append("</i> ")
                append("<code>").append(StringUtil.escapeXmlEntities(raw)).append("</code>")
                append("</body></html>")
            }
            return EnrichedRenderError(message, tooltip)
        }

        PARSE_ERROR_LINE.find(raw)?.let { match ->
            val lineNum = match.groupValues[1]
            val detail = raw.substringAfter(match.value).trimStart(':', ' ', '\n')
                .ifEmpty { raw }
            val message = MyMessageBundle.message("render.error.parse", lineNum, detail)
            return EnrichedRenderError(message, buildGenericTooltip(message, raw))
        }

        EXPECTING_GOT.find(raw)?.let { match ->
            val expected = match.groupValues[1]
            val got = match.groupValues[2]
            val message = MyMessageBundle.message("render.error.expecting", expected, got)
            return EnrichedRenderError(message, buildGenericTooltip(message, raw))
        }

        LEXICAL_ERROR.find(raw)?.let { match ->
            val lineNum = match.groupValues[1]
            val detail = raw.substringAfter(match.value).trimStart(':', ' ', '\n')
                .ifEmpty { raw }
            val message = MyMessageBundle.message("render.error.lexical", lineNum, detail)
            return EnrichedRenderError(message, buildGenericTooltip(message, raw))
        }

        // Fallback: use the raw message with a generic wrapper
        val message = MyMessageBundle.message("render.error.generic", raw)
        return EnrichedRenderError(message, buildGenericTooltip(message, raw))
    }

    private fun buildGenericTooltip(message: String, raw: String): String = buildString {
        append("<html><body>")
        append("<b>").append(StringUtil.escapeXmlEntities(message)).append("</b>")
        if (message != raw && raw !in message) {
            append("<br><br>")
            append("<i>").append(MyMessageBundle.message("tooltip.render.error.original")).append("</i> ")
            append("<code>").append(StringUtil.escapeXmlEntities(raw)).append("</code>")
        }
        append("<br><br>")
        append(MyMessageBundle.message("tooltip.render.error.hint"))
        append("</body></html>")
    }
}
