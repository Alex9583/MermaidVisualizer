package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlockContent
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDiagram
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.application.options.CodeStyle
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext

/**
 * Provides context-aware keyword completion inside diagram bodies.
 * Includes flowchart directions, block keywords with insert handlers, and `end`.
 */
class MermaidKeywordProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val kind = MermaidCompletionData.detectDiagramKind(position) ?: return

        if (!MermaidCompletionData.isInsideDiagramBody(position)) {
            // Special case: flowchart direction right after the DIAGRAM_TYPE token
            addFlowchartDirections(position, kind, result)
            return
        }

        addKeywords(position, kind, result)
        addFlowchartDirections(position, kind, result)
    }

    private fun addKeywords(position: PsiElement, kind: MermaidDiagramKind, result: CompletionResultSet) {
        val keywords = MermaidCompletionData.keywordsFor(kind)
        val blockKeywords = MermaidCompletionData.blockKeywordsFor(kind)
        val keywordTypeText = MyMessageBundle.message("completion.mermaid.keyword")
        val insideBlock = PsiTreeUtil.getParentOfType(position, MermaidBlockContent::class.java) != null

        for (keyword in keywords) {
            if (keyword == "end" && !insideBlock) continue
            val isBlock = keyword in blockKeywords
            val priority = when {
                keyword == "end" -> 85.0
                isBlock -> 82.0
                else -> 80.0
            }
            var builder = LookupElementBuilder.create(keyword)
                .withTypeText(keywordTypeText)
                .withBoldness(isBlock)
            if (isBlock) {
                builder = builder.withInsertHandler(BlockKeywordInsertHandler)
            }
            result.addElement(PrioritizedLookupElement.withPriority(builder, priority))
        }
    }

    private fun addFlowchartDirections(
        position: PsiElement,
        kind: MermaidDiagramKind,
        result: CompletionResultSet,
    ) {
        if (kind != MermaidDiagramKind.FLOWCHART && kind != MermaidDiagramKind.GRAPH) return

        if (isAfterDirectionKeyword(position) || isOnDiagramTypeLine(position)) {
            addDirectionElements(result)
        }
    }

    private fun isAfterDirectionKeyword(position: PsiElement): Boolean {
        var prev = PsiTreeUtil.prevLeaf(position, true)
        while (prev != null && prev.elementType == TokenType.WHITE_SPACE) {
            prev = PsiTreeUtil.prevLeaf(prev, true)
        }
        return prev != null && prev.elementType == MermaidTokenTypes.KEYWORD && prev.text == "direction"
    }

    private fun isOnDiagramTypeLine(position: PsiElement): Boolean {
        val flowchartDiagram = PsiTreeUtil.getParentOfType(position, MermaidFlowchartDiagram::class.java)
            ?: return false
        val diagramTypeNode = flowchartDiagram.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)
            ?: return false

        val diagramTypeOffset = diagramTypeNode.startOffset
        val positionOffset = position.textOffset
        val text = flowchartDiagram.containingFile.text
        if (diagramTypeOffset > text.length || positionOffset > text.length) return false

        val diagramTypeLine = StringUtil.offsetToLineNumber(text, diagramTypeOffset)
        val positionLine = StringUtil.offsetToLineNumber(text, positionOffset)
        return diagramTypeLine == positionLine
    }

    private fun addDirectionElements(result: CompletionResultSet) {
        val directionTypeText = MyMessageBundle.message("completion.mermaid.direction")
        for (dir in MermaidCompletionData.FLOWCHART_DIRECTIONS) {
            val element = LookupElementBuilder.create(dir.code)
                .withTypeText(directionTypeText)
                .withTailText(" ${MyMessageBundle.message(dir.descriptionKey)}", true)
            result.addElement(PrioritizedLookupElement.withPriority(element, 90.0))
        }
    }
}

private object BlockKeywordInsertHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val editor = context.editor
        val document = editor.document
        val offset = context.tailOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, offset))
        val currentIndent = lineText.takeWhile { it == ' ' || it == '\t' }

        val indentOptions = CodeStyle.getIndentOptions(context.file)
        val oneLevel = if (indentOptions.USE_TAB_CHARACTER) "\t"
                       else " ".repeat(indentOptions.INDENT_SIZE)

        val bodyIndent = currentIndent + oneLevel
        val template = " \n$bodyIndent\n${currentIndent}end"
        document.insertString(offset, template)
        editor.caretModel.moveToOffset(offset + 1 + bodyIndent.length + 1)
        context.commitDocument()
    }
}
