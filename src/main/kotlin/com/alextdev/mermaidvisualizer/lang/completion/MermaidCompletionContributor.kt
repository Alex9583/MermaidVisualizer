package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.lang.MermaidLanguage
import com.alextdev.mermaidvisualizer.lang.MermaidTokenSets
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns

private val MERMAID_ELEMENT = PlatformPatterns.psiElement().withLanguage(MermaidLanguage)

class MermaidCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, MERMAID_ELEMENT, MermaidDiagramTypeProvider())
        extend(CompletionType.BASIC, MERMAID_ELEMENT, MermaidKeywordProvider())
        extend(CompletionType.BASIC, MERMAID_ELEMENT, MermaidNodeNameProvider())
        extend(CompletionType.BASIC, MERMAID_ELEMENT, MermaidArrowProvider())
        extend(CompletionType.BASIC, MERMAID_ELEMENT, MermaidDirectiveProvider())
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val tokenType = parameters.originalPosition?.node?.elementType
        if (tokenType in MermaidTokenSets.STRINGS || tokenType in MermaidTokenSets.COMMENTS) return
        super.fillCompletionVariants(parameters, result)
    }
}
