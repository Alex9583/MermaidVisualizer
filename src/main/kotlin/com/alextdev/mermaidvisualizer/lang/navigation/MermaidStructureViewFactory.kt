package com.alextdev.mermaidvisualizer.lang.navigation

import com.alextdev.mermaidvisualizer.MermaidIcons
import com.alextdev.mermaidvisualizer.lang.MermaidFile
import com.alextdev.mermaidvisualizer.lang.MermaidTokenTypes
import com.alextdev.mermaidvisualizer.lang.completion.MermaidCompletionData
import com.alextdev.mermaidvisualizer.lang.completion.MermaidDiagramKind
import com.alextdev.mermaidvisualizer.lang.psi.MermaidBlock
import com.alextdev.mermaidvisualizer.lang.psi.MermaidClassDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidDiagramBody
import com.alextdev.mermaidvisualizer.lang.psi.MermaidErDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidFlowchartDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidGenericDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidNodeRef
import com.alextdev.mermaidvisualizer.lang.psi.MermaidPsiUtil
import com.alextdev.mermaidvisualizer.lang.psi.MermaidSequenceDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStateDiagram
import com.alextdev.mermaidvisualizer.lang.psi.MermaidStatement
import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

class MermaidStructureViewFactory : PsiStructureViewFactory {

    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is MermaidFile) return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                MermaidStructureViewModel(psiFile, editor)
        }
    }
}

private class MermaidStructureViewModel(file: PsiFile, editor: Editor?) :
    StructureViewModelBase(file, editor, MermaidStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element.value is MermaidNodeRef
}

private class MermaidStructureViewElement(private val element: PsiElement) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element

    override fun getAlphaSortKey(): String {
        if (!element.isValid) return ""
        return if (element is MermaidNodeRef) element.name ?: "" else element.text.take(30)
    }

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? {
            if (!element.isValid) return null
            return when (element) {
                is MermaidFile -> element.name
                is MermaidFlowchartDiagram -> buildDiagramLabel(element)
                is MermaidSequenceDiagram -> "sequenceDiagram"
                is MermaidClassDiagram -> "classDiagram"
                is MermaidErDiagram -> "erDiagram"
                is MermaidStateDiagram -> buildStateDiagramLabel(element)
                is MermaidGenericDiagram -> buildGenericDiagramLabel(element)
                is MermaidBlock -> buildBlockLabel(element)
                is MermaidNodeRef -> element.name
                else -> element.text.take(30)
            }
        }

        override fun getLocationString(): String? = null

        override fun getIcon(unused: Boolean): Icon? {
            if (!element.isValid) return null
            return when (element) {
                is MermaidFile -> MermaidIcons.FILE
                is MermaidFlowchartDiagram, is MermaidSequenceDiagram,
                is MermaidClassDiagram, is MermaidErDiagram,
                is MermaidStateDiagram, is MermaidGenericDiagram -> MermaidIcons.FILE
                is MermaidBlock -> AllIcons.Nodes.Folder
                is MermaidNodeRef -> nodeRefIcon()
                else -> null
            }
        }

        private fun nodeRefIcon(): Icon {
            val kind = MermaidCompletionData.detectDiagramKind(element)
            return when (kind) {
                MermaidDiagramKind.CLASS -> AllIcons.Nodes.Class
                MermaidDiagramKind.SEQUENCE -> AllIcons.Nodes.Property
                else -> AllIcons.Nodes.Variable
            }
        }
    }

    override fun getChildren(): Array<StructureViewTreeElement> {
        if (!element.isValid) return emptyArray()
        return when (element) {
            is MermaidFile -> fileChildren()
            is MermaidBlock -> blockChildren(element)
            else -> {
                val body = PsiTreeUtil.getChildOfType(element, MermaidDiagramBody::class.java)
                if (body != null) diagramChildren(body) else emptyArray()
            }
        }
    }

    private fun fileChildren(): Array<StructureViewTreeElement> {
        val diagrams = PsiTreeUtil.findChildrenOfType(element, MermaidDiagram::class.java)
        // Single-diagram optimization: skip the diagram node layer
        if (diagrams.size == 1) {
            val diagram = diagrams.first()
            val body = PsiTreeUtil.findChildOfType(diagram, MermaidDiagramBody::class.java)
            return diagramChildren(body)
        }
        return diagrams.map { d ->
            val specific = d.flowchartDiagram ?: d.sequenceDiagram ?: d.classDiagram
                ?: d.erDiagram ?: d.stateDiagram ?: d.genericDiagram ?: d
            MermaidStructureViewElement(specific)
        }.toTypedArray()
    }

    private fun diagramChildren(body: MermaidDiagramBody?): Array<StructureViewTreeElement> {
        if (body == null) return emptyArray()
        val children = mutableListOf<StructureViewTreeElement>()

        for (block in body.blockList) {
            children.add(MermaidStructureViewElement(block))
        }

        val kind = MermaidCompletionData.detectDiagramKind(body)
        if (kind == MermaidDiagramKind.SEQUENCE || kind == MermaidDiagramKind.CLASS) {
            val declared = MermaidPsiUtil.collectDeclaredNodes(body, kind)
            for (node in declared) {
                children.add(MermaidStructureViewElement(node.element))
            }
        } else {
            collectUniqueNodes(body, children)
        }

        return children.toTypedArray()
    }

    private fun blockChildren(block: MermaidBlock): Array<StructureViewTreeElement> {
        val content = block.blockContent ?: return emptyArray()
        val children = mutableListOf<StructureViewTreeElement>()

        for (nestedBlock in content.blockList) {
            children.add(MermaidStructureViewElement(nestedBlock))
        }

        collectUniqueNodes(content, children)
        return children.toTypedArray()
    }

    /**
     * Collects unique node refs from direct [MermaidStatement] children of [container],
     * filtering out labels (inside brackets) and message text (after colon).
     */
    private fun collectUniqueNodes(container: PsiElement, out: MutableList<StructureViewTreeElement>) {
        val seen = mutableSetOf<String>()
        val statements = container.children.filterIsInstance<MermaidStatement>()
        for (stmt in statements) {
            for (child in stmt.children) {
                if (child is MermaidNodeRef) {
                    val name = child.name ?: continue
                    if (name.isNotBlank() &&
                        MermaidPsiUtil.isNodeName(child) &&
                        seen.add(name)
                    ) {
                        out.add(MermaidStructureViewElement(child))
                    }
                }
            }
        }
    }

    override fun navigate(requestFocus: Boolean) {
        if (element is NavigatablePsiElement && element.isValid) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean =
        element is NavigatablePsiElement && element.isValid && element.canNavigate()

    override fun canNavigateToSource(): Boolean =
        element is NavigatablePsiElement && element.isValid && element.canNavigateToSource()
}

// ── Presentation helpers ────────────────────────────────────────────

private fun buildDiagramLabel(diagram: MermaidFlowchartDiagram): String {
    val direction = diagram.flowchartDirection?.text
    val type = diagram.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text ?: "flowchart"
    return if (direction != null) "$type $direction" else type
}

private fun buildStateDiagramLabel(diagram: MermaidStateDiagram): String =
    diagram.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text ?: "stateDiagram"

private fun buildGenericDiagramLabel(diagram: MermaidGenericDiagram): String =
    diagram.node.findChildByType(MermaidTokenTypes.DIAGRAM_TYPE)?.text ?: "diagram"

private fun buildBlockLabel(block: MermaidBlock): String {
    val keyword = block.node.findChildByType(MermaidTokenTypes.KEYWORD)?.text ?: "block"
    val label = MermaidPsiUtil.getBlockLabel(block)
    return if (label != null) "$keyword $label" else keyword
}
