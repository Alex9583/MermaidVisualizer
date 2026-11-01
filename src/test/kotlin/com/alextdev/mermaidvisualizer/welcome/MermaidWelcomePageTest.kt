package com.alextdev.mermaidvisualizer.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidWelcomePageTest {

    private lateinit var template: String

    @BeforeAll
    fun loadTemplate() {
        val stream = MermaidWelcomePageTest::class.java.classLoader.getResourceAsStream(WELCOME_TEMPLATE_RESOURCE)
        assertNotNull(stream, "Missing $WELCOME_TEMPLATE_RESOURCE")
        template = stream!!.use { it.reader(Charsets.UTF_8).readText() }
    }

    private val sections = listOf(
        ChangelogSection("1.14.0", "2026-11-01", "- Add the welcome page (#40)"),
        ChangelogSection("1.13.1", "2026-10-18", "- Update Mermaid.js to v12.2.0 (#33)"),
    )

    private fun page(previousShown: String? = null, shown: List<ChangelogSection> = sections.take(1)) =
        buildWelcomePage(template, "1.14.0", "12.2.0", shown, previousShown)

    @Test
    fun `template contains every placeholder`() {
        for (placeholder in listOf(PLACEHOLDER_VERSION, PLACEHOLDER_MERMAID_VERSION, PLACEHOLDER_WHATS_NEW_SCOPE, PLACEHOLDER_WHATS_NEW)) {
            assertTrue(template.contains(placeholder), "Template lost $placeholder")
        }
    }

    @Test
    fun `no placeholder is left`() {
        val markdown = page()
        assertFalse(markdown.contains("{{"), markdown)
        assertFalse(markdown.contains("}}"), markdown)
    }

    @Test
    fun `title carries the version`() {
        assertEquals("# Welcome to Mermaid Visualizer 1.14.0", page().lineSequence().first())
    }

    @Test
    fun `bundled Mermaid version is mentioned`() {
        assertTrue(page().contains("Mermaid.js (v12.2.0)"))
        assertTrue(buildWelcomePage(template, "1.14.0", null, sections, null).contains("Mermaid.js (bundled)"))
    }

    @Test
    fun `support links are present with Buy Me a Coffee first`() {
        val markdown = page()
        val coffee = markdown.indexOf("($BUY_ME_A_COFFEE_URL)")
        val star = markdown.indexOf("($GITHUB_REPOSITORY_URL)")
        val rate = markdown.indexOf("($MARKETPLACE_REVIEWS_URL)")
        assertTrue(coffee >= 0, "Buy Me a Coffee link")
        assertTrue(star >= 0, "GitHub star link")
        assertTrue(rate >= 0, "Marketplace review link")
        assertTrue(coffee < star && coffee < rate, "Buy Me a Coffee comes first")
    }

    @Test
    fun `feedback links issues and mentions the error banner button`() {
        val markdown = page()
        assertTrue(markdown.contains("($GITHUB_ISSUES_URL)"))
        assertTrue(markdown.contains("Report on GitHub"))
    }

    @Test
    fun `page has exactly two live mermaid examples`() {
        assertEquals(2, Regex("^```mermaid$", RegexOption.MULTILINE).findAll(page()).count())
        assertTrue(page().contains("flowchart LR"))
        assertTrue(page().contains("sequenceDiagram"))
    }

    @Test
    fun `footer explains how to disable and reopen the page`() {
        val markdown = page()
        assertTrue(markdown.contains("Show the welcome page after plugin updates"))
        assertTrue(markdown.contains("Help > Mermaid Visualizer: What's New"))
    }

    @Test
    fun `settings tour lists the Mermaid settings and the color scheme page`() {
        val markdown = page()
        for (label in listOf("Theme", "Look", "Layout engine", "Font family", "Max text size", "Max edges",
            "Live reload delay", "Error highlight delay", "Max preview height", "Custom background and line colors")) {
            assertTrue(markdown.contains("**$label**"), "Settings tour misses $label")
        }
        assertTrue(markdown.contains("Settings > Tools > Mermaid"))
        assertTrue(markdown.contains("Settings > Editor > Color Scheme > Mermaid"))
    }

    @Test
    fun `single version heading uses in`() {
        val markdown = page()
        assertTrue(markdown.contains("## What's new in 1.14.0"))
        assertTrue(markdown.contains("- Add the welcome page"))
    }

    @Test
    fun `several versions heading uses since the previous version`() {
        val markdown = page(previousShown = "1.13.0", shown = sections)
        assertTrue(markdown.contains("## What's new since 1.13.0"))
        assertTrue(markdown.contains("### 1.14.0 (2026-11-01)"))
        assertTrue(markdown.contains("### 1.13.1 (2026-10-18)"))
    }

    @Test
    fun `fallback section shows its own version in the heading`() {
        val markdown = buildWelcomePage(template, "1.15.0-dev", "12.2.0", sections.take(1), null)
        assertTrue(markdown.contains("## What's new in 1.14.0"))
    }

    @Test
    fun `missing release notes are stated`() {
        val markdown = buildWelcomePage(template, "1.14.0", "12.2.0", emptyList(), null)
        assertTrue(markdown.contains("## What's new in 1.14.0"))
        assertTrue(markdown.contains("No release notes are bundled with this build"))
    }

    @Test
    fun `changelog text is not re-expanded`() {
        val tricky = listOf(ChangelogSection("1.14.0", null, "- Mentions {{VERSION}} literally"))
        assertTrue(buildWelcomePage(template, "1.14.0", "12.2.0", tricky, null).contains("{{VERSION}} literally"))
    }

    @Test
    fun `loadWelcomePage uses the bundled template and changelog`() {
        val markdown = loadWelcomePage("1.9.1", previousShown = null)
        assertEquals("# Welcome to Mermaid Visualizer 1.9.1", markdown.lineSequence().first())
        assertTrue(markdown.contains("## What's new in 1.9.1"))
        assertTrue(markdown.contains("system browser"), "1.9.1 release notes from the embedded CHANGELOG.md")
        assertFalse(markdown.contains("{{"))
    }

    @Test
    fun `loadWelcomePage after an update lists the versions since the previous one`() {
        val markdown = loadWelcomePage("1.10.0", previousShown = "1.9.0")
        assertTrue(markdown.contains("## What's new since 1.9.0"))
        assertTrue(markdown.contains("### 1.10.0"))
        assertTrue(markdown.contains("### 1.9.1"))
        assertFalse(markdown.contains("### 1.9.0"))
    }
}
