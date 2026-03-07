package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MyMessageBundle
import com.intellij.lang.Language

object MermaidLanguage : Language("Mermaid") {
    private fun readResolve(): Any = MermaidLanguage
    override fun getDisplayName(): String = MyMessageBundle.message("language.mermaid.displayName")
}
