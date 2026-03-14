package com.alextdev.mermaidvisualizer.editor

import com.alextdev.mermaidvisualizer.lang.MermaidFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

private const val EDITOR_TYPE_ID = "mermaid-split-editor"
private const val PREVIEW_TYPE_ID = "mermaid-preview"

private class MermaidPreviewEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == MermaidFileType
    override fun createEditor(project: Project, file: VirtualFile): FileEditor = MermaidPreviewFileEditor(project, file)
    override fun getEditorTypeId(): String = PREVIEW_TYPE_ID
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

internal class MermaidEditorProvider : TextEditorWithPreviewProvider(MermaidPreviewEditorProvider()) {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == MermaidFileType
    }

    override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor {
        return MermaidSplitEditor(firstEditor, secondEditor as MermaidPreviewFileEditor)
    }

    override fun getEditorTypeId(): String = EDITOR_TYPE_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
