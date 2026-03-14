package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview

internal class MermaidSplitEditor(
    editor: TextEditor,
    preview: MermaidPreviewFileEditor,
) : TextEditorWithPreview(
    editor,
    preview,
    MyMessageBundle.message("editor.name"),
    Layout.SHOW_EDITOR_AND_PREVIEW,
) {
    init {
        preview.attachEditor(editor.editor)
    }
}
