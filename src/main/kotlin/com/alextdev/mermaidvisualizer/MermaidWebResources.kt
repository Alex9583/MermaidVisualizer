package com.alextdev.mermaidvisualizer

/**
 * Web resources (`src/main/resources/web`) and the script load order shared by `MermaidPreviewPanel` and
 * `MermaidBrowserExtension`. The Node tests keep a copy of [CORE_SCRIPTS] (`mermaid-core-sandbox.mjs`).
 */
internal object MermaidWebResources {
    const val RESOURCE_DIR = "web"

    const val MERMAID_JS = "mermaid.min.js"
    const val MERMAID_VERSION = "mermaid.version"
    const val ZOOM_JS = "mermaid-zoom.js"
    const val RENDER_JS = "mermaid-render.js"
    const val STANDALONE_JS = "mermaid-standalone.js"
    const val SHADOW_CSS = "mermaid-shadow.css"
    const val PREVIEW_CSS = "mermaid-preview.css"
    const val STANDALONE_CSS = "mermaid-standalone.css"

    /** Markdown preview only: generated at runtime, never read from the classpath. */
    const val SHADOW_CSS_INIT_JS = "mermaid-shadow-css-init.js"
    const val CONFIG_INIT_JS = "mermaid-config-init.js"

    /** The module registry first, then the modules in dependency order (a wrong order fails at page load). */
    val CORE_SCRIPTS: List<String> = listOf(
        "mermaid-core.js",
        "mermaid-core-text.js",
        "mermaid-core-shadow.js",
        "mermaid-core-runtime.js",
        "mermaid-core-export.js",
        "mermaid-core-error-positions.js",
        "mermaid-core-error-labels.js",
        "mermaid-core-errors.js",
        "mermaid-core-banner.js",
        "mermaid-core-pipeline.js",
    )

    /** Standalone editor scripts after `mermaid.min.js`, in load order. */
    val STANDALONE_SCRIPTS: List<String> = CORE_SCRIPTS + ZOOM_JS + STANDALONE_JS

    /** Markdown preview plugin scripts after `mermaid.min.js` and the two generated init scripts, in load order. */
    val MARKDOWN_SCRIPTS: List<String> = CORE_SCRIPTS + ZOOM_JS + RENDER_JS

    val MARKDOWN_RESOURCE_NAMES: Set<String> =
        (listOf(MERMAID_JS, SHADOW_CSS_INIT_JS, CONFIG_INIT_JS, PREVIEW_CSS, SHADOW_CSS) + MARKDOWN_SCRIPTS).toSet()

    fun path(name: String): String = "$RESOURCE_DIR/$name"
}
