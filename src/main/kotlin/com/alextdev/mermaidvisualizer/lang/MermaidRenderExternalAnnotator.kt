package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.MermaidRenderErrorCategory
import com.alextdev.mermaidvisualizer.editor.getMermaidRenderError
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Document
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace

/**
 * Surfaces Mermaid.js rendering errors as editor annotations.
 *
 * The error data is populated asynchronously by [com.alextdev.mermaidvisualizer.editor.MermaidPreviewFileEditor]
 * via a JCEF bridge callback and stored in [com.intellij.openapi.vfs.VirtualFile] UserData (only once the
 * "Error highlight delay" has elapsed, see [com.alextdev.mermaidvisualizer.editor.RenderErrorPublisher]).
 * When an error is published, [com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.restart] is called
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
        val range = computeRenderErrorRange(document, annotationResult) { offset -> tokenEndAt(file, offset) }
        val shortcut = KeymapUtil.getFirstKeyboardShortcutText(IdeActions.ACTION_SHOW_INTENTION_ACTIONS)
        val enriched = MermaidRenderErrorEnricher.enrich(annotationResult, shortcut.ifEmpty { null })

        holder.newAnnotation(renderErrorSeverity(annotationResult.category), enriched.message)
            .range(range)
            .tooltip(enriched.tooltip)
            .withFix(MermaidCopyRenderErrorIntention(annotationResult))
            .withFix(MermaidReportRenderErrorIntention(annotationResult))
            .create()
    }

    /** End offset of the (non-whitespace) token at [offset], or null. */
    private fun tokenEndAt(file: PsiFile, offset: Int): Int? {
        val leaf = file.findElementAt(offset) ?: return null
        if (leaf is PsiWhiteSpace) return null
        return leaf.textRange.endOffset
    }
}

/**
 * An unfinished line is the normal state of a diagram being typed: it gets a discreet weak warning.
 * Every other render error is a warning.
 */
internal fun renderErrorSeverity(category: MermaidRenderErrorCategory): HighlightSeverity = when (category) {
    MermaidRenderErrorCategory.INCOMPLETE -> HighlightSeverity.WEAK_WARNING
    MermaidRenderErrorCategory.SYNTAX,
    MermaidRenderErrorCategory.UNKNOWN_DIAGRAM,
    MermaidRenderErrorCategory.LIMIT,
    MermaidRenderErrorCategory.GENERIC -> HighlightSeverity.WARNING
}

/**
 * Range annotated for [error]:
 * - line and column inside the line: from the column to the end of the token starting there
 *   ([tokenEndAt], clamped to the line; a single character when it returns null);
 * - line only (or a column past the end of the line): the whole line;
 * - no usable line: the first line (diagram type declaration).
 */
internal fun computeRenderErrorRange(
    document: Document,
    error: MermaidRenderError,
    tokenEndAt: (offset: Int) -> Int? = { null },
): TextRange {
    val errorLine = error.line
    if (errorLine != null && errorLine <= document.lineCount) {
        val lineIndex = errorLine - 1
        val startOffset = document.getLineStartOffset(lineIndex)
        val endOffset = document.getLineEndOffset(lineIndex)
        val column = error.column
        if (column != null && column - 1 < endOffset - startOffset) {
            val columnOffset = startOffset + column - 1
            val tokenEnd = tokenEndAt(columnOffset)?.coerceIn(columnOffset + 1, endOffset) ?: (columnOffset + 1)
            return TextRange(columnOffset, tokenEnd)
        }
        return TextRange(startOffset, endOffset)
    }
    // Fallback: annotate the first line (diagram type declaration)
    val endOffset = if (document.lineCount > 0) document.getLineEndOffset(0) else document.textLength
    return TextRange(0, endOffset.coerceIn(0, document.textLength))
}
