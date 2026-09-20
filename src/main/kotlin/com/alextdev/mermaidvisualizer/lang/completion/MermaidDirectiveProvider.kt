package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

/** Identifier-like run right before the caret: config keys and values such as `redux-dark-color`, `elk.box`, `handDrawn`. */
private val DIRECTIVE_PREFIX = Regex("""[A-Za-z0-9_.\-]*$""")

/** `'key': 'va` / `key: va` right before the caret — the config key whose value is being typed. */
private val KEY_BEFORE_CARET = Regex("""["']?([A-Za-z]+)["']?\s*:\s*["']?[A-Za-z0-9_.\-]*$""")

/**
 * Provides directive and configuration completion for `%%{init: {...}}%%` blocks.
 *
 * A directive is lexed as several consecutive DIRECTIVE tokens (`%%{`, text chunks split on `}`, `}%%`).
 * The platform's default completion prefix for such a token is the whole chunk up to the caret
 * (`init: {'theme': '`), which no lookup item can match, so the prefix is recomputed from the
 * identifier-like run before the caret and the current key is detected from the text before it.
 */
class MermaidDirectiveProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val leaf = parameters.originalPosition ?: return

        if (leaf.elementType == MermaidTokenTypes.DIRECTIVE) {
            val textBeforeCaret = directiveTextBeforeCaret(leaf, parameters.offset)
            val prefix = DIRECTIVE_PREFIX.find(textBeforeCaret)?.value ?: ""
            addDirectiveContentCompletions(textBeforeCaret, result.withPrefixMatcher(prefix))
            return
        }

        // At file level (outside any diagram body), offer the init directive template
        if (MermaidCompletionData.isInsideDiagramBody(parameters.position)) return
        addDirectiveTemplate(result)
    }

    /** Text of the directive from its `%%{` opener up to the caret, joined across the DIRECTIVE tokens. */
    private fun directiveTextBeforeCaret(leaf: PsiElement, caretOffset: Int): String {
        val inLeaf = (caretOffset - leaf.textRange.startOffset).coerceIn(0, leaf.textLength)
        val parts = ArrayDeque<String>()
        parts.addFirst(leaf.text.substring(0, inLeaf))
        var current: PsiElement? = if (leaf.text.startsWith("%%{")) null else leaf.prevSibling
        while (current != null && current.elementType == MermaidTokenTypes.DIRECTIVE) {
            parts.addFirst(current.text)
            if (current.text.startsWith("%%{")) break
            current = current.prevSibling
        }
        return parts.joinToString("")
    }

    private fun addDirectiveTemplate(result: CompletionResultSet) {
        val element = LookupElementBuilder.create("%%{init: {'theme': 'default'}}%%")
            .withPresentableText("%%{init: ...}%%")
            .withTypeText(MyMessageBundle.message("completion.mermaid.directive"))
        result.addElement(PrioritizedLookupElement.withPriority(element, 50.0))
    }

    private fun addDirectiveContentCompletions(textBeforeCaret: String, result: CompletionResultSet) {
        // Only inside the init config object: `%%{init: {` ... — other directives (`%%{wrap}%%`) get nothing.
        val afterInit = textBeforeCaret.substringAfter("init", missingDelimiterValue = "")
        if ("{" !in afterInit) return

        when (KEY_BEFORE_CARET.find(textBeforeCaret)?.groupValues?.get(1)) {
            "theme" -> addValues(MermaidCompletionData.DIRECTIVE_THEME_VALUES, "completion.mermaid.config.theme", result)
            "look" -> addValues(MermaidCompletionData.DIRECTIVE_LOOK_VALUES, "completion.mermaid.config.look", result)
            "layout" -> addValues(MermaidCompletionData.DIRECTIVE_LAYOUT_VALUES, "completion.mermaid.config.layout", result)
            else -> addValues(MermaidCompletionData.DIRECTIVE_CONFIG_KEYS, "completion.mermaid.config", result, priority = 50.0)
        }
    }

    private fun addValues(
        values: List<String>,
        typeTextKey: String,
        result: CompletionResultSet,
        priority: Double = 52.0,
    ) {
        val typeText = MyMessageBundle.message(typeTextKey)
        for (value in values) {
            val element = LookupElementBuilder.create(value).withTypeText(typeText)
            result.addElement(PrioritizedLookupElement.withPriority(element, priority))
        }
    }
}
