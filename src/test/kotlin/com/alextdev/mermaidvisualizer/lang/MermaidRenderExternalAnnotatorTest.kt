package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.alextdev.mermaidvisualizer.editor.MermaidRenderErrorCategory
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pure tests of the range ([computeRenderErrorRange]) and severity ([renderErrorSeverity]) logic of
 * [MermaidRenderExternalAnnotator]. The annotator itself is exercised through the highlighting pipeline
 * in [MermaidRenderExternalAnnotatorHighlightingTest].
 */
class MermaidRenderExternalAnnotatorTest {

    private fun computeRange(document: Document, error: MermaidRenderError): TextRange =
        computeRenderErrorRange(document, error)

    // "flowchart LR" = 12 chars → getLineEndOffset(0) = 12
    // "flowchart LR\n    A --> B" → line 2 starts at offset 13

    @Test
    fun testLineAndColumnPlacesAtOffset() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val error = MermaidRenderError("error", line = 2, column = 5)
        val range = computeRange(doc, error)
        // Line 2 starts at offset 13, column 5 → offset 13 + 4 = 17, range = [17, 18)
        assertEquals(17, range.startOffset)
        assertEquals(18, range.endOffset)
    }

    @Test
    fun testLineOnlyCoversFullLine() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val error = MermaidRenderError("error", line = 1, column = null)
        val range = computeRange(doc, error)
        // Line 1 = "flowchart LR" → [0, 12)
        assertEquals(0, range.startOffset)
        assertEquals(12, range.endOffset)
    }

    @Test
    fun testColumnBeyondLineEndFallsBackToWholeLine() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val error = MermaidRenderError("error", line = 1, column = 100)
        val range = computeRange(doc, error)
        // Column 100 is beyond "flowchart LR" (12 chars), fallback to whole line
        assertEquals(0, range.startOffset)
        assertEquals(12, range.endOffset)
    }

    @Test
    fun testNoLineInfoAnnotatesFirstLine() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val error = MermaidRenderError("error", line = null, column = null)
        val range = computeRange(doc, error)
        // Fallback: first line → [0, 12)
        assertEquals(0, range.startOffset)
        assertEquals(12, range.endOffset)
    }

    @Test
    fun testLineOutOfRangeAnnotatesFirstLine() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val error = MermaidRenderError("error", line = 999, column = null)
        val range = computeRange(doc, error)
        // Line 999 is out of range → fallback to first line
        assertEquals(0, range.startOffset)
        assertEquals(12, range.endOffset)
    }

    @Test
    fun testSingleLineDocument() {
        val doc = DocumentImpl("flowchart LR")
        val error = MermaidRenderError("error", line = 1, column = 5)
        val range = computeRange(doc, error)
        // Line 1, column 5 → offset 4, range = [4, 5)
        assertEquals(4, range.startOffset)
        assertEquals(5, range.endOffset)
    }

    @Test
    fun testEmptyDocumentFallback() {
        val doc = DocumentImpl("")
        val error = MermaidRenderError("error", line = null, column = null)
        val range = computeRange(doc, error)
        assertEquals(0, range.startOffset)
        assertTrue(range.endOffset >= 0, "endOffset should be >= 0")
    }

    // ── Column narrowed to the token ─────────────────────────────────────

    @Test
    fun testColumnExtendsToTokenEnd() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        // Line 2, column 7 → offset 19 = start of "-->", token ends at 22
        val range = computeRenderErrorRange(doc, MermaidRenderError("error", line = 2, column = 7)) { 22 }
        assertEquals(TextRange(19, 22), range)
    }

    @Test
    fun testTokenEndIsClampedToTheLine() {
        val doc = DocumentImpl("flowchart LR\n    A --> B\nnext")
        // A token running past the end of line 2 (e.g. a multi-line string) stops at the line end (24)
        val range = computeRenderErrorRange(doc, MermaidRenderError("error", line = 2, column = 7)) { 100 }
        assertEquals(TextRange(19, 24), range)
    }

    @Test
    fun testTokenEndBeforeColumnKeepsOneCharacter() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val range = computeRenderErrorRange(doc, MermaidRenderError("error", line = 2, column = 7)) { 10 }
        assertEquals(TextRange(19, 20), range)
    }

    @Test
    fun testColumnJustPastLineEndCoversWholeLine() {
        val doc = DocumentImpl("flowchart LR\n    A -->")
        // "    A -->" has 9 characters: column 10 points after its last character
        val range = computeRenderErrorRange(doc, MermaidRenderError("error", line = 2, column = 10)) { 22 }
        assertEquals(TextRange(13, 22), range)
    }

    @Test
    fun testHugeColumnDoesNotOverflow() {
        val doc = DocumentImpl("flowchart LR\n    A --> B")
        val range = computeRange(doc, MermaidRenderError("error", line = 2, column = Int.MAX_VALUE))
        assertEquals(TextRange(13, 24), range)
    }

    // ── Severity by category ────────────────────────────────────────────

    @Test
    fun testIncompleteIsWeakWarning() {
        assertEquals(HighlightSeverity.WEAK_WARNING, renderErrorSeverity(MermaidRenderErrorCategory.INCOMPLETE))
    }

    @Test
    fun testOtherCategoriesAreWarnings() {
        for (category in MermaidRenderErrorCategory.entries - MermaidRenderErrorCategory.INCOMPLETE) {
            assertEquals(HighlightSeverity.WARNING, renderErrorSeverity(category), "Severity of $category")
        }
    }
}
