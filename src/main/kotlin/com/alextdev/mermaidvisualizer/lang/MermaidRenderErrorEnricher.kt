package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.MermaidRenderErrorCategory
import com.alextdev.mermaidvisualizer.sanitizeRawForReport
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

/** Longest raw Mermaid message quoted in a tooltip (the full text stays available through "Copy Mermaid error"). */
private const val TOOLTIP_RAW_MAX_LENGTH = 600

/**
 * Produces the annotation message and HTML tooltip of a [MermaidRenderError].
 *
 * Errors classified by the JS side carry a one-line `summary` (whose line number is the document line):
 * it becomes the message, and the tooltip depends on the [MermaidRenderErrorCategory]. Errors without a
 * summary keep the legacy path: the raw Mermaid.js message is categorized with regexes and enriched with
 * contextual information (valid types, syntax hints, etc.).
 */
object MermaidRenderErrorEnricher {

    private val DIAGRAM_NOT_FOUND = Regex("""Diagram (\S+) not found""", RegexOption.IGNORE_CASE)
    private val PARSE_ERROR_LINE = Regex("""(?:Parse|Syntax) error on line (\d+)""", RegexOption.IGNORE_CASE)
    private val EXPECTING_GOT = Regex("""[Ee]xpecting\s+'([^']+)'.*got\s+'([^']+)'""")
    private val LEXICAL_ERROR = Regex("""Lexical error on line (\d+)""", RegexOption.IGNORE_CASE)

    // Rendering limits. The JS side (mermaid-core-runtime.js) already rewrites both cases into these messages;
    // Mermaid's raw edge-limit error is matched too in case it ever reaches Kotlin unrewritten.
    private val TEXT_SIZE_EXCEEDED = Regex("""Diagram text size exceeds the limit \((\d+) > (\d+) characters\)""")
    private val EDGE_LIMIT_EXCEEDED = Regex("""Diagram has more than (\d+) edges""")
    private val RAW_EDGE_LIMIT_EXCEEDED = Regex("""Edge limit exceeded\. (\d+) edges found""")

    // "line N" mentions in a raw Mermaid message: relative to Mermaid's preprocessed text.
    private val RAW_LINE_NUMBER = Regex("""\bline (\d+)""", RegexOption.IGNORE_CASE)
    private val SPACE_BEFORE_SPACE = Regex(" (?= )")

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

    /**
     * [intentionShortcut]: text of the "Show Context Actions" shortcut quoted in summary tooltips
     * (the annotator passes the active keymap's one); null falls back to the bundle default (Alt+Enter).
     */
    fun enrich(error: MermaidRenderError, intentionShortcut: String? = null): EnrichedRenderError {
        val summary = error.summary
        if (!summary.isNullOrBlank()) {
            return enrichFromSummary(error, summary.trim(), intentionShortcut)
        }
        val raw = error.message

        TEXT_SIZE_EXCEEDED.find(raw)?.let { match ->
            val message = MyMessageBundle.message("render.error.maxTextSize", match.groupValues[1], match.groupValues[2])
            return EnrichedRenderError(message, buildLimitTooltip(message))
        }

        (EDGE_LIMIT_EXCEEDED.find(raw) ?: RAW_EDGE_LIMIT_EXCEEDED.find(raw))?.let { match ->
            val message = MyMessageBundle.message("render.error.maxEdges", match.groupValues[1])
            return EnrichedRenderError(message, buildLimitTooltip(message))
        }

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

    private fun enrichFromSummary(error: MermaidRenderError, summary: String, intentionShortcut: String?): EnrichedRenderError {
        val tooltip = buildString {
            append("<html><body>")
            append("<b>").append(StringUtil.escapeXmlEntities(summary)).append("</b>")
            when (error.category) {
                MermaidRenderErrorCategory.INCOMPLETE -> {
                    append("<br><br>")
                    append(MyMessageBundle.message("tooltip.render.error.incomplete.hint"))
                }
                MermaidRenderErrorCategory.LIMIT -> {
                    append("<br><br>")
                    append(MyMessageBundle.message("tooltip.render.error.limit.hint"))
                }
                MermaidRenderErrorCategory.UNKNOWN_DIAGRAM -> {
                    appendRawMessage(error)
                    append("<br><br>")
                    append("<b>").append(MyMessageBundle.message("tooltip.diagram.types.header")).append("</b><br>")
                    append("<code>").append(StringUtil.escapeXmlEntities(ALL_DIAGRAM_TYPES)).append("</code>")
                    append("<br><br>")
                    append(MyMessageBundle.message("tooltip.render.error.hint"))
                }
                MermaidRenderErrorCategory.SYNTAX,
                MermaidRenderErrorCategory.GENERIC -> {
                    appendRawMessage(error)
                    append("<br><br>")
                    append(MyMessageBundle.message("tooltip.render.error.hint"))
                }
            }
            val shortcut = intentionShortcut?.takeIf { it.isNotBlank() }
                ?: MyMessageBundle.message("tooltip.render.error.actions.shortcut")
            append("<br><br>")
            append("<i>")
            append(StringUtil.escapeXmlEntities(MyMessageBundle.message("tooltip.render.error.actions", shortcut)))
            append("</i>")
            append("</body></html>")
        }
        return EnrichedRenderError(summary, tooltip)
    }

    /**
     * The raw Mermaid message, monospaced with its line breaks and space runs kept (Jison points at the
     * offending token with a `---^` line), capped, with the diagram text of "no diagram type detected"
     * errors redacted. Followed by a note when it quotes line numbers other than the annotated line.
     */
    private fun StringBuilder.appendRawMessage(error: MermaidRenderError) {
        val raw = sanitizeRawForReport(error.message).let {
            if (it.length <= TOOLTIP_RAW_MAX_LENGTH) return@let it
            // Never split a surrogate pair.
            val end = if (Character.isHighSurrogate(it[TOOLTIP_RAW_MAX_LENGTH - 1])) TOOLTIP_RAW_MAX_LENGTH - 1 else TOOLTIP_RAW_MAX_LENGTH
            it.substring(0, end).trimEnd() + "\u2026"
        }
        if (raw.isBlank()) return
        append("<br><br>")
        append("<i>").append(MyMessageBundle.message("tooltip.render.error.original")).append("</i><br>")
        append("<code>")
        raw.lines().forEachIndexed { index, line ->
            if (index > 0) append("<br>")
            append(StringUtil.escapeXmlEntities(line).replace(SPACE_BEFORE_SPACE, "&nbsp;"))
        }
        append("</code>")
        if (RAW_LINE_NUMBER.findAll(error.message).any { it.groupValues[1].toIntOrNull() != error.line }) {
            append("<br>")
            append("<i>").append(MyMessageBundle.message("tooltip.render.error.raw.line.note")).append("</i>")
        }
    }

    /** Limit errors are not syntax errors: the generic "check the syntax" hint would mislead. */
    private fun buildLimitTooltip(message: String): String = buildString {
        append("<html><body>")
        append("<b>").append(StringUtil.escapeXmlEntities(message)).append("</b>")
        append("<br><br>")
        append(MyMessageBundle.message("tooltip.render.error.limit.hint"))
        append("</body></html>")
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
