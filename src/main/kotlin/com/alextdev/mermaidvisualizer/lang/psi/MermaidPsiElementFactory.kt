package com.alextdev.mermaidvisualizer.lang.psi

import com.alextdev.mermaidvisualizer.lang.MermaidFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object MermaidPsiElementFactory {

    fun createNodeRef(project: Project, name: String): MermaidNodeRef {
        require(name.isNotBlank() && name.none { it.isWhitespace() }) {
            "Invalid Mermaid node name: '$name'"
        }
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "dummy.mmd", MermaidFileType, "flowchart LR\n    $name"
        )
        val nodeRef = PsiTreeUtil.findChildOfType(file, MermaidNodeRef::class.java)
            ?: error("Failed to create MermaidNodeRef for '$name'")
        // A name the lexer splits (`B@`, `A-->B`) would silently produce a truncated node ref.
        check(nodeRef.text == name) { "Invalid Mermaid node name: '$name' is not a single identifier" }
        return nodeRef
    }
}
