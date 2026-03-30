package com.alextdev.mermaidvisualizer.lang.psi.impl

import com.alextdev.mermaidvisualizer.lang.psi.MermaidNamedElement
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiElementFactory
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.util.IncorrectOperationException

abstract class MermaidNamedElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), MermaidNamedElement {

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val newElement = try {
            MermaidPsiElementFactory.createNodeRef(project, name)
        } catch (e: IllegalArgumentException) {
            throw IncorrectOperationException(e)
        } catch (e: IllegalStateException) {
            throw IncorrectOperationException(e)
        }
        return replace(newElement)
    }

    override fun getNameIdentifier(): PsiElement? = firstChild

    override fun getReferences(): Array<PsiReference> =
        ReferenceProvidersRegistry.getReferencesFromProviders(this)

    override fun getReference(): PsiReference? =
        references.firstOrNull()
}
