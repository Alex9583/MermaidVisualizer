package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.MermaidIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object MermaidFileType : LanguageFileType(MermaidLanguage) {
    override fun getName(): String = "Mermaid"
    override fun getDescription(): String = "Mermaid diagram file"
    override fun getDefaultExtension(): String = "mmd"
    override fun getIcon(): Icon = MermaidIcons.FILE
}
