package com.alextdev.mermaidvisualizer.lang

import com.alextdev.mermaidvisualizer.editor.MermaidRenderError
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

/**
 * Tests for [MermaidRenderExternalAnnotator.computeAnnotationRange] using reflection
 * since the method is private.
 */
class MermaidRenderExternalAnnotatorTest {

    private val annotator = MermaidRenderExternalAnnotator()

    private val computeAnnotationRange: Method = MermaidRenderExternalAnnotator::class.java
        .getDeclaredMethod("computeAnnotationRange", Document::class.java, MermaidRenderError::class.java)
        .also { it.isAccessible = true }

    private fun computeRange(document: Document, error: MermaidRenderError): TextRange {
        return computeAnnotationRange.invoke(annotator, document, error) as TextRange
    }

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
}
