package com.alextdev.mermaidvisualizer.lang.inspection.fix

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil

/**
 * Quick fix that removes an unused declaration statement (e.g., `participant X` or `class X`).
 */
class MermaidRemoveDeclarationFix(
    private val keyword: String,
    private val nodeName: String,
) : LocalQuickFix {

    override fun getFamilyName(): String =
        MyMessageBundle.message("inspection.fix.remove.declaration.family")

    override fun getName(): String =
        MyMessageBundle.message("inspection.fix.remove.declaration", keyword, nodeName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val stmt = PsiTreeUtil.getParentOfType(element, MermaidStatement::class.java, false)
            ?: return
        val file = stmt.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val fileText = document.text

        // Delete the full line containing this statement (including leading whitespace and trailing newline)
        val stmtStart = stmt.textRange.startOffset
        val stmtEnd = stmt.textRange.endOffset
        val lineStart = fileText.lastIndexOf('\n', stmtStart - 1).let { if (it < 0) 0 else it }
        val lineEnd = fileText.indexOf('\n', stmtEnd).let { if (it < 0) fileText.length else it + 1 }

        // Avoid deleting beyond the file if this is the last line
        val deleteStart = if (lineStart == 0 && fileText.getOrNull(0) != '\n') 0 else lineStart
        val deleteEnd = lineEnd.coerceAtMost(fileText.length)

        WriteCommandAction.runWriteCommandAction(project) {
            document.deleteString(deleteStart, deleteEnd)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}
