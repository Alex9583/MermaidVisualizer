package com.alextdev.mermaidvisualizer.lang.inspection

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidSuggestDiagramTypeFix
import com.alextdev.mermaidvisualizer.lang.psi.MermaidGenericDiagram
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor

/**
 * Reports diagram type keywords that do not match any known Mermaid diagram type.
 *
 * Only inspects [MermaidGenericDiagram] nodes — typed diagrams (FlowchartDiagram,
 * SequenceDiagram, etc.) are only created when the parser matches a known type,
 * so they are inherently valid. However, [MermaidGenericDiagram] also holds valid
 * generic types (gantt, pie, mindmap, etc.) — only types not matching any
 * [MermaidDiagramKind.keyword] are flagged.
 */
class MermaidUnknownDiagramTypeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PsiElementVisitor() {
            override fun visitElement(element: com.intellij.psi.PsiElement) {
                if (element !is MermaidGenericDiagram) return
                val typeNode = element.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE) ?: return
                val typeText = typeNode.text
                if (KNOWN_KEYWORDS.contains(typeText)) return

                val typeElement = typeNode.psi
                val suggestions = MermaidSuggestDiagramTypeFix.suggestClosest(typeText)
                val fixes = suggestions
                    .map { MermaidSuggestDiagramTypeFix(typeText, it) }
                    .toTypedArray()

                val message = if (suggestions.isNotEmpty()) {
                    MyMessageBundle.message(
                        "inspection.unknown.diagram.type",
                        typeText,
                        suggestions.joinToString(", "),
                    )
                } else {
                    MyMessageBundle.message("inspection.unknown.diagram.type.no.suggestion", typeText)
                }

                holder.registerProblem(
                    typeElement,
                    message,
                    ProblemHighlightType.ERROR,
                    *fixes,
                )
            }
        }
    }
}

private val KNOWN_KEYWORDS: Set<String> = MermaidDiagramKind.entries.map { it.keyword }.toSet()
