package com.alextdev.mermaidvisualizer.lang.inspection.fix

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/**
 * Quick fix that removes an unused declaration statement (e.g., `participant X` or `class X`).
 *
 * Deletes the entire line containing [declarationStartOffset], because declaration keywords
 * and the node name always reside on the same line, and the Grammar-Kit parser creates
 * a single MermaidStatement per diagram body.
 */
class MermaidRemoveDeclarationFix(
    private val keyword: String,
    private val nodeName: String,
    private val declarationStartOffset: Int,
) : LocalQuickFix {

    override fun getFamilyName(): String =
        MyMessageBundle.message("inspection.fix.remove.declaration.family")

    override fun getName(): String =
        MyMessageBundle.message("inspection.fix.remove.declaration", keyword, nodeName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = element.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

        val line = document.getLineNumber(declarationStartOffset)
        val lineStart = document.getLineStartOffset(line)
        val lineEnd = document.getLineEndOffset(line)

        // Include the trailing newline if present, or the leading newline for the last line
        val fileText = document.text
        val deleteEnd = if (lineEnd < fileText.length && fileText[lineEnd] == '\n') lineEnd + 1 else lineEnd
        val deleteStart = if (deleteEnd == lineEnd && lineStart > 0 && fileText[lineStart - 1] == '\n') lineStart - 1 else lineStart

        WriteCommandAction.runWriteCommandAction(project) {
            document.deleteString(deleteStart, deleteEnd)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}