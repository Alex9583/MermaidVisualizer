package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext

/**
 * Provides node/participant name completion by scanning [MermaidNodeRef][com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef]
 * elements within the same diagram. Scope is limited to the current diagram (not cross-diagram).
 */
class MermaidNodeNameProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (!MermaidCompletionData.isInsideDiagramBody(position)) return

        val diagramBody = MermaidPsiUtil.findDiagramBody(position) ?: return
        val nodeNames = MermaidPsiUtil.collectAllIdentifiers(diagramBody)

        // Exclude the dummy identifier inserted by the platform at caret position
        val currentText = position.text
        val nodeTypeText = MyMessageBundle.message("completion.mermaid.node")

        for (name in nodeNames) {
            if (name == currentText) continue
            val element = LookupElementBuilder.create(name)
                .withTypeText(nodeTypeText)
            result.addElement(PrioritizedLookupElement.withPriority(element, 90.0))
        }
    }
}
