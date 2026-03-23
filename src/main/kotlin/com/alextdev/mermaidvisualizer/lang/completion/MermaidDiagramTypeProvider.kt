package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MermaidIcons
import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides diagram type keyword completion at file-level positions
 * (outside any diagram body).
 */
class MermaidDiagramTypeProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (MermaidCompletionData.isInsideDiagramBody(position)) return

        val typeText = MyMessageBundle.message("completion.mermaid.diagramType")
        for (kind in MermaidDiagramKind.entries) {
            val element = LookupElementBuilder.create(kind.keyword)
                .withIcon(MermaidIcons.FILE)
                .withTypeText(typeText)
                .withTailText(" ${MyMessageBundle.message(kind.displayKey)}", true)
                .withBoldness(kind in MermaidCompletionData.POPULAR_TYPES)
            result.addElement(PrioritizedLookupElement.withPriority(element, 100.0))
        }
    }
}
