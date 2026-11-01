package com.alextdev.mermaidvisualizer.welcome

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException

private val LOG = Logger.getInstance("MermaidChangelog")

/** Root `CHANGELOG.md`, copied into the plugin resources by the `processResources` task of `build.gradle.kts`. */
internal const val CHANGELOG_RESOURCE = "welcome/CHANGELOG.md"

/** `## [1.13.1] - 2026-10-18` (the date part is optional). */
private val SECTION_HEADER = Regex("""^##\s*\[([^\]]+)]\s*(?:-\s*(\S+))?\s*$""")
private val VERSION_PATTERN = Regex("""^v?(\d+(?:\.\d+)*)(?:-([0-9A-Za-z.-]+))?$""")

/** `(#33)` at the end of a changelog bullet: the auto-release workflow appends the pull request number. */
private val PR_REFERENCE = Regex("""\(#(\d+)\)""")
private const val PULL_REQUEST_URL = "https://github.com/Alex9583/MermaidVisualizer/pull/"

/** `### Added` inside a section body (levels 3 to 5, so that the demoted heading stays valid). */
private val SUB_HEADING = Regex("""^#{3,5}(?=\s)""", RegexOption.MULTILINE)

/** One `## [x.y.z] - date` block of the changelog; [body] holds its lines (bullets), trimmed of blank edges. */
internal data class ChangelogSection(val version: String, val date: String?, val body: String)

/**
 * Semantic-ish version: numeric components compared numerically (`1.9.1 < 1.10.0`), missing components are 0
 * (`1.2 == 1.2.0`), and a pre-release suffix sorts before the release (`1.14.0-beta < 1.14.0`).
 */
internal class PluginVersion(private val parts: List<Int>, private val preRelease: String?) : Comparable<PluginVersion> {

    override fun compareTo(other: PluginVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return when {
            preRelease == other.preRelease -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> preRelease.compareTo(other.preRelease)
        }
    }

    override fun equals(other: Any?): Boolean = other is PluginVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode() * 31 + (preRelease?.hashCode() ?: 0)

    override fun toString(): String = parts.joinToString(".") + (preRelease?.let { "-$it" } ?: "")

    companion object {
        /** Parses `1.13.1`, `v1.13.1`, `1.14.0-beta.1`; returns null for anything else (`Unreleased`). */
        fun parse(text: String?): PluginVersion? {
            val match = VERSION_PATTERN.matchEntire(text?.trim().orEmpty()) ?: return null
            val parts = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
            return PluginVersion(parts, match.groups[2]?.value)
        }
    }
}

/** The parsed changelog: sections in file order (newest first in this repository). */
internal class MermaidChangelog(val sections: List<ChangelogSection>) {

    private val versioned: List<Pair<PluginVersion, ChangelogSection>> = sections
        .mapNotNull { section -> PluginVersion.parse(section.version)?.let { it to section } }
        .sortedByDescending { it.first }

    /** Newest section by version (not by file position), or null when no section has a valid version. */
    val latest: ChangelogSection?
        get() = versioned.firstOrNull()?.second

    fun sectionFor(version: String?): ChangelogSection? {
        val wanted = PluginVersion.parse(version) ?: return null
        return versioned.firstOrNull { it.first == wanted }?.second
    }

    /**
     * Sections to show after an update from [previousShown] to [current], newest first:
     * - first install ([previousShown] null or unparsable): the [current] section only;
     * - update: every section with `previousShown < version <= current`;
     * - downgrade, same version or nothing in the range: the [current] section only;
     * - when [current] has no section (dev build, unparsable version): the [latest] section.
     */
    fun sectionsBetween(previousShown: String?, current: String): List<ChangelogSection> {
        val currentVersion = PluginVersion.parse(current)
        val fallback = listOfNotNull(sectionFor(current) ?: latest)
        if (currentVersion == null) return fallback
        val previousVersion = PluginVersion.parse(previousShown) ?: return fallback
        val range = versioned
            .filter { (version, _) -> version > previousVersion && version <= currentVersion }
            .map { it.second }
        return range.ifEmpty { fallback }
    }

    companion object {
        fun parse(markdown: String): MermaidChangelog {
            val sections = mutableListOf<ChangelogSection>()
            var header: MatchResult? = null
            val body = mutableListOf<String>()

            fun flush() {
                val current = header ?: return
                sections += ChangelogSection(
                    version = current.groupValues[1].trim(),
                    date = current.groups[2]?.value,
                    body = body.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }.joinToString("\n"),
                )
                body.clear()
            }

            for (line in markdown.replace("\r\n", "\n").lines()) {
                val match = SECTION_HEADER.matchEntire(line.trimEnd())
                when {
                    match != null -> {
                        flush()
                        header = match
                    }
                    // A level-1/2 heading that is not a version closes the current section (e.g. a footer).
                    line.startsWith("# ") || line.startsWith("## ") -> {
                        flush()
                        header = null
                    }
                    header != null -> body += line.trimEnd()
                }
            }
            flush()
            return MermaidChangelog(sections)
        }

        /** Loads the changelog embedded at build time; an empty changelog when the resource is missing. */
        fun loadBundled(): MermaidChangelog {
            val text = try {
                MermaidChangelog::class.java.classLoader.getResourceAsStream(CHANGELOG_RESOURCE)
                    ?.use { it.reader(Charsets.UTF_8).readText() }
            } catch (e: IOException) {
                LOG.warn("Failed to read $CHANGELOG_RESOURCE", e)
                null
            }
            if (text == null) {
                LOG.warn("$CHANGELOG_RESOURCE is missing from the plugin resources: the welcome page has no release notes")
                return MermaidChangelog(emptyList())
            }
            return parse(text)
        }
    }
}

/**
 * Renders sections back to Markdown for the welcome page (under its `## What's new` heading). Several sections
 * get a `### version` heading each, their own sub-headings (`### Added`) being demoted one level; a single one
 * is rendered as its bullets plus its release date. `(#N)` becomes a link to the pull request.
 */
internal fun renderChangelogSections(sections: List<ChangelogSection>): String {
    if (sections.isEmpty()) return ""
    return sections.joinToString("\n\n") { section ->
        val body = linkPullRequests(section.body).ifBlank { "- No details for this version." }
        if (sections.size == 1) {
            if (section.date != null) "$body\n\n_Released ${section.date}._" else body
        } else {
            val title = if (section.date != null) "${section.version} (${section.date})" else section.version
            "### $title\n\n${demoteSubHeadings(body)}"
        }
    }
}

private fun demoteSubHeadings(body: String): String =
    SUB_HEADING.replace(body) { match -> "#" + match.value }

private fun linkPullRequests(text: String): String =
    PR_REFERENCE.replace(text) { match -> "([#${match.groupValues[1]}]($PULL_REQUEST_URL${match.groupValues[1]}))" }
