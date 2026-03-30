package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

/**
 * Registers [MermaidNodeReference] on [MermaidNodeRef] elements that represent
 * actual node names (not labels inside brackets or text after colons).
 *
 * Filtering is centralized via [MermaidPsiUtil.isNodeName] — labels inside brackets
 * and text after colons are excluded.
 */
class MermaidReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(MermaidNodeRef::class.java),
            MermaidNodeReferenceProvider(),
        )
    }
}

private class MermaidNodeReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext,
    ): Array<PsiReference> {
        val nodeRef = element as? MermaidNodeRef ?: return PsiReference.EMPTY_ARRAY
        if (!MermaidPsiUtil.isNodeName(nodeRef)) return PsiReference.EMPTY_ARRAY
        return arrayOf(MermaidNodeReference(nodeRef))
    }
}
