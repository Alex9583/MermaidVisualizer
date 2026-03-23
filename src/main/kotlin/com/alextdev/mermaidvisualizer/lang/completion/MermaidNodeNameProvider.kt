package com.alextdev.mermaidvisualizer.lang.completion

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.psi.MermaidClassDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagramBody
import com.alextdev.mermaidvisualizer.lang.psi.MermaidErDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidGenericDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidSequenceDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStateDiagram
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Provides node/participant name completion by scanning IDENTIFIER tokens
 * within the same diagram. Scope is limited to the current diagram (not cross-diagram).
 */
class MermaidNodeNameProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        if (!MermaidCompletionData.isInsideDiagramBody(position)) return

        val diagramBody = findDiagramBody(position) ?: return
        val nodeNames = collectIdentifiers(diagramBody)

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

    /**
     * Finds the root [MermaidDiagramBody] for the current diagram
     * by walking up to the typed diagram node and getting its body.
     */
    private fun findDiagramBody(position: PsiElement): MermaidDiagramBody? {
        val diagramNode = PsiTreeUtil.getParentOfType(
            position,
            MermaidFlowchartDiagram::class.java,
            MermaidSequenceDiagram::class.java,
            MermaidClassDiagram::class.java,
            MermaidErDiagram::class.java,
            MermaidStateDiagram::class.java,
            MermaidGenericDiagram::class.java,
        ) ?: return null

        return when (diagramNode) {
            is MermaidFlowchartDiagram -> diagramNode.diagramBody
            is MermaidSequenceDiagram -> diagramNode.diagramBody
            is MermaidClassDiagram -> diagramNode.diagramBody
            is MermaidErDiagram -> diagramNode.diagramBody
            is MermaidStateDiagram -> diagramNode.diagramBody
            is MermaidGenericDiagram -> diagramNode.diagramBody
            else -> null
        }
    }

    /**
     * Collects all unique IDENTIFIER token texts from all statements in the diagram body,
     * including nested blocks.
     */
    private fun collectIdentifiers(diagramBody: MermaidDiagramBody): Set<String> {
        val names = linkedSetOf<String>()
        val statements = PsiTreeUtil.findChildrenOfType(diagramBody, MermaidStatement::class.java)
        for (stmt in statements) {
            var child = stmt.firstChild
            while (child != null) {
                if (child.node.elementType == MermaidTokenTypes.IDENTIFIER) {
                    val text = child.text
                    if (text.isNotBlank()) {
                        names.add(text)
                    }
                }
                child = child.nextSibling
            }
        }
        return names
    }
}
