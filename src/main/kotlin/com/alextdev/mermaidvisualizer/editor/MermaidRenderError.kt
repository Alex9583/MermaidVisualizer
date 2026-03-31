package com.alextdev.mermaidvisualizer.editor

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

data class MermaidRenderError(
    val message: String,
    val line: Int?,
    val column: Int?,
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
