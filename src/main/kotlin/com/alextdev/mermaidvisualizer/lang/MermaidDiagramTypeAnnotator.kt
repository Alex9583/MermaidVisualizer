package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.util.text.StringUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile

/**
 * Provides user-friendly error messages and quick-fixes for misspelled diagram types.
 *
 * The Grammar-Kit parser produces generic error messages like
 * "COMMENT, DIAGRAM_TYPE or DIRECTIVE expected, got 'flowchar'" which are cryptic.
 * This annotator detects identifiers at the file level that look like misspelled
 * diagram types and replaces the error with a clearer message + quick fix.
 */
class MermaidDiagramTypeAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiErrorElement) return
        // Only process file-level errors (direct child of MermaidFile)
        if (element.parent !is MermaidFile) return

        // Look for an IDENTIFIER token inside the error element that could be a misspelled diagram type
        val identifierChild = element.firstChild ?: return
        if (identifierChild.node.elementType != MermaidTokenTypes.IDENTIFIER) return

        val text = identifierChild.text
        if (text.isBlank()) return

        val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest(text, 3)
        if (suggestions.isEmpty()) return

        // Only suggest if the closest match is reasonably close (Levenshtein ≤ half the length + 2)
        val bestMatch = suggestions.first()
        val distance = MermaidSuggestDiagramTypeFix.editDistance(text.lowercase(), bestMatch.lowercase())
        if (distance > (text.length / 2) + 2) return

        val targetRange = identifierChild.textRange
        val message = MyMessageBundle.message("annotator.unknown.diagram.type", text, bestMatch)
        val tooltip = buildDiagramTypeTooltip(text, suggestions)
        val builder = holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(identifierChild)
            .tooltip(tooltip)
        for (suggestion in suggestions) {
            builder.withFix(ReplaceDiagramTypeFix(text, suggestion, targetRange))
        }
        builder.create()
    }
}

private fun buildDiagramTypeTooltip(wrongType: String, suggestions: List<String>): String = buildString {
    val escapedType = StringUtil.escapeXmlEntities(wrongType)
    append("<html><body>")
    append("<b>").append(MyMessageBundle.message("tooltip.unknown.diagram.type", escapedType)).append("</b><br>")
    append("<b>").append(MyMessageBundle.message("tooltip.did.you.mean")).append("</b> ")
    append(suggestions.joinToString(", ") { "<code>$it</code>" })
    append("<br><br>")
    append("<b>").append(MyMessageBundle.message("tooltip.diagram.types.header")).append("</b><br>")
    val popular = MermaidCompletionData.POPULAR_TYPES.map { it.keyword }
    val others = MermaidDiagramKind.entries
        .map { it.keyword }
        .distinct()
        .filter { it !in popular }
    append(popular.joinToString(", ") { "<b><code>$it</code></b>" })
    append(", ")
    append(others.joinToString(", ") { "<code>$it</code>" })
    append("</body></html>")
}

/**
 * IntentionAction that replaces a misspelled diagram type with the correct one.
 * Stores the exact [targetRange] to replace, avoiding caret-position ambiguity.
 */
private class ReplaceDiagramTypeFix(
    private val wrongType: String,
    private val suggestion: String,
    private val targetRange: TextRange,
) : IntentionAction {

    override fun getText(): String =
        MyMessageBundle.message("inspection.fix.replace.diagram.type", wrongType, suggestion)

    override fun getFamilyName(): String =
        MyMessageBundle.message("inspection.fix.replace.diagram.type.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true
    override fun startInWriteAction(): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (file == null) return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        // Validate the stored range against the current document state to handle edits between
        // annotation creation and fix invocation
        if (targetRange.endOffset > document.textLength) return
        if (document.getText(targetRange) != wrongType) return
        document.replaceString(targetRange.startOffset, targetRange.endOffset, suggestion)
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }
}

