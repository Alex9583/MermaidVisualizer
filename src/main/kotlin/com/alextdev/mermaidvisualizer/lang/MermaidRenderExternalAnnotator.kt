package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.getMermaidRenderError
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

/**
 * Surfaces Mermaid.js rendering errors as editor annotations.
 *
 * The error data is populated asynchronously by [com.alextdev.mermaidvisualizer.editor.MermaidPreviewFileEditor]
 * via a JCEF bridge callback and stored in [com.intellij.openapi.vfs.VirtualFile] UserData.
 * When an error arrives, [com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.restart] is called
 * to trigger this annotator.
 */
class MermaidRenderExternalAnnotator : ExternalAnnotator<MermaidRenderError?, MermaidRenderError?>() {

    override fun collectInformation(file: PsiFile): MermaidRenderError? {
        return file.virtualFile?.getMermaidRenderError()
    }

    override fun doAnnotate(collectedInfo: MermaidRenderError?): MermaidRenderError? {
        return collectedInfo
    }

    override fun apply(file: PsiFile, annotationResult: MermaidRenderError?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        val document = file.viewProvider.document ?: return
        val range = computeAnnotationRange(document, annotationResult)
        val enriched = MermaidRenderErrorEnricher.enrich(annotationResult)

        holder.newAnnotation(HighlightSeverity.WARNING, enriched.message)
            .range(range)
            .tooltip(enriched.tooltip)
            .create()
    }

    private fun computeAnnotationRange(document: Document, error: MermaidRenderError): TextRange {
        val errorLine = error.line
        if (errorLine != null && errorLine >= 1 && errorLine <= document.lineCount) {
            val lineIndex = errorLine - 1
            val startOffset = document.getLineStartOffset(lineIndex)
            val endOffset = document.getLineEndOffset(lineIndex)
            if (error.column != null && error.column >= 1) {
                val colOffset = startOffset + error.column - 1
                if (colOffset < endOffset) {
                    return TextRange(colOffset, (colOffset + 1).coerceAtMost(endOffset))
                }
            }
            return TextRange(startOffset, endOffset)
        }
        // Fallback: annotate the first line (diagram type declaration)
        val endOffset = if (document.lineCount > 0) document.getLineEndOffset(0) else document.textLength
        return TextRange(0, endOffset.coerceAtLeast(0))
    }
}
