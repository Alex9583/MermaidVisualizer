package com.alextdev.mermaidvisualizer.lang.inspection

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidReplaceArrowFix
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import kotlin.math.abs
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType

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
                    if (child.node.elementType == MermaidTokenTypes.ARROW && !isInsideText(child) && !isTrailingDots(child)) {
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

/**
 * Arrows inside node labels (`A[x -> y]`), pipe-delimited edge text (`-->|a -> b|`) or after a colon
 * (message text, class members) are plain text and must not be validated.
 */
private fun isInsideText(arrow: PsiElement): Boolean =
    MermaidPsiUtil.isInsideBrackets(arrow) ||
        MermaidPsiUtil.isInsidePipes(arrow) ||
        MermaidPsiUtil.isAfterColon(arrow)

/**
 * A dot-only ARROW (`..`) glued to the identifier before it (`Loading... --> Done`) is the tail of
 * a node id, not a link. Only whitespace-separated `..` is validated (`A .. B`).
 */
private fun isTrailingDots(arrow: PsiElement): Boolean {
    if (!arrow.text.all { it == '.' }) return false
    val prev = arrow.prevSibling ?: return false
    return prev.node.elementType != TokenType.WHITE_SPACE && prev.firstChild?.node?.elementType == MermaidTokenTypes.IDENTIFIER
}

/**
 * Pieces of the flowchart "link with text" syntax (`A -- text --> B`, `A -. text .-> B`,
 * `A == text ==> B`) and of open links (`A --- B`, `A === B`). They are lexed as separate ARROW
 * tokens but are not arrows on their own, so they are not in the completion catalog.
 */
private val FLOWCHART_LINK_FRAGMENTS = setOf("--", "---", "==", "===", "-.", "-.-", ".->")

/**
 * Diagram kinds using the flowchart link grammar: variable-length arrows (`---->`), `-- text -->`
 * fragments and markerless links. Use case (`Customer -- "places order" ---> Checkout`) and
 * agentflow (`check -- yes --> ship`) share it since Mermaid 12.
 */
private val FLOWCHART_LINK_KINDS = setOf(
    MermaidDiagramKind.FLOWCHART, MermaidDiagramKind.GRAPH, MermaidDiagramKind.SWIMLANE,
    MermaidDiagramKind.USECASE, MermaidDiagramKind.AGENTFLOW,
)

private fun isArrowValidForKind(
    arrowText: String,
    kind: MermaidDiagramKind,
    validArrows: Set<String>,
): Boolean {
    if (arrowText in validArrows) return true

    // Activation shorthand in sequence diagrams: `->>+`, `-->>-`, `->+`, `-->-`
    if (kind == MermaidDiagramKind.SEQUENCE && arrowText.length > 2 &&
        (arrowText.endsWith('+') || arrowText.endsWith('-'))
    ) {
        if (arrowText.dropLast(1) in validArrows) return true
    }

    // Handle variable-length arrows and link fragments in the flowchart family (e.g., ----> normalizes to --->)
    if (kind in FLOWCHART_LINK_KINDS) {
        val normalized = arrowText
            .replace(Regex("-{3,}"), "---")
            .replace(Regex("={3,}"), "===")
        if (normalized in validArrows || normalized in FLOWCHART_LINK_FRAGMENTS) return true
        // Handle long bidirectional: <-----> → <-->
        val normalizedBidi = arrowText.replace(Regex("<-{2,}>"), "<-->")
        if (normalizedBidi in validArrows) return true
    }
    return false
}
