package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

/**
 * Provides directive and configuration completion for `%%{init: {...}}%%` blocks.
 * Heuristic parsing of the DIRECTIVE token content to determine position.
 */
class MermaidDirectiveProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (MermaidCompletionData.isInsideDiagramBody(position)) return

        // Check if we're adjacent to or inside a DIRECTIVE token
        val leaf = parameters.originalPosition ?: return
        if (leaf.elementType == MermaidTokenTypes.DIRECTIVE) {
            addDirectiveContentCompletions(leaf.text, result)
            return
        }

        // At file level, offer the init directive template
        addDirectiveTemplate(result)
    }

    private fun addDirectiveTemplate(result: CompletionResultSet) {
        val element = LookupElementBuilder.create("%%{init: {'theme': 'default'}}%%")
            .withPresentableText("%%{init: ...}%%")
            .withTypeText(MyMessageBundle.message("completion.mermaid.directive"))
        result.addElement(PrioritizedLookupElement.withPriority(element, 50.0))
    }

    private fun addDirectiveContentCompletions(directiveText: String, result: CompletionResultSet) {
        val configTypeText = MyMessageBundle.message("completion.mermaid.config")

        // Detect if we're inside the init config object
        if ("init" in directiveText && "{" in directiveText.substringAfter("init")) {
            // Offer config keys
            for (key in MermaidCompletionData.DIRECTIVE_CONFIG_KEYS) {
                val element = LookupElementBuilder.create(key)
                    .withTypeText(configTypeText)
                result.addElement(PrioritizedLookupElement.withPriority(element, 50.0))
            }

            if ("theme" in directiveText) {
                val themeTypeText = MyMessageBundle.message("completion.mermaid.config.theme")
                for (value in MermaidCompletionData.DIRECTIVE_THEME_VALUES) {
                    val element = LookupElementBuilder.create(value)
                        .withTypeText(themeTypeText)
                    result.addElement(PrioritizedLookupElement.withPriority(element, 52.0))
                }
            }

            if ("look" in directiveText) {
                val lookTypeText = MyMessageBundle.message("completion.mermaid.config.look")
                for (value in MermaidCompletionData.DIRECTIVE_LOOK_VALUES) {
                    val element = LookupElementBuilder.create(value)
                        .withTypeText(lookTypeText)
                    result.addElement(PrioritizedLookupElement.withPriority(element, 52.0))
                }
            }
        }
    }
}
