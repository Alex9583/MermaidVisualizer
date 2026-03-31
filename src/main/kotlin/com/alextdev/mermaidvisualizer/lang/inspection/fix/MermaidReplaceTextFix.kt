package com.alextdev.mermaidvisualizer.lang.inspection.fix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/**
 * Base quick fix that replaces the text of the highlighted PSI element with [replacement].
 */
abstract class MermaidReplaceTextFix(
    protected val replacement: String,
) : LocalQuickFix {

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val range = element.textRange

        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(range.startOffset, range.endOffset, replacement)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}