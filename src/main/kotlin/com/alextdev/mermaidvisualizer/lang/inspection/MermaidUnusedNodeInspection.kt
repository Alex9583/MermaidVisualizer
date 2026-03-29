package com.alextdev.mermaidvisualizer.lang.inspection

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.inspection.fix.MermaidRemoveDeclarationFix
import com.alextdev.mermaidvisualizer.lang.psi.MermaidClassDiagram
import com.alextdev.mermaidvisualizer.lang.MermaidFile
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.alextdev.mermaidvisualizer.lang.psi.MermaidSequenceDiagram
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Reports node declarations that are never used in relationships or messages.
 *
 * - **Sequence**: `participant X` or `actor X` declared but X never appears in any message.
 * - **Class**: `class X` declared but X never appears in any relationship.
 */
class MermaidUnusedNodeInspection : LocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file !is MermaidFile) return null
        val problems = mutableListOf<ProblemDescriptor>()

        PsiTreeUtil.findChildrenOfType(file, MermaidSequenceDiagram::class.java).forEach { seq ->
            checkDeclaredVsUsed(seq.diagramBody, MermaidDiagramKind.SEQUENCE, manager, isOnTheFly, problems)
        }
        PsiTreeUtil.findChildrenOfType(file, MermaidClassDiagram::class.java).forEach { cls ->
            checkDeclaredVsUsed(cls.diagramBody, MermaidDiagramKind.CLASS, manager, isOnTheFly, problems)
        }

        return problems.toTypedArray().ifEmpty { null }
    }

    private fun checkDeclaredVsUsed(
        body: com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagramBody?,
        kind: MermaidDiagramKind,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        problems: MutableList<ProblemDescriptor>,
    ) {
        if (body == null) return
        val declared = MermaidPsiUtil.collectDeclaredNodes(body, kind)
        if (declared.isEmpty()) return

        val usedNames = MermaidPsiUtil.collectUsedIdentifiers(body, kind)
            .map { it.name }
            .toSet()

        for (node in declared) {
            if (node.name !in usedNames) {
                problems.add(
                    manager.createProblemDescriptor(
                        node.element,
                        MyMessageBundle.message("inspection.unused.node", node.name, node.keyword),
                        MermaidRemoveDeclarationFix(node.keyword, node.name),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        isOnTheFly,
                    )
                )
            }
        }
    }

}
