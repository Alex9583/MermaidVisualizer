package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Reference from a [MermaidNodeRef] usage to its declaration or first occurrence.
 *
 * - **SEQUENCE/CLASS**: resolves to the explicit declaration (`participant X`, `class X`),
 *   falling back to the first occurrence if no declaration exists
 * - **All other types**: resolves to the first occurrence of the identifier in the diagram body
 */
class MermaidNodeReference(element: MermaidNodeRef) :
    PsiReferenceBase<MermaidNodeRef>(element, TextRange(0, element.textLength), true) {

    override fun resolve(): PsiElement? {
        if (!element.isValid) return null
        val name = element.name ?: return null
        val body = MermaidPsiUtil.findDiagramBody(element) ?: return null
        val kind = MermaidCompletionData.detectDiagramKind(element)

        if (kind == MermaidDiagramKind.SEQUENCE || kind == MermaidDiagramKind.CLASS) {
            val decl = MermaidPsiUtil.collectDeclaredNodeRefs(body, kind)
                .find { it.name == name }
            if (decl != null) return decl
        }

        return MermaidPsiUtil.findFirstNodeRef(body, name)
    }

    override fun handleElementRename(newElementName: String): PsiElement =
        element.setName(newElementName)

    override fun getVariants(): Array<Any> = emptyArray()
}
