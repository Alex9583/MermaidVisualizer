package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidLexer
import com.alextdev.mermaidvisualizer.lang.MermaidTokenSets
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet

/**
 * Enables the Find Usages action (Alt+F7) for Mermaid node references.
 *
 * Provides a [WordsScanner] for indexing and type descriptions for the UI.
 * The actual usage search is handled by the standard IntelliJ pipeline via
 * [MermaidNodeReference] and [MermaidReferenceContributor].
 */
class MermaidFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        MermaidLexer(),
        TokenSet.create(MermaidTokenTypes.IDENTIFIER),
        MermaidTokenSets.COMMENTS,
        MermaidTokenSets.STRINGS,
    )

    override fun canFindUsagesFor(element: PsiElement): Boolean =
        element is MermaidNodeRef

    override fun getType(element: PsiElement): String {
        val kind = MermaidCompletionData.detectDiagramKind(element)
        return when (kind) {
            MermaidDiagramKind.SEQUENCE -> MyMessageBundle.message("find.usages.type.participant")
            MermaidDiagramKind.CLASS -> MyMessageBundle.message("find.usages.type.class")
            MermaidDiagramKind.ER -> MyMessageBundle.message("find.usages.type.entity")
            MermaidDiagramKind.STATE, MermaidDiagramKind.STATE_V1 ->
                MyMessageBundle.message("find.usages.type.state")
            MermaidDiagramKind.GIT_GRAPH -> MyMessageBundle.message("find.usages.type.branch")
            else -> MyMessageBundle.message("find.usages.type.node")
        }
    }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? MermaidNodeRef)?.name ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        (element as? MermaidNodeRef)?.name ?: element.text

    override fun getHelpId(element: PsiElement): String? = null
}
