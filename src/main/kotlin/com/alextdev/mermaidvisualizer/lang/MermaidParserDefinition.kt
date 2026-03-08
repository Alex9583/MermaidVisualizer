package com.alextdev.mermaidvisualizer.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class MermaidParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = MermaidLexer()

    override fun getCommentTokens(): TokenSet = MermaidTokenSets.COMMENTS

    override fun getWhitespaceTokens(): TokenSet = MermaidTokenSets.WHITE_SPACES

    override fun getStringLiteralElements(): TokenSet = MermaidTokenSets.STRINGS

    override fun createParser(project: Project?): PsiParser = MermaidSimpleParser()

    override fun getFileNodeType(): IFileElementType = MERMAID_FILE

    override fun createFile(viewProvider: FileViewProvider): PsiFile = MermaidFile(viewProvider)

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)
}

private class MermaidSimpleParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        marker.done(root)
        return builder.treeBuilt
    }
}
