package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil

/**
 * Shared PSI traversal utilities for inspections and completion.
 * Pure PSI analysis — no IDE UI dependencies.
 *
 * NOTE: The Grammar-Kit parser creates a SINGLE [MermaidStatement] per diagram body
 * (all tokens are in one statement — no newline-based splitting). All analysis works
 * at the token neighbor level using [prevSignificantSibling]/[nextSignificantSibling].
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

    /** Returns the next non-whitespace sibling, or null. */
    private fun nextSignificantSibling(element: PsiElement): PsiElement? {
        var sib = element.nextSibling
        while (sib != null && sib.node.elementType == TokenType.WHITE_SPACE) {
            sib = sib.nextSibling
        }
        return sib
    }

    /** Returns the previous non-whitespace sibling, or null. */
    private fun prevSignificantSibling(element: PsiElement): PsiElement? {
        var sib = element.prevSibling
        while (sib != null && sib.node.elementType == TokenType.WHITE_SPACE) {
            sib = sib.prevSibling
        }
        return sib
    }

    /**
     * Returns true if the IDENTIFIER is inside a bracket group `[...]`.
     * Labels like `Start` in `A[Start]` are inside brackets and are NOT node names.
     */
    private fun isInsideBrackets(element: PsiElement): Boolean {
        // Walk backwards looking for an unmatched BRACKET_OPEN
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

    // ── Identifier collection ───────────────────────────────────────────

    fun collectAllIdentifiers(body: MermaidDiagramBody): Set<String> {
        val names = linkedSetOf<String>()
        val statements = PsiTreeUtil.findChildrenOfType(body, MermaidStatement::class.java)
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

    // ── Declaration analysis (sequence / class) ─────────────────────────

    private val SEQUENCE_DECLARATION_KEYWORDS = setOf("participant", "actor")
    private val CLASS_DECLARATION_KEYWORDS = setOf("class")

    /**
     * Collects explicitly declared nodes in [body].
     *
     * Scans all tokens for patterns: KEYWORD("participant"|"actor"|"class") → IDENTIFIER.
     * Also handles `create participant X` / `create actor X`.
     */
    fun collectDeclaredNodes(body: MermaidDiagramBody, kind: MermaidDiagramKind): Set<DeclaredNode> {
        val declKeywords = when (kind) {
            MermaidDiagramKind.SEQUENCE -> SEQUENCE_DECLARATION_KEYWORDS
            MermaidDiagramKind.CLASS -> CLASS_DECLARATION_KEYWORDS
            else -> return emptySet()
        }

        val declared = linkedSetOf<DeclaredNode>()
        val statements = PsiTreeUtil.findChildrenOfType(body, MermaidStatement::class.java)

        for (stmt in statements) {
            var child = stmt.firstChild
            while (child != null) {
                if (child.node.elementType == MermaidTokenTypes.KEYWORD) {
                    val kwText = child.text
                    if (kwText in declKeywords) {
                        val nameElement = nextSignificantSibling(child)
                        if (nameElement?.node?.elementType == MermaidTokenTypes.IDENTIFIER) {
                            declared.add(DeclaredNode(nameElement.text, nameElement, kwText))
                        }
                    } else if (kwText == "create") {
                        val nextKw = nextSignificantSibling(child)
                        if (nextKw?.node?.elementType == MermaidTokenTypes.KEYWORD && nextKw.text in declKeywords) {
                            val nameElement = nextSignificantSibling(nextKw)
                            if (nameElement?.node?.elementType == MermaidTokenTypes.IDENTIFIER) {
                                declared.add(DeclaredNode(nameElement.text, nameElement, nextKw.text))
                            }
                        }
                    }
                }
                child = child.nextSibling
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
     * Collects all IDENTIFIER tokens that are NOT:
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

        val used = linkedSetOf<UsedNode>()
        val statements = PsiTreeUtil.findChildrenOfType(body, MermaidStatement::class.java)

        for (stmt in statements) {
            var afterColon = false
            var child = stmt.firstChild
            while (child != null) {
                val type = child.node.elementType

                // COLON starts message text / property section — skip until next ARROW or declaration keyword
                if (type == MermaidTokenTypes.COLON) {
                    afterColon = true
                    child = child.nextSibling
                    continue
                }

                // ARROW or declaration keyword resets the "after colon" state
                if (type == MermaidTokenTypes.ARROW ||
                    (type == MermaidTokenTypes.KEYWORD && child.text in skipKeywords)
                ) {
                    afterColon = false
                    child = child.nextSibling
                    continue
                }

                if (type == MermaidTokenTypes.IDENTIFIER && !afterColon) {
                    val text = child.text
                    if (text.isNotBlank() && !isInsideBrackets(child)) {
                        val prev = prevSignificantSibling(child)
                        val isDeclaration = prev?.node?.elementType == MermaidTokenTypes.KEYWORD &&
                            prev.text in skipKeywords
                        if (!isDeclaration) {
                            used.add(UsedNode(text, child))
                        }
                    }
                }
                child = child.nextSibling
            }
        }
        return used
    }
}
