package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

class MermaidArrowProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (!MermaidCompletionData.isInsideDiagramBody(position)) return

        // Check prev token first (most selective — filters ~80%+ of cases)
        val prevLeaf = findPrevNonWhitespaceLeaf(position) ?: return
        val prevType = prevLeaf.elementType
        if (prevType != MermaidTokenTypes.IDENTIFIER && prevType != MermaidTokenTypes.BRACKET_CLOSE) return

        val kind = MermaidCompletionData.detectDiagramKind(position) ?: return

        val arrows = MermaidCompletionData.arrowsFor(kind)
        if (arrows.isEmpty()) return

        val arrowTypeText = MyMessageBundle.message("completion.mermaid.arrow")
        for (entry in arrows) {
            val element = LookupElementBuilder.create(entry.arrow)
                .withTypeText(arrowTypeText)
                .withTailText(" ${MyMessageBundle.message(entry.descriptionKey)}", true)
                .withInsertHandler(ArrowInsertHandler)
            result.addElement(PrioritizedLookupElement.withPriority(element, 70.0))
        }
    }

    private fun findPrevNonWhitespaceLeaf(element: PsiElement): PsiElement? {
        var prev = PsiTreeUtil.prevLeaf(element, true)
        while (prev != null && prev.elementType == TokenType.WHITE_SPACE) {
            prev = PsiTreeUtil.prevLeaf(prev, true)
        }
        return prev
    }
}

private object ArrowInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document
        val offset = context.tailOffset
        // Add trailing space after arrow
        if (offset >= document.textLength || document.text[offset] != ' ') {
            document.insertString(offset, " ")
        }
        editor.caretModel.moveToOffset(offset + 1)
    }
}
