package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.ErrorReportRequest
import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.copyErrorToClipboard
import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.getMermaidRenderError
import com.alextdev.mermaidvisualizer.reportErrorOnGitHub
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Alt+Enter actions attached by [MermaidRenderExternalAnnotator] to a render error annotation.
 * They only touch the clipboard / browser (no write action, no preview) and are offered only while
 * the file still carries the annotated [error] (a newer render may have replaced or cleared it).
 */
internal sealed class MermaidRenderErrorIntention(protected val error: MermaidRenderError) : IntentionAction, DumbAware {

    override fun getFamilyName(): String = text

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean =
        file?.virtualFile?.getMermaidRenderError() == error
}

/** Copies the raw Mermaid.js message, as the preview banner's "Copy error" button does. */
internal class MermaidCopyRenderErrorIntention(error: MermaidRenderError) : MermaidRenderErrorIntention(error) {

    override fun getText(): String = MyMessageBundle.message("intention.render.error.copy")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        copyErrorToClipboard(error.message, project)
    }
}

/** Opens a prefilled GitHub issue for the error, as the preview banner's "Report on GitHub" button does. */
internal class MermaidReportRenderErrorIntention(error: MermaidRenderError) : MermaidRenderErrorIntention(error) {

    override fun getText(): String = MyMessageBundle.message("intention.render.error.report")

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        reportErrorOnGitHub(ErrorReportRequest(error.message, error.summary, error.category.payloadValue), project)
    }
}
