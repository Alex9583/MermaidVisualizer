package com.alextdev.mermaidvisualizer.welcome

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MermaidChangelogTest {

    private val synthetic = MermaidChangelog.parse(
        """
        # Changelog

        All notable changes to the Mermaid Visualizer plugin will be documented in this file.

        ## [1.10.0] - 2026-07-24

        - Add "Open in new tab" feature for Mermaid diagrams (#24)

        ## [1.9.1] - 2026-07-18

        - Open Mermaid click command links in the system browser (#22)
        - Second bullet

        ## [1.9.0] - 2026-07-06

        - Update Mermaid.js to v11.16.0 (#20)

        ## [1.8.0] - 2026-06-22

        - Add custom background/line colors (#18)
        """.trimIndent(),
    )

    private fun versions(sections: List<ChangelogSection>) = sections.map { it.version }

    // --- Parsing ---

    @Test
    fun `bundled changelog is embedded in the resources`() {
        val stream = MermaidChangelog::class.java.classLoader.getResourceAsStream(CHANGELOG_RESOURCE)
        assertNotNull(stream, "processResources must copy CHANGELOG.md to $CHANGELOG_RESOURCE")
        stream?.close()
    }

    @Test
    fun `bundled changelog parses into versioned sections with bullets`() {
        val changelog = MermaidChangelog.loadBundled()
        assertTrue(changelog.sections.size >= 20, "Expected the full history, got ${changelog.sections.size} sections")
        for (section in changelog.sections) {
            assertNotNull(PluginVersion.parse(section.version), "Unparsable version '${section.version}'")
            assertNotNull(section.date, "Section ${section.version} has no date")
            assertTrue(section.body.lines().any { it.startsWith("- ") }, "Section ${section.version} has no bullet")
            assertFalse(section.body.lines().any { it.startsWith("## ") }, "Section ${section.version} swallowed a header")
        }
    }

    @Test
    fun `bundled changelog contains known historical releases`() {
        val changelog = MermaidChangelog.loadBundled()
        val section = changelog.sectionFor("1.9.1")
        assertNotNull(section)
        assertEquals("2026-07-18", section?.date)
        assertTrue(section!!.body.contains("system browser"))
        assertNotNull(changelog.sectionFor("1.13.1"))
    }

    @Test
    fun `bundled changelog latest is the highest version`() {
        val changelog = MermaidChangelog.loadBundled()
        val latest = PluginVersion.parse(changelog.latest?.version)
        assertNotNull(latest)
        for (section in changelog.sections) {
            assertTrue(PluginVersion.parse(section.version)!! <= latest!!)
        }
    }

    @Test
    fun `parse extracts version date and body`() {
        assertEquals(listOf("1.10.0", "1.9.1", "1.9.0", "1.8.0"), versions(synthetic.sections))
        val section = synthetic.sections[1]
        assertEquals("2026-07-18", section.date)
        assertEquals("- Open Mermaid click command links in the system browser (#22)\n- Second bullet", section.body)
    }

    @Test
    fun `parse ignores the preamble and handles CRLF`() {
        val changelog = MermaidChangelog.parse("# Changelog\r\n\r\nIntro text\r\n\r\n## [2.0.0] - 2027-01-01\r\n\r\n- Big release\r\n")
        assertEquals(1, changelog.sections.size)
        assertEquals("- Big release", changelog.sections.single().body)
    }

    @Test
    fun `parse accepts a header without date and an unreleased section`() {
        val changelog = MermaidChangelog.parse("## [Unreleased]\n\n- Pending\n\n## [1.2.0]\n\n- Done\n")
        assertEquals(listOf("Unreleased", "1.2.0"), versions(changelog.sections))
        assertNull(changelog.sections[1].date)
        assertEquals("1.2.0", changelog.latest?.version, "Unreleased cannot be ordered and is never the latest")
    }

    @Test
    fun `parse of empty or header-less text yields no section`() {
        assertTrue(MermaidChangelog.parse("").sections.isEmpty())
        assertTrue(MermaidChangelog.parse("# Changelog\n\nNothing yet").sections.isEmpty())
        assertNull(MermaidChangelog.parse("").latest)
    }

    // --- Version ordering ---

    @Test
    fun `versions compare numerically`() {
        assertTrue(PluginVersion.parse("1.9.1")!! < PluginVersion.parse("1.10.0")!!)
        assertTrue(PluginVersion.parse("1.10.0")!! < PluginVersion.parse("1.10.1")!!)
        assertTrue(PluginVersion.parse("2.0.0")!! > PluginVersion.parse("1.99.99")!!)
    }

    @Test
    fun `missing components are zero and a v prefix is accepted`() {
        assertEquals(PluginVersion.parse("1.2"), PluginVersion.parse("1.2.0"))
        assertEquals(PluginVersion.parse("v1.2.0"), PluginVersion.parse("1.2.0"))
        assertEquals(PluginVersion.parse("1.2")!!.hashCode(), PluginVersion.parse("1.2.0")!!.hashCode())
    }

    @Test
    fun `pre-release sorts before the release`() {
        assertTrue(PluginVersion.parse("1.14.0-beta.1")!! < PluginVersion.parse("1.14.0")!!)
        assertTrue(PluginVersion.parse("1.14.0-beta.1")!! > PluginVersion.parse("1.13.9")!!)
    }

    @Test
    fun `invalid versions are not parsed`() {
        assertNull(PluginVersion.parse(null))
        assertNull(PluginVersion.parse(""))
        assertNull(PluginVersion.parse("Unreleased"))
        assertNull(PluginVersion.parse("1..2"))
        assertNull(PluginVersion.parse("99999999999.0"))
    }

    // --- sectionsBetween ---

    @Test
    fun `first install shows only the current version`() {
        assertEquals(listOf("1.9.1"), versions(synthetic.sectionsBetween(null, "1.9.1")))
    }

    @Test
    fun `update across several versions lists every newer section newest first`() {
        assertEquals(listOf("1.10.0", "1.9.1", "1.9.0"), versions(synthetic.sectionsBetween("1.8.0", "1.10.0")))
    }

    @Test
    fun `update to the next version lists only that version`() {
        assertEquals(listOf("1.10.0"), versions(synthetic.sectionsBetween("1.9.1", "1.10.0")))
    }

    @Test
    fun `update range excludes versions newer than the current one`() {
        assertEquals(listOf("1.9.1", "1.9.0"), versions(synthetic.sectionsBetween("1.8.0", "1.9.1")))
    }

    @Test
    fun `unknown current version falls back to the latest section`() {
        assertEquals(listOf("1.10.0"), versions(synthetic.sectionsBetween(null, "1.11.0-SNAPSHOT")))
        assertEquals(listOf("1.10.0"), versions(synthetic.sectionsBetween(null, "dev")))
        assertEquals(listOf("1.10.0"), versions(synthetic.sectionsBetween("1.10.0", "1.11.0")))
    }

    @Test
    fun `dev build newer than the changelog still lists the changes since the previous version`() {
        assertEquals(listOf("1.10.0", "1.9.1"), versions(synthetic.sectionsBetween("1.9.0", "1.11.0")))
    }

    @Test
    fun `downgrade shows the current version only`() {
        assertEquals(listOf("1.9.0"), versions(synthetic.sectionsBetween("1.10.0", "1.9.0")))
    }

    @Test
    fun `unparsable previous version is treated as a first install`() {
        assertEquals(listOf("1.9.1"), versions(synthetic.sectionsBetween("garbage", "1.9.1")))
    }

    @Test
    fun `empty changelog yields no section`() {
        assertTrue(MermaidChangelog(emptyList()).sectionsBetween("1.0.0", "1.1.0").isEmpty())
    }

    // --- Rendering ---

    @Test
    fun `single section renders its bullets and release date without heading`() {
        val markdown = renderChangelogSections(synthetic.sectionsBetween(null, "1.9.1"))
        assertFalse(markdown.contains("###"))
        assertTrue(markdown.contains("- Second bullet"))
        assertTrue(markdown.contains("_Released 2026-07-18._"))
    }

    @Test
    fun `several sections render one heading per version`() {
        val markdown = renderChangelogSections(synthetic.sectionsBetween("1.8.0", "1.10.0"))
        assertTrue(markdown.contains("### 1.10.0 (2026-07-24)"))
        assertTrue(markdown.contains("### 1.9.1 (2026-07-18)"))
        assertTrue(markdown.contains("### 1.9.0 (2026-07-06)"))
        assertTrue(markdown.indexOf("### 1.10.0") < markdown.indexOf("### 1.9.0"), "Newest first")
    }

    @Test
    fun `sub-headings are demoted below the version headings`() {
        val changelog = MermaidChangelog.parse("## [1.1.0]\n\n### Fixed\n- B\n\n## [1.0.0]\n\n### Added\n- A\n")
        val several = renderChangelogSections(changelog.sectionsBetween("0.9.0", "1.1.0"))
        assertTrue(several.contains("### 1.0.0\n\n#### Added\n- A"), several)
        val single = renderChangelogSections(changelog.sectionsBetween(null, "1.0.0"))
        assertTrue(single.startsWith("### Added\n- A"), single)
    }

    @Test
    fun `pull request numbers become links`() {
        val markdown = renderChangelogSections(synthetic.sectionsBetween(null, "1.10.0"))
        assertTrue(
            markdown.contains("([#24](https://github.com/Alex9583/MermaidVisualizer/pull/24))"),
            markdown,
        )
    }

    @Test
    fun `no section renders nothing`() {
        assertEquals("", renderChangelogSections(emptyList()))
    }
}
