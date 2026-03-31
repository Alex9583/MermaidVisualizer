package com.alextdev.mermaidvisualizer.lang.inspection

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidReplaceArrowFix
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import kotlin.math.abs
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor

/**
 * Reports arrow tokens that are not valid for the current diagram type.
 *
 * For example, `->>` (sequence async arrow) is not valid in a flowchart diagram.
 * Only validates diagrams that have defined arrow sets in [MermaidCompletionData.arrowsFor].
 */
class MermaidInvalidArrowInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element !is MermaidStatement) return
                val kind = MermaidCompletionData.detectDiagramKind(element) ?: return
                val arrows = nonLabeledArrows(kind)
                if (arrows.isEmpty()) return
                val validArrows = arrows.toSet()

                var child = element.firstChild
                while (child != null) {
                    if (child.node.elementType == MermaidTokenTypes.ARROW) {
                        val arrowText = child.text
                        if (!isArrowValidForKind(arrowText, kind, validArrows)) {
                            val kindName = kind.keyword
                            val validSummary = arrows.joinToString(", ")
                            val fixes = arrows
                                .sortedWith(compareBy(
                                    { MermaidSuggestDiagramTypeFix.editDistance(arrowText, it) },
                                    { abs(it.length - arrowText.length) },
                                ))
                                .take(3)
                                .map { MermaidReplaceArrowFix(it) }
                                .toTypedArray()

                            holder.registerProblem(
                                child,
                                MyMessageBundle.message("inspection.invalid.arrow", arrowText, kindName, validSummary),
                                ProblemHighlightType.WARNING,
                                *fixes,
                            )
                        }
                    }
                    child = child.nextSibling
                }
            }
        }
    }
}

/**
 * Regex matching labeled arrow templates like `-->|text|` where `|text|` is at the END.
 * The lexer tokenizes these as separate tokens (ARROW + PIPE + IDENTIFIER + PIPE),
 * so the template must not be in the valid arrow set for inspection.
 * End-anchored to avoid matching ER cardinality markers like `||--|{` or `}|--||`.
 */
private val LABELED_ARROW_TEMPLATE = Regex("\\|[^|]+\\|$")

private fun nonLabeledArrows(kind: MermaidDiagramKind): List<String> {
    return MermaidCompletionData.arrowsFor(kind)
        .map { it.arrow }
        .filter { !LABELED_ARROW_TEMPLATE.containsMatchIn(it) }
}

private fun isArrowValidForKind(
    arrowText: String,
    kind: MermaidDiagramKind,
    validArrows: Set<String>,
): Boolean {
    if (arrowText in validArrows) return true

    // Handle variable-length arrows in flowchart/graph (e.g., ----> normalizes to --->)
    if (kind == MermaidDiagramKind.FLOWCHART || kind == MermaidDiagramKind.GRAPH) {
        val normalized = arrowText
            .replace(Regex("-{3,}"), "---")
            .replace(Regex("={3,}"), "===")
        if (normalized in validArrows) return true
        // Handle long bidirectional: <-----> → <-->
        val normalizedBidi = arrowText.replace(Regex("<-{2,}>"), "<-->")
        if (normalizedBidi in validArrows) return true
    }
    return false
}
