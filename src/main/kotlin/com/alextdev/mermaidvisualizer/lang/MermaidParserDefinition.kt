package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.lang.parser.MermaidParser
import com.alextdev.mermaidvisualizer.lang.psi.MermaidElementTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MermaidParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = MermaidLexer()

    override fun getCommentTokens(): TokenSet = MermaidTokenSets.COMMENTS

    override fun getWhitespaceTokens(): TokenSet = MermaidTokenSets.WHITE_SPACES

    override fun getStringLiteralElements(): TokenSet = MermaidTokenSets.STRINGS

    override fun createParser(project: Project?): PsiParser = MermaidParser()

    override fun getFileNodeType(): IFileElementType = MERMAID_FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MermaidFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = MermaidElementTypes.Factory.createElement(node)
}
