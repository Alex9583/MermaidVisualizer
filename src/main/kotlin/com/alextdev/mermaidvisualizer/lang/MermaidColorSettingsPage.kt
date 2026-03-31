package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MermaidIcons
import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

private val DESCRIPTORS = arrayOf(
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.comment"), MERMAID_COMMENT_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.directive"), MERMAID_DIRECTIVE_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.diagramType"), MERMAID_DIAGRAM_TYPE_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.keyword"), MERMAID_KEYWORD_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.string"), MERMAID_STRING_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.arrow"), MERMAID_ARROW_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.number"), MERMAID_NUMBER_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.braces"), MERMAID_BRACES_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.punctuation"), MERMAID_PUNCTUATION_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.identifier"), MERMAID_IDENTIFIER_KEY),
    AttributesDescriptor(MyMessageBundle.message("color.settings.mermaid.badCharacter"), MERMAID_BAD_CHAR_KEY),
)

class MermaidColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = MyMessageBundle.message("color.settings.mermaid.displayName")

    override fun getIcon(): Icon = MermaidIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = MermaidSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
}

private val DEMO_TEXT = """
    %% Flowchart example
    %%{init: {'theme': 'forest'}}%%
    flowchart LR
        subgraph "Auth Service"
            A[Login] --> B{Valid?}
            B -->|Yes| C[Dashboard]
            B -->|No| D[Error]
        end

    sequenceDiagram
        participant Alice
        Alice ->> Bob: Hello
        Bob -->> Alice: Hi

    pie title Browsers
        "Chrome" : 65.3
        "Firefox" : 10
""".trimIndent()
