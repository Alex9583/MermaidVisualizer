package com.alextdev.mermaidvisualizer

import com.alextdev.mermaidvisualizer.welcome.currentPluginVersion
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.net.URLEncoder
import java.util.Base64

private val LOG = Logger.getInstance("MermaidErrorReport")

private const val UNKNOWN_VALUE = "unknown"

internal const val GITHUB_NEW_ISSUE_URL = "https://github.com/Alex9583/MermaidVisualizer/issues/new"

/** Issue form of `.github/ISSUE_TEMPLATE`, prefilled through its field ids passed as query parameters. */
internal const val GITHUB_ISSUE_TEMPLATE = "render-error.yml"
internal const val ISSUE_FIELD_SUMMARY = "summary"
internal const val ISSUE_FIELD_CATEGORY = "category"
internal const val ISSUE_FIELD_ERROR = "error"
internal const val ISSUE_FIELD_ENVIRONMENT = "environment"

/** GitHub rejects very long prefilled-issue URLs: the error field is cut so that the whole URL stays below this. */
internal const val GITHUB_ISSUE_URL_MAX_LENGTH = 6000

internal const val ISSUE_TITLE_PREFIX = "[Bug] Render error"
private const val ISSUE_TITLE_SUMMARY_MAX_LENGTH = 80
private const val ISSUE_SUMMARY_MAX_LENGTH = 300
private const val REPORT_RAW_MAX_LENGTH = 4000

/** Plain text (the error field is rendered as a code block): no Markdown emphasis. */
internal const val REPORT_TRUNCATION_NOTE = "[truncated: the full report has been copied to your clipboard]"

internal const val REPORT_WHAT_HAPPENED_PROMPT = "**What happened:** _(please describe what you were doing and what you expected)_"

// The error categories produced by classifyRenderError (web/mermaid-core-errors.js); also the options of the form's dropdown.
internal val KNOWN_CATEGORIES = setOf("incomplete", "syntax", "unknown-diagram", "limit", "generic")

// Mermaid's unknown-diagram error embeds the whole diagram text: only its first line is kept in a report.
private val UNKNOWN_DIAGRAM_TEXT = Regex(
    """(No diagram type detected matching given configuration for text:)\s*(.*)""",
    RegexOption.DOT_MATCHES_ALL,
)
private val BACKTICK_RUN = Regex("`+")
private val WHITESPACE_RUN = Regex("""\s+""")

/** Where the error happened, written at the top of a bug report. */
internal data class ReportEnvironment(
    val pluginVersion: String,
    val ide: String,
    val os: String,
    val mermaidVersion: String,
) {
    companion object {
        fun current(): ReportEnvironment {
            // Descriptor lookups by plugin id are internal API from 2026.2 on: read the version injected into the
            // welcome startup activity (PluginAware) instead.
            val pluginVersion = currentPluginVersion() ?: UNKNOWN_VALUE
            val appInfo = ApplicationInfo.getInstance()
            return ReportEnvironment(
                pluginVersion = pluginVersion,
                ide = "${appInfo.fullApplicationName} (build ${appInfo.build.asString()})",
                os = "${SystemInfo.OS_NAME} ${SystemInfo.OS_VERSION} (${SystemInfo.OS_ARCH})",
                mermaidVersion = readBundledMermaidVersion(),
            )
        }

        private fun readBundledMermaidVersion(): String = try {
            ReportEnvironment::class.java.classLoader.getResourceAsStream(MermaidWebResources.path(MermaidWebResources.MERMAID_VERSION))
                ?.use { it.reader(Charsets.UTF_8).readText().trim() }
                ?.ifEmpty { null }
                ?: UNKNOWN_VALUE
        } catch (e: IOException) {
            LOG.warn("Failed to read ${MermaidWebResources.MERMAID_VERSION}", e)
            UNKNOWN_VALUE
        }
    }
}

/** Payload of the "Report on GitHub" banner button: `{ raw, summary, category, line, column }`. */
internal data class ErrorReportRequest(val raw: String, val summary: String?, val category: String?)

/** Parses the banner payload, or returns null when it is not a JSON object with a non-blank `raw` string. */
internal fun parseErrorReportRequest(json: String): ErrorReportRequest? {
    val obj = try {
        JsonParser.parseString(json) as? JsonObject
    } catch (e: JsonParseException) {
        LOG.warn("Invalid error report payload (length=${json.length})", e)
        null
    } ?: return null
    val raw = obj.stringOrNull("raw")?.takeIf { it.isNotBlank() } ?: return null
    return ErrorReportRequest(raw, obj.stringOrNull("summary"), obj.stringOrNull("category"))
}

private fun JsonObject.stringOrNull(name: String): String? {
    val element: JsonElement = get(name) ?: return null
    return if (element.isJsonPrimitive && element.asJsonPrimitive.isString) element.asString else null
}

/** The environment lines of a report, shared by the Markdown report and the issue form's environment field. */
internal fun formatEnvironment(env: ReportEnvironment): String =
    "- Plugin version: ${env.pluginVersion}\n- IDE: ${env.ide}\n- OS: ${env.os}\n- Mermaid.js version: ${env.mermaidVersion}"

private fun cleanSummary(summary: String?): String = summary?.replace(WHITESPACE_RUN, " ")?.trim().orEmpty()

/**
 * Markdown body of a bug report for a Mermaid render error, copied to the clipboard (the issue URL may be
 * truncated). The diagram source is never included (it may be confidential): the reporter is asked to paste
 * a minimal example in the empty block. Written in English on purpose (not localized): it is meant for the
 * project's issue tracker.
 */
internal fun buildErrorReport(raw: String, summary: String?, category: String?, env: ReportEnvironment): String {
    val error = sanitizeRawForReport(raw)
    val fence = fenceFor(error)
    val cleanSummary = cleanSummary(summary)
    return buildString {
        appendLine(REPORT_WHAT_HAPPENED_PROMPT)
        appendLine()
        appendLine(formatEnvironment(env))
        appendLine()
        appendLine("**Summary:** ${cleanSummary.ifEmpty { "(none)" }}")
        if (category != null && category in KNOWN_CATEGORIES) {
            appendLine("**Category:** `$category`")
        }
        appendLine()
        appendLine("**Error message:**")
        appendLine()
        appendLine("${fence}text")
        appendLine(error)
        appendLine(fence)
        appendLine()
        appendLine("**Diagram (please paste a minimal example):**")
        appendLine()
        appendLine("```mermaid")
        appendLine()
        appendLine("```")
        appendLine()
        append("_Please review the error message before submitting: it may quote a few characters of your diagram._")
    }
}

/** Raw error text as it may appear in a report: diagram text redacted, length capped. */
internal fun sanitizeRawForReport(raw: String): String {
    val normalized = raw.replace("\r\n", "\n").trimEnd()
    val match = UNKNOWN_DIAGRAM_TEXT.find(normalized)
    val redacted = if (match == null) {
        normalized
    } else {
        val firstLine = match.groupValues[2].lineSequence().firstOrNull().orEmpty().trim()
        normalized.substring(0, match.range.first) + match.groupValues[1] + " " +
            safeTake(firstLine, 60) + " [rest of the diagram omitted]"
    }
    return if (redacted.length > REPORT_RAW_MAX_LENGTH) safeTake(redacted, REPORT_RAW_MAX_LENGTH) + "\n[…]" else redacted
}

/** A code fence longer than any backtick run of [content], so the content cannot close it. */
private fun fenceFor(content: String): String {
    val longestRun = BACKTICK_RUN.findAll(content).maxOfOrNull { it.value.length } ?: 0
    return "`".repeat(maxOf(3, longestRun + 1))
}

/** Issue title: `[Bug] Render error: <summary, at most 80 chars>`, or `[Bug] Render error` without summary. */
internal fun buildIssueTitle(summary: String?): String {
    val clean = cleanSummary(summary)
    if (clean.isEmpty()) return ISSUE_TITLE_PREFIX
    val short = if (clean.length > ISSUE_TITLE_SUMMARY_MAX_LENGTH) {
        safeTake(clean, ISSUE_TITLE_SUMMARY_MAX_LENGTH - 1).trimEnd() + "…"
    } else {
        clean
    }
    return "$ISSUE_TITLE_PREFIX: $short"
}

/** What the "Report on GitHub" flow prefills in the issue form (`.github/ISSUE_TEMPLATE/render-error.yml`). */
internal data class IssueFormFields(
    val title: String,
    val summary: String?,
    val category: String?,
    val error: String,
    val environment: String,
)

internal fun buildIssueFormFields(request: ErrorReportRequest, env: ReportEnvironment): IssueFormFields = IssueFormFields(
    title = buildIssueTitle(request.summary),
    summary = cleanSummary(request.summary).ifEmpty { null }?.let { safeTake(it, ISSUE_SUMMARY_MAX_LENGTH) },
    category = request.category?.takeIf { it in KNOWN_CATEGORIES },
    error = sanitizeRawForReport(request.raw),
    environment = formatEnvironment(env),
)

/**
 * Prefilled "new issue" URL targeting the render-error issue form: `template`, `title`, then one query
 * parameter per form field id (a missing summary or an unknown category is not sent). Values are UTF-8
 * percent-encoded (spaces as %20). When the URL would exceed [GITHUB_ISSUE_URL_MAX_LENGTH], the error
 * field (the only long one, sent last) is cut and ends with [REPORT_TRUNCATION_NOTE]: callers copy the
 * full report to the clipboard.
 */
internal fun buildGitHubIssueUrl(fields: IssueFormFields): String {
    val prefix = buildString {
        append(GITHUB_NEW_ISSUE_URL).append("?template=").append(encodeQueryValue(GITHUB_ISSUE_TEMPLATE))
        append("&title=").append(encodeQueryValue(fields.title))
        fields.summary?.let { append('&').append(ISSUE_FIELD_SUMMARY).append('=').append(encodeQueryValue(it)) }
        fields.category?.let { append('&').append(ISSUE_FIELD_CATEGORY).append('=').append(encodeQueryValue(it)) }
        append('&').append(ISSUE_FIELD_ENVIRONMENT).append('=').append(encodeQueryValue(fields.environment))
        append('&').append(ISSUE_FIELD_ERROR).append('=')
    }
    val full = prefix + encodeQueryValue(fields.error)
    if (full.length <= GITHUB_ISSUE_URL_MAX_LENGTH) return full

    val fits = { length: Int -> (prefix + encodeQueryValue(truncatedError(fields.error, length))).length <= GITHUB_ISSUE_URL_MAX_LENGTH }
    // Longest error prefix that fits; `low` always fits (0 = note only).
    var low = 0
    var high = fields.error.length
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (fits(mid)) low = mid else high = mid - 1
    }
    return prefix + encodeQueryValue(truncatedError(fields.error, low))
}

private fun truncatedError(error: String, length: Int): String =
    safeTake(error, length).trimEnd() + "\n\n" + REPORT_TRUNCATION_NOTE

private fun encodeQueryValue(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

/** First [length] chars of [text], without splitting a surrogate pair. */
private fun safeTake(text: String, length: Int): String {
    if (length >= text.length) return text
    if (length <= 0) return ""
    val end = if (Character.isHighSurrogate(text[length - 1])) length - 1 else length
    return text.substring(0, end)
}

/** Decodes a base64 UTF-8 BrowserPipe payload (Markdown preview convention), or returns null. */
internal fun decodeBase64Utf8(b64: String): String? = try {
    String(Base64.getDecoder().decode(b64), Charsets.UTF_8)
} catch (e: IllegalArgumentException) {
    LOG.warn("Invalid base64 payload from the Markdown preview (length=${b64.length})", e)
    null
}

/** "Copy error" banner button: puts the raw Mermaid message on the clipboard. Call on the EDT. */
internal fun copyErrorToClipboard(raw: String, project: Project?) {
    if (raw.isBlank()) {
        LOG.warn("Copy error requested with an empty message")
        notifyMermaid(project, MyMessageBundle.message("render.error.copy.failed"), NotificationType.ERROR)
        return
    }
    try {
        CopyPasteManager.getInstance().setContents(StringSelection(raw))
        notifyMermaid(project, MyMessageBundle.message("render.error.copy.success"), NotificationType.INFORMATION)
    } catch (e: IllegalStateException) {
        LOG.warn("Failed to copy the Mermaid error to the clipboard", e)
        notifyMermaid(project, MyMessageBundle.message("render.error.copy.failed"), NotificationType.ERROR)
    }
}

/**
 * "Report on GitHub" banner button: builds the report from the JSON payload, copies it to the
 * clipboard (the URL may be truncated) and opens a prefilled GitHub issue. Call on the EDT.
 */
internal fun reportErrorOnGitHub(jsonPayload: String, project: Project?) {
    val request = parseErrorReportRequest(jsonPayload)
    if (request == null) {
        LOG.warn("Ignoring malformed error report payload (length=${jsonPayload.length})")
        notifyMermaid(project, MyMessageBundle.message("render.error.report.failed"), NotificationType.ERROR)
        return
    }
    reportErrorOnGitHub(request, project)
}

/**
 * Builds the report for [request], copies it to the clipboard (the URL may be truncated) and opens a
 * prefilled GitHub issue. Shared by the banner button and the editor intention. Call on the EDT.
 */
internal fun reportErrorOnGitHub(request: ErrorReportRequest, project: Project?) {
    if (request.raw.isBlank()) {
        LOG.warn("Error report requested with an empty message")
        notifyMermaid(project, MyMessageBundle.message("render.error.report.failed"), NotificationType.ERROR)
        return
    }
    val env = ReportEnvironment.current()
    val body = buildErrorReport(request.raw, request.summary, request.category, env)
    try {
        CopyPasteManager.getInstance().setContents(StringSelection(body))
    } catch (e: IllegalStateException) {
        LOG.warn("Failed to copy the error report to the clipboard", e)
    }
    openExternalLink(buildGitHubIssueUrl(buildIssueFormFields(request, env)), GITHUB_ISSUE_URL_MAX_LENGTH)
    notifyMermaid(project, MyMessageBundle.message("render.error.report.opened"), NotificationType.INFORMATION)
}

/** Markdown preview entry point of [copyErrorToClipboard] (base64 UTF-8 payload). */
internal fun copyErrorToClipboardFromBase64(b64: String, project: Project?) {
    val raw = decodeBase64Utf8(b64)
    if (raw == null) {
        notifyMermaid(project, MyMessageBundle.message("render.error.copy.failed"), NotificationType.ERROR)
        return
    }
    copyErrorToClipboard(raw, project)
}

/** Markdown preview entry point of [reportErrorOnGitHub] (base64 UTF-8 payload). */
internal fun reportErrorOnGitHubFromBase64(b64: String, project: Project?) {
    val json = decodeBase64Utf8(b64)
    if (json == null) {
        notifyMermaid(project, MyMessageBundle.message("render.error.report.failed"), NotificationType.ERROR)
        return
    }
    reportErrorOnGitHub(json, project)
}
