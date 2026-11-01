package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

/**
 * Kind of a Mermaid.js render error, as classified by `classifyRenderError` in `web/mermaid-core-errors.js`
 * (payload field `category`). Drives the annotation severity and the tooltip content.
 */
enum class MermaidRenderErrorCategory(val payloadValue: String) {
    /** The input stops in the middle of a construct: the user is most likely still typing the line. */
    INCOMPLETE("incomplete"),
    SYNTAX("syntax"),
    UNKNOWN_DIAGRAM("unknown-diagram"),
    /** "Max text size" / "Max edges" exceeded: not a syntax problem. */
    LIMIT("limit"),
    GENERIC("generic");

    companion object {
        /** Maps a payload `category` string; unknown or missing values become [GENERIC]. */
        fun fromPayload(value: String?): MermaidRenderErrorCategory =
            entries.firstOrNull { it.payloadValue == value } ?: GENERIC
    }
}

/**
 * A Mermaid.js render error reported by the standalone preview.
 *
 * - [message]: the raw Mermaid.js message (its "line N" may differ from [line]: it is relative to
 *   Mermaid's preprocessed text).
 * - [line]/[column]: 1-based position in the document (front matter, `%%` comments and trimmed leading
 *   blank lines already accounted for by the JS side), or null when unknown.
 * - [summary]: one-line human description built by the JS side, or null for payloads without one.
 */
data class MermaidRenderError(
    val message: String,
    val line: Int?,
    val column: Int?,
    val summary: String? = null,
    val category: MermaidRenderErrorCategory = MermaidRenderErrorCategory.GENERIC,
) {
    init {
        require(line == null || line >= 1) { "line must be >= 1, got $line" }
        require(column == null || column >= 1) { "column must be >= 1, got $column" }
    }
}

val MERMAID_RENDER_ERROR_KEY: Key<MermaidRenderError> = Key.create("mermaid.render.error")

fun VirtualFile.setMermaidRenderError(error: MermaidRenderError?) {
    putUserData(MERMAID_RENDER_ERROR_KEY, error)
}

fun VirtualFile.getMermaidRenderError(): MermaidRenderError? {
    return getUserData(MERMAID_RENDER_ERROR_KEY)
}
