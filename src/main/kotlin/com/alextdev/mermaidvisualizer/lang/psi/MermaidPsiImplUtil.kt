package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

@Suppress("unused")
object MermaidPsiImplUtil {

    @JvmStatic
    fun getDiagramTypeName(element: PsiElement): String? {
        val child = element.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)
        return child?.text
    }

    @JvmStatic
    fun getBlockKeywordText(element: PsiElement): String? {
        val child = element.node.findChildByType(MermaidTokenTypes.KEYWORD)
        return child?.text
    }

    @JvmStatic
    fun getDiagramBody(element: PsiElement): MermaidDiagramBody? {
        return PsiTreeUtil.getChildOfType(element, MermaidDiagramBody::class.java)
    }
}
