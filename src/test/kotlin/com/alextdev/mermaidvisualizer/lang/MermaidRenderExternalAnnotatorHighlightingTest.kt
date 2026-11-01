package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.MermaidRenderErrorCategory
import com.alextdev.mermaidvisualizer.editor.setMermaidRenderError
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

/**
 * Runs [MermaidRenderExternalAnnotator] through the real highlighting pipeline (ExternalToolPass):
 * the render error is put on the fixture's virtual file as [com.alextdev.mermaidvisualizer.editor.MermaidPreviewFileEditor]
 * does once the settle delay has elapsed.
 */
class MermaidRenderExternalAnnotatorHighlightingTest : BasePlatformTestCase() {

    // "flowchart LR" (12 chars) + "\n" → line 2 "    A -->" starts at offset 13, "-->" is [19, 22)
    private val source = "flowchart LR\n    A -->\n"

    private val incompleteSummary = "Line 2 looks unfinished: expected a node after \"A -->\""
    private val syntaxSummary = "Syntax error at line 2: unexpected \"-->\""

    private val incompleteError = MermaidRenderError(
        "Parse error on line 2:\n...LR    A -->\n-------------^\nExpecting 'AMP', got 'EOF'", 2, 7,
        summary = incompleteSummary,
        category = MermaidRenderErrorCategory.INCOMPLETE,
    )
    private val syntaxError = MermaidRenderError(
        "Parse error on line 2:\nExpecting 'SEMI', got 'ARROW'", 2, null,
        summary = syntaxSummary,
        category = MermaidRenderErrorCategory.SYNTAX,
    )

    private fun configure(error: MermaidRenderError?) {
        myFixture.configureByText("render-error.mmd", source)
        myFixture.file.virtualFile.setMermaidRenderError(error)
    }

    private fun renderErrorHighlight(description: String): HighlightInfo? =
        myFixture.doHighlighting().firstOrNull { it.description == description }

    fun testIncompleteErrorIsWeakWarningOnTheToken() {
        configure(incompleteError)
        val info = renderErrorHighlight(incompleteSummary)
        assertNotNull("The external annotator should report the render error", info)
        assertEquals(HighlightSeverity.WEAK_WARNING, info!!.severity)
        assertEquals(19, info.startOffset)
        assertEquals(22, info.endOffset)
        assertEquals("-->", source.substring(info.startOffset, info.endOffset))
    }

    fun testSyntaxErrorIsWarningOnTheWholeLine() {
        configure(syntaxError)
        val info = renderErrorHighlight(syntaxSummary)
        assertNotNull("The external annotator should report the render error", info)
        assertEquals(HighlightSeverity.WARNING, info!!.severity)
        assertEquals(13, info.startOffset)
        assertEquals(22, info.endOffset)
    }

    fun testTooltipMentionsTheActions() {
        configure(syntaxError)
        val info = renderErrorHighlight(syntaxSummary)
        assertNotNull(info)
        val tooltip = info!!.toolTip.orEmpty()
        assertTrue(tooltip, tooltip.contains("to copy the error or report it on GitHub"))
        assertTrue(tooltip, tooltip.contains("Expecting"))
    }

    fun testNoRenderErrorNoAnnotation() {
        configure(null)
        val highlights = myFixture.doHighlighting()
        assertTrue(
            "No render error annotation expected: $highlights",
            highlights.none { it.description == incompleteSummary || it.description == syntaxSummary },
        )
    }

    fun testBothIntentionsAreOfferedAtTheCaret() {
        configure(incompleteError)
        myFixture.editor.caretModel.moveToOffset(20)
        val texts = myFixture.availableIntentions.map { it.text }
        assertTrue("Copy intention missing in $texts", "Copy Mermaid error" in texts)
        assertTrue("Report intention missing in $texts", "Report Mermaid error on GitHub…" in texts)
    }

    fun testIntentionsAreOfferedOnASyntaxError() {
        configure(syntaxError)
        myFixture.editor.caretModel.moveToOffset(15)
        assertSize(1, myFixture.filterAvailableIntentions("Copy Mermaid error"))
        assertSize(1, myFixture.filterAvailableIntentions("Report Mermaid error on GitHub"))
    }

    fun testIntentionsAreNotOfferedOutsideTheRange() {
        configure(incompleteError)
        myFixture.editor.caretModel.moveToOffset(3) // inside "flowchart", line 1
        assertEmpty(myFixture.filterAvailableIntentions("Copy Mermaid error"))
    }

    fun testCopyIntentionCopiesTheRawMessage() {
        configure(incompleteError)
        myFixture.editor.caretModel.moveToOffset(20)
        val copy = myFixture.findSingleIntention("Copy Mermaid error")
        myFixture.launchAction(copy)
        val clipboard = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)
        assertEquals(incompleteError.message, clipboard)
    }

    fun testIntentionsAreUnavailableOnceTheErrorIsGone() {
        configure(incompleteError)
        myFixture.editor.caretModel.moveToOffset(20)
        val copy = MermaidCopyRenderErrorIntention(incompleteError)
        val report = MermaidReportRenderErrorIntention(incompleteError)
        assertTrue(copy.isAvailable(project, myFixture.editor, myFixture.file))
        assertTrue(report.isAvailable(project, myFixture.editor, myFixture.file))
        assertFalse(copy.startInWriteAction())
        assertFalse(report.startInWriteAction())

        myFixture.file.virtualFile.setMermaidRenderError(syntaxError)
        assertFalse("A newer error replaced the annotated one", copy.isAvailable(project, myFixture.editor, myFixture.file))

        myFixture.file.virtualFile.setMermaidRenderError(null)
        assertFalse(copy.isAvailable(project, myFixture.editor, myFixture.file))
        assertFalse(report.isAvailable(project, myFixture.editor, myFixture.file))
    }
}
