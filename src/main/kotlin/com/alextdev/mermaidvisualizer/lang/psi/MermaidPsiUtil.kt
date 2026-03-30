package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil

/**
 * Shared PSI traversal utilities for inspections, completion, and navigation.
 * Pure PSI analysis — no IDE UI dependencies.
 */
object MermaidPsiUtil {

    // ── Data classes ────────────────────────────────────────────────────

    data class DeclaredNode(val name: String, val element: PsiElement, val keyword: String)
    data class UsedNode(val name: String, val element: PsiElement)

    // ── Diagram navigation ──────────────────────────────────────────────

    fun findDiagramBody(element: PsiElement): MermaidDiagramBody? {
        val diagramNode = PsiTreeUtil.getParentOfType(
            element,
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

    // ── Token neighbor helpers ───────────────────────────────────────────

    /** Returns the previous non-whitespace sibling, or null. */
    private fun prevSignificantSibling(element: PsiElement): PsiElement? {
        var sib = element.prevSibling
        while (sib != null && sib.node.elementType == TokenType.WHITE_SPACE) {
            sib = sib.prevSibling
        }
        return sib
    }

    /**
     * Returns true if the element is inside a bracket group `[...]`, `(...)`, or `{...}`.
     * Labels like `Start` in `A[Start]` are inside brackets and are NOT node names.
     */
    internal fun isInsideBrackets(element: PsiElement): Boolean {
        var depth = 0
        var sib = element.prevSibling
        while (sib != null) {
            val type = sib.node.elementType
            if (type == MermaidTokenTypes.BRACKET_CLOSE) {
                depth++
            } else if (type == MermaidTokenTypes.BRACKET_OPEN) {
                if (depth == 0) return true
                depth--
            }
            sib = sib.prevSibling
        }
        return false
    }

    /**
     * Returns true if the element follows a COLON without an intervening reset token
     * (ARROW, KEYWORD, END_KW, SEMICOLON) or newline. Used to filter out message text
     * in sequence diagrams, descriptions in ER diagrams, and other post-colon content.
     */
    internal fun isAfterColon(element: PsiElement): Boolean {
        var sib = element.prevSibling
        while (sib != null) {
            val type = sib.node.elementType
            when {
                type == MermaidTokenTypes.COLON -> return true
                type == MermaidTokenTypes.ARROW ||
                    type == MermaidTokenTypes.KEYWORD ||
                    type == MermaidTokenTypes.END_KW ||
                    type == MermaidTokenTypes.SEMICOLON -> return false
                type == TokenType.WHITE_SPACE && sib.text.contains('\n') -> return false
            }
            sib = sib.prevSibling
        }
        return false
    }

    /**
     * Returns true if the [MermaidNodeRef] represents a real node name
     * (not a label inside brackets and not message text after a colon).
     */
    fun isNodeName(nodeRef: MermaidNodeRef): Boolean =
        !isInsideBrackets(nodeRef) && !isAfterColon(nodeRef)

    // ── Identifier collection ───────────────────────────────────────────

    /**
     * Collects all unique identifier names from [MermaidNodeRef] elements in [body].
     * Traverses recursively through blocks.
     */
    fun collectAllIdentifiers(body: MermaidDiagramBody): Set<String> =
        PsiTreeUtil.findChildrenOfType(body, MermaidNodeRef::class.java)
            .mapNotNullTo(linkedSetOf()) { it.name?.takeIf(String::isNotBlank) }

    // ── Node ref lookup ─────────────────────────────────────────────────

    /**
     * Finds the first [MermaidNodeRef] with the given [name] in [body] (by document order).
     */
    fun findFirstNodeRef(body: MermaidDiagramBody, name: String): MermaidNodeRef? =
        PsiTreeUtil.findChildrenOfType(body, MermaidNodeRef::class.java)
            .firstOrNull { it.name == name }

    /**
     * Collects declared [MermaidNodeRef] elements for reference resolution.
     */
    fun collectDeclaredNodeRefs(body: MermaidDiagramBody, kind: MermaidDiagramKind): List<MermaidNodeRef> =
        collectDeclaredNodes(body, kind).mapNotNull { it.element as? MermaidNodeRef }

    // ── Declaration analysis (sequence / class) ─────────────────────────

    private val SEQUENCE_DECLARATION_KEYWORDS = setOf("participant", "actor")
    private val CLASS_DECLARATION_KEYWORDS = setOf("class")

    /**
     * Collects explicitly declared nodes in [body].
     *
     * Scans siblings for patterns: KEYWORD("participant"|"actor"|"class") → MermaidNodeRef.
     * Also handles `create participant X` / `create actor X`.
     */
    fun collectDeclaredNodes(body: MermaidDiagramBody, kind: MermaidDiagramKind): Set<DeclaredNode> {
        val declKeywords = when (kind) {
            MermaidDiagramKind.SEQUENCE -> SEQUENCE_DECLARATION_KEYWORDS
            MermaidDiagramKind.CLASS -> CLASS_DECLARATION_KEYWORDS
            else -> return emptySet()
        }

        val declared = linkedSetOf<DeclaredNode>()

        for (nodeRef in PsiTreeUtil.findChildrenOfType(body, MermaidNodeRef::class.java)) {
            val name = nodeRef.name?.takeIf(String::isNotBlank) ?: continue
            val prev = prevSignificantSibling(nodeRef) ?: continue

            // Matches "participant Alice", "actor Bob", "class Animal",
            // and also "create participant Alice" (prev is still "participant")
            if (prev.node.elementType == MermaidTokenTypes.KEYWORD && prev.text in declKeywords) {
                declared.add(DeclaredNode(name, nodeRef, prev.text))
            }
        }

        return declared
    }

    // ── Usage analysis (sequence / class) ───────────────────────────────

    private val SEQUENCE_SKIP_KEYWORDS = SEQUENCE_DECLARATION_KEYWORDS + "create"
    private val CLASS_SKIP_KEYWORDS = CLASS_DECLARATION_KEYWORDS

    /**
     * Collects identifiers used as participant references (sequence) or
     * relationship endpoints (class).
     *
     * Collects all [MermaidNodeRef] elements that are NOT:
     * - Immediately after a declaration keyword (participant/actor/class/create)
     * - Inside brackets (labels like `[Start]`)
     * - After a COLON (message text in sequence, properties in class)
     */
    fun collectUsedIdentifiers(body: MermaidDiagramBody, kind: MermaidDiagramKind): Set<UsedNode> {
        val skipKeywords = when (kind) {
            MermaidDiagramKind.SEQUENCE -> SEQUENCE_SKIP_KEYWORDS
            MermaidDiagramKind.CLASS -> CLASS_SKIP_KEYWORDS
            else -> return emptySet()
        }

        return PsiTreeUtil.findChildrenOfType(body, MermaidNodeRef::class.java)
            .filter { ref ->
                val name = ref.name
                !name.isNullOrBlank() &&
                    !isInsideBrackets(ref) &&
                    !isAfterColon(ref) &&
                    !isAfterKeyword(ref, skipKeywords)
            }
            .mapTo(linkedSetOf()) { UsedNode(it.name!!, it) }
    }

    private fun isAfterKeyword(element: PsiElement, keywords: Set<String>): Boolean {
        val prev = prevSignificantSibling(element) ?: return false
        return prev.node.elementType == MermaidTokenTypes.KEYWORD && prev.text in keywords
    }

    // ── Block helpers ───────────────────────────────────────────────────

    /**
     * Extracts the label/title from a [MermaidBlock].
     * For `subgraph Payment` returns "Payment", for `loop "Every minute"` returns "Every minute".
     * Returns null if no label is found (e.g. bare `alt`).
     */
    fun getBlockLabel(block: MermaidBlock): String? {
        var child = block.firstChild
        var foundKeyword = false
        while (child != null) {
            if (child is MermaidBlockContent) break
            if (child.node.elementType == MermaidTokenTypes.KEYWORD) {
                foundKeyword = true
            } else if (foundKeyword) {
                if (child is MermaidNodeRef) return child.name
                val type = child.node.elementType
                if (type == MermaidTokenTypes.STRING_DOUBLE) return child.text.removeSurrounding("\"")
                if (type == MermaidTokenTypes.STRING_SINGLE) return child.text.removeSurrounding("'")
            }
            child = child.nextSibling
        }
        return null
    }
}
