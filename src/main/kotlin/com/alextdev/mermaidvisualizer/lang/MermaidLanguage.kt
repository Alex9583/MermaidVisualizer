package com.alextdev.mermaidvisualizer.lang

import com.intellij.lang.Language

object MermaidLanguage : Language("Mermaid") {
    private fun readResolve(): Any = MermaidLanguage
    override fun getDisplayName(): String = "Mermaid"
}
