package com.alextdev.mermaidvisualizer.welcome

import com.alextdev.mermaidvisualizer.MermaidWebResources
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException

private val LOG = Logger.getInstance("MermaidWelcomePage")

internal const val WELCOME_TEMPLATE_RESOURCE = "welcome/welcome.md"
private const val MERMAID_VERSION_RESOURCE = "${MermaidWebResources.RESOURCE_DIR}/${MermaidWebResources.MERMAID_VERSION}"

internal const val PLACEHOLDER_VERSION = "{{VERSION}}"
internal const val PLACEHOLDER_MERMAID_VERSION = "{{MERMAID_VERSION}}"
internal const val PLACEHOLDER_WHATS_NEW_SCOPE = "{{WHATS_NEW_SCOPE}}"
internal const val PLACEHOLDER_WHATS_NEW = "{{WHATS_NEW}}"

/** Used only if the bundled template is missing (broken build): keeps the page meaningful. */
private const val FALLBACK_TEMPLATE = """# Mermaid Visualizer $PLACEHOLDER_VERSION

## What's new $PLACEHOLDER_WHATS_NEW_SCOPE

$PLACEHOLDER_WHATS_NEW

Support the project: [Buy Me a Coffee](https://buymeacoffee.com/alextdev) · [GitHub](https://github.com/Alex9583/MermaidVisualizer) · [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30432-mermaid-visualizer/reviews)
"""

private const val NO_RELEASE_NOTES = "_No release notes are bundled with this build._"

/**
 * Fills the welcome page [template]. The page content is English Markdown (like the GitHub error report),
 * not bundle keys. [WHATS_NEW][PLACEHOLDER_WHATS_NEW] is replaced last so changelog text is never re-expanded.
 */
internal fun buildWelcomePage(
    template: String,
    version: String,
    mermaidVersion: String?,
    sections: List<ChangelogSection>,
    previousShown: String?,
): String {
    val whatsNew = renderChangelogSections(sections).ifEmpty { NO_RELEASE_NOTES }
    return template
        .replace(PLACEHOLDER_VERSION, version)
        .replace(PLACEHOLDER_MERMAID_VERSION, mermaidVersion?.let { "v$it" } ?: "bundled")
        .replace(PLACEHOLDER_WHATS_NEW_SCOPE, whatsNewScope(sections, version, previousShown))
        .replace(PLACEHOLDER_WHATS_NEW, whatsNew)
}

/** `since 1.12.0` when several versions are listed, otherwise `in <the version of the section shown>`. */
internal fun whatsNewScope(sections: List<ChangelogSection>, version: String, previousShown: String?): String = when {
    sections.size > 1 && previousShown != null -> "since $previousShown"
    sections.size == 1 -> "in ${sections.single().version}"
    else -> "in $version".trimEnd()
}

/**
 * The welcome page for [version] from the bundled template and changelog: the changes since [previousShown]
 * (null = first install or manual request = the current version only).
 */
internal fun loadWelcomePage(version: String, previousShown: String?): String {
    val changelog = MermaidChangelog.loadBundled()
    val template = readResource(WELCOME_TEMPLATE_RESOURCE) ?: run {
        LOG.warn("$WELCOME_TEMPLATE_RESOURCE is missing from the plugin resources, using a minimal page")
        FALLBACK_TEMPLATE
    }
    val mermaidVersion = readResource(MERMAID_VERSION_RESOURCE)?.trim()?.ifEmpty { null }
    return buildWelcomePage(template, version, mermaidVersion, changelog.sectionsBetween(previousShown, version), previousShown)
}

private fun readResource(path: String): String? = try {
    MermaidChangelog::class.java.classLoader.getResourceAsStream(path)?.use { it.reader(Charsets.UTF_8).readText() }
} catch (e: IOException) {
    LOG.warn("Failed to read $path", e)
    null
}
