package com.alextdev.mermaidvisualizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.Base64

class MermaidErrorReportTest {

    private val env = ReportEnvironment(
        pluginVersion = "1.13.1",
        ide = "IntelliJ IDEA 2025.3 (build IU-253.1234.5)",
        os = "Mac OS X 15.1 (aarch64)",
        mermaidVersion = "12.2.0",
    )

    private val parseError = "Parse error on line 3:\n...hart TD    A[Start\n---------------------^\n" +
        "Expecting 'SQE', 'PE', got '1'"

    private fun queryParam(url: String, name: String): String? =
        URI(url).rawQuery.split('&')
            .map { it.substringBefore('=') to it.substringAfter('=') }
            .firstOrNull { it.first == name }
            ?.second
            ?.let { URLDecoder.decode(it, Charsets.UTF_8) }

    // --- Report body ---

    @Test
    fun `report lists the environment, summary, raw error and an empty diagram block`() {
        val report = buildErrorReport(parseError, "Line 2 looks unfinished: expected ]", "incomplete", env)

        assertTrue(report.startsWith("**What happened:** _(please describe"), "The reporter is asked for context first")
        assertTrue(report.contains("- Plugin version: 1.13.1"))
        assertTrue(report.contains("- IDE: IntelliJ IDEA 2025.3 (build IU-253.1234.5)"))
        assertTrue(report.contains("- OS: Mac OS X 15.1 (aarch64)"))
        assertTrue(report.contains("- Mermaid.js version: 12.2.0"))
        assertTrue(report.contains("**Summary:** Line 2 looks unfinished: expected ]"))
        assertTrue(report.contains("**Category:** `incomplete`"))
        assertTrue(report.contains("```text\n$parseError\n```"), "The raw error should be in a fenced block, untouched")
        assertTrue(report.contains("**Diagram (please paste a minimal example):**\n\n```mermaid\n\n```"),
            "The diagram block should be left empty for the reporter")

        val order = listOf("**What happened:**", "- Plugin version:", "- IDE:", "- OS:", "- Mermaid.js version:", "**Summary:**", "```text", "```mermaid")
            .map { report.indexOf(it) }
        assertEquals(order.sorted(), order, "Sections should appear in order: $order")
    }

    @Test
    fun `report never includes the diagram source`() {
        val secret = "flowchar TD\n    Payroll --> SecretBonus[Confidential 2026 bonus plan]"
        val raw = "No diagram type detected matching given configuration for text: $secret"

        val report = buildErrorReport(raw, "Unknown diagram type \"flowchar\": check the first line of the diagram", "unknown-diagram", env)

        assertFalse(report.contains("SecretBonus"), "Only the first line of the embedded diagram text may be kept")
        assertFalse(report.contains("Confidential"))
        assertTrue(report.contains("No diagram type detected matching given configuration for text: flowchar TD [rest of the diagram omitted]"))
        assertTrue(report.contains("```mermaid\n\n```"))
    }

    @Test
    fun `report handles a missing summary and ignores unknown categories`() {
        val report = buildErrorReport("Boom", null, "<script>", env)

        assertTrue(report.contains("**Summary:** (none)"))
        assertFalse(report.contains("**Category:**"))
        assertFalse(report.contains("<script>"))
    }

    @Test
    fun `raw error containing backticks cannot close its fence`() {
        val raw = "Unexpected ``` and ```` in text"

        val report = buildErrorReport(raw, "x", "generic", env)

        assertTrue(report.contains("`````text\n$raw\n`````"), "The fence should be longer than any backtick run of the error")
    }

    @Test
    fun `very long raw errors are capped`() {
        val report = buildErrorReport("x".repeat(10_000), "x", "generic", env)

        assertTrue(report.length < 6_000, "Report length: ${report.length}")
        assertTrue(report.contains("[…]"))
    }

    // --- Title ---

    @Test
    fun `title includes the summary truncated to 80 chars`() {
        assertEquals("[Bug] Render error: Syntax error at line 3", buildIssueTitle("Syntax error at line 3"))

        val title = buildIssueTitle("a".repeat(200))
        val summaryPart = title.removePrefix("[Bug] Render error: ")
        assertEquals(80, summaryPart.length)
        assertTrue(summaryPart.endsWith("…"))
    }

    @Test
    fun `title without summary`() {
        assertEquals("[Bug] Render error", buildIssueTitle(null))
        assertEquals("[Bug] Render error", buildIssueTitle("   "))
        assertEquals("[Bug] Render error: a b", buildIssueTitle("a\n  b"))
    }

    // --- URL ---

    private fun fields(raw: String = parseError, summary: String? = "Line 2 looks unfinished: expected ]", category: String? = "incomplete") =
        buildIssueFormFields(ErrorReportRequest(raw, summary, category), env)

    @Test
    fun `issue url targets the render error form of the plugin repository with every field prefilled`() {
        val url = buildGitHubIssueUrl(fields(summary = "été + 100%"))

        assertTrue(url.startsWith("https://github.com/Alex9583/MermaidVisualizer/issues/new?template=render-error.yml&title="))
        assertFalse(url.contains('+'), "Spaces must be encoded as %20 and '+' as %2B")
        assertFalse(url.contains(' '))
        assertTrue(url.contains("%20"))
        assertTrue(url.contains("%2B"))
        assertEquals(GITHUB_ISSUE_TEMPLATE, queryParam(url, "template"))
        assertEquals("[Bug] Render error: été + 100%", queryParam(url, "title"))
        assertEquals("été + 100%", queryParam(url, ISSUE_FIELD_SUMMARY))
        assertEquals("incomplete", queryParam(url, ISSUE_FIELD_CATEGORY))
        assertEquals(parseError, queryParam(url, ISSUE_FIELD_ERROR), "A short error should be sent in full")
        assertEquals(formatEnvironment(env), queryParam(url, ISSUE_FIELD_ENVIRONMENT))
        assertTrue(queryParam(url, ISSUE_FIELD_ENVIRONMENT)!!.contains("- Plugin version: 1.13.1\n- IDE: IntelliJ IDEA 2025.3"))
        assertNull(queryParam(url, "body"), "Issue forms ignore the body parameter")
        assertTrue(isSafeExternalUrl(url, GITHUB_ISSUE_URL_MAX_LENGTH))
    }

    @Test
    fun `fields follow the parameter order template, title, summary, category, environment, error`() {
        val url = buildGitHubIssueUrl(fields())
        val names = URI(url).rawQuery.split('&').map { it.substringBefore('=') }
        assertEquals(listOf("template", "title", ISSUE_FIELD_SUMMARY, ISSUE_FIELD_CATEGORY, ISSUE_FIELD_ENVIRONMENT, ISSUE_FIELD_ERROR), names)
    }

    @Test
    fun `a blank summary and an unknown category are not sent`() {
        val url = buildGitHubIssueUrl(fields(summary = "  ", category = "<script>"))

        assertEquals("[Bug] Render error", queryParam(url, "title"))
        assertNull(queryParam(url, ISSUE_FIELD_SUMMARY))
        assertNull(queryParam(url, ISSUE_FIELD_CATEGORY))
        assertFalse(url.contains("script"))
        assertEquals(parseError, queryParam(url, ISSUE_FIELD_ERROR))
    }

    @Test
    fun `the error field is sanitized and the summary capped like the report`() {
        val secret = "flowchar TD\n    Payroll --> SecretBonus[Confidential]"
        val url = buildGitHubIssueUrl(fields(raw = "No diagram type detected matching given configuration for text: $secret", summary = "s ".repeat(400)))

        val error = requireNotNull(queryParam(url, ISSUE_FIELD_ERROR))
        assertFalse(error.contains("SecretBonus"))
        assertTrue(error.endsWith("for text: flowchar TD [rest of the diagram omitted]"))
        assertEquals(300, queryParam(url, ISSUE_FIELD_SUMMARY)!!.length)
    }

    @Test
    fun `long errors are truncated to fit 6000 chars with a note, the other fields intact`() {
        val raw = "Parse error: " + "é€ ".repeat(1_200)
        assertTrue(buildGitHubIssueUrl(fields(raw = raw.substring(0, 100))).length < GITHUB_ISSUE_URL_MAX_LENGTH)

        val url = buildGitHubIssueUrl(fields(raw = raw))

        assertTrue(url.length <= GITHUB_ISSUE_URL_MAX_LENGTH, "URL length: ${url.length}")
        assertTrue(url.length > GITHUB_ISSUE_URL_MAX_LENGTH - 200, "The budget should be used, not wasted: ${url.length}")
        val sentError = requireNotNull(queryParam(url, ISSUE_FIELD_ERROR)) { "No error parameter in $url" }
        assertTrue(sentError.endsWith("\n\n$REPORT_TRUNCATION_NOTE"))
        assertTrue(sentError.startsWith("Parse error: é€ é€"))
        assertEquals(formatEnvironment(env), queryParam(url, ISSUE_FIELD_ENVIRONMENT), "Only the error field is cut")
        assertEquals("Line 2 looks unfinished: expected ]", queryParam(url, ISSUE_FIELD_SUMMARY))
        assertTrue(isSafeExternalUrl(url, GITHUB_ISSUE_URL_MAX_LENGTH))
        assertFalse(isSafeExternalUrl(url), "Issue URLs need the raised limit of openExternalLink")
    }

    @Test
    fun `truncation never splits a surrogate pair`() {
        val url = buildGitHubIssueUrl(fields(raw = "😀".repeat(3_000)))

        assertTrue(url.length <= GITHUB_ISSUE_URL_MAX_LENGTH)
        val sentError = requireNotNull(queryParam(url, ISSUE_FIELD_ERROR)) { "No error parameter in $url" }
        assertFalse(sentError.contains('\uFFFD'), "No replacement character from a cut surrogate pair")
        assertFalse(sentError.contains('?'))
        assertTrue(sentError.startsWith("😀😀"))
    }

    // --- Issue form template ---

    @Test
    fun `the issue form declares every prefilled field, a required description and the known categories`() {
        val template = File(".github/ISSUE_TEMPLATE/$GITHUB_ISSUE_TEMPLATE")
        assertTrue(template.isFile, "Missing ${template.absolutePath}")
        val yaml = template.readText()

        val ids = Regex("""^\s+id:\s*(\S+)\s*$""", RegexOption.MULTILINE).findAll(yaml).map { it.groupValues[1] }.toList()
        assertEquals(listOf("description", "diagram", ISSUE_FIELD_SUMMARY, ISSUE_FIELD_CATEGORY, ISSUE_FIELD_ERROR, ISSUE_FIELD_ENVIRONMENT), ids)

        val description = yaml.substring(yaml.indexOf("id: description")).substringBefore("- type:")
        assertTrue(description.contains("required: true"), "The reporter must describe what happened")

        val category = yaml.substring(yaml.indexOf("id: $ISSUE_FIELD_CATEGORY")).substringBefore("- type:")
        val options = Regex("""^\s+-\s+(\S+)\s*$""", RegexOption.MULTILINE).findAll(category).map { it.groupValues[1] }.toSet()
        assertEquals(KNOWN_CATEGORIES, options, "Dropdown options must match the categories the plugin may send")

        assertTrue(yaml.contains("title: \"$ISSUE_TITLE_PREFIX: \""))
    }

    // --- Payloads ---

    @Test
    fun `report request parsing`() {
        val request = parseErrorReportRequest(
            """{"raw":"Parse error","summary":"Syntax error at line 1","category":"syntax","line":1,"column":2}"""
        )
        assertEquals(ErrorReportRequest("Parse error", "Syntax error at line 1", "syntax"), request)

        assertEquals(ErrorReportRequest("x", null, null), parseErrorReportRequest("""{"raw":"x","summary":null,"category":3}"""))
        assertNull(parseErrorReportRequest("""{"summary":"no raw"}"""))
        assertNull(parseErrorReportRequest("""{"raw":"   "}"""))
        assertNull(parseErrorReportRequest("""{"raw":{"nested":true}}"""))
        assertNull(parseErrorReportRequest("""["raw"]"""))
        assertNull(parseErrorReportRequest("not json {"))
    }

    @Test
    fun `base64 payloads from the Markdown preview are decoded as UTF-8`() {
        val text = "Parse error near « é » 😀"
        val b64 = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

        assertEquals(text, decodeBase64Utf8(b64))
        assertNull(decodeBase64Utf8("not base64!"))
    }
}
