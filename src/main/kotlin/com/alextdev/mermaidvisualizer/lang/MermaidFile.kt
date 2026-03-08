package com.alextdev.mermaidvisualizer.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class MermaidFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MermaidLanguage) {
    override fun getFileType(): FileType = MermaidFileType
}
