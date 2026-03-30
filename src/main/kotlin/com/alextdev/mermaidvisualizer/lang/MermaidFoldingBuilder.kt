package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlock
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Provides code folding for Mermaid diagrams: block keywords (subgraph/loop/alt/etc.)
 * fold from end-of-keyword-line to end-of-`end`, and consecutive comments are grouped.
 */
class MermaidFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        addBlockFoldRegions(root, document, descriptors)
        addCommentFoldRegions(root, document, descriptors)
        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String {
        if (node.elementType == MermaidTokenTypes.COMMENT) return "%% ..."
        return "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun addBlockFoldRegions(root: PsiElement, document: Document, out: MutableList<FoldingDescriptor>) {
        for (block in PsiTreeUtil.findChildrenOfType(root, MermaidBlock::class.java)) {
            val endKw = block.node.findChildByType(MermaidTokenTypes.END_KW) ?: continue

            val startLine = document.getLineNumber(block.textRange.startOffset)
            val endLine = document.getLineNumber(endKw.startOffset)
            if (endLine <= startLine) continue

            val foldStart = document.getLineEndOffset(startLine)
            val foldEnd = block.textRange.endOffset
            if (foldEnd - foldStart < 2) continue

            out.add(FoldingDescriptor(block.node, TextRange(foldStart, foldEnd), null))
        }
    }

    private fun addCommentFoldRegions(root: PsiElement, document: Document, out: MutableList<FoldingDescriptor>) {
        val comments = PsiTreeUtil.collectElements(root) { it.node.elementType in MermaidTokenSets.COMMENTS }
        if (comments.size < 2) return

        var i = 0
        while (i < comments.size) {
            var j = i
            var currentLine = document.getLineNumber(comments[i].textRange.startOffset)
            // Extend the group as long as the next comment is on the immediately following line
            while (j + 1 < comments.size) {
                val nextLine = document.getLineNumber(comments[j + 1].textRange.startOffset)
                if (nextLine != currentLine + 1) break
                j++
                currentLine = nextLine
            }
            if (j > i) {
                val range = TextRange(comments[i].textRange.startOffset, comments[j].textRange.endOffset)
                out.add(FoldingDescriptor(comments[i].node, range, null))
            }
            i = j + 1
        }
    }
}
