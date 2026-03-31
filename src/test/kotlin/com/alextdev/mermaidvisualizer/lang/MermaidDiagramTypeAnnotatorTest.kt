package com.alextdev.mermaidvisualizer.lang

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidDiagramTypeAnnotatorTest : BasePlatformTestCase() {

    fun testMisspelledFlowcharProducesError() {
        myFixture.configureByText("test.mmd", "flowchar LR\n    A --> B")
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val annotation = highlights.firstOrNull { it.description?.contains("Did you mean") == true }
        assertNotNull("Should produce a 'Did you mean' annotation for 'flowchar'", annotation)
        assertTrue("Should suggest flowchart", annotation!!.description.contains("flowchart"))
    }

    fun testMisspelledSequencDiagramProducesError() {
        // "sequencDiagram" (missing 'e') is distance 1 from "sequenceDiagram"
        myFixture.configureByText("test.mmd", "sequencDiagram\n    Alice ->> Bob: Hello")
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val annotation = highlights.firstOrNull { it.description?.contains("Did you mean") == true }
        assertNotNull("Should produce a 'Did you mean' annotation for 'sequencDiagram'", annotation)
        assertTrue("Should suggest sequenceDiagram", annotation!!.description.contains("sequenceDiagram"))
    }

    fun testDistantTypoSuppressed() {
        myFixture.configureByText("test.mmd", "zzzzzzzzz\n    A --> B")
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val customAnnotations = highlights.filter {
            it.description?.contains("Did you mean") == true
        }
        assertTrue("Distant typo should not produce diagram type suggestion", customAnnotations.isEmpty())
    }

    fun testQuickFixReplacesDiagramType() {
        myFixture.configureByText("test.mmd", "flowchar LR\n    A --> B")
        val fixes = myFixture.getAllQuickFixes()
        val replaceFix = fixes.firstOrNull { it.text.contains("flowchart") }
        assertNotNull("Should have a fix suggesting flowchart", replaceFix)
        myFixture.launchAction(replaceFix!!)
        val text = myFixture.editor.document.text
        assertTrue("Document should contain 'flowchart' after fix", text.startsWith("flowchart"))
    }

    fun testQuickFixForGrap() {
        myFixture.configureByText("test.mmd", "grap TD\n    A --> B")
        val fixes = myFixture.getAllQuickFixes()
        val graphFix = fixes.firstOrNull { it.text.contains("graph") }
        assertNotNull("Should suggest 'graph'", graphFix)
        myFixture.launchAction(graphFix!!)
        assertTrue("Document should start with 'graph'", myFixture.editor.document.text.startsWith("graph"))
    }

    fun testValidFlowchartNoAnnotation() {
        myFixture.configureByText("test.mmd", "flowchart LR\n    A --> B")
        val highlights = myFixture.doHighlighting(HighlightSeverity.ERROR)
        val customAnnotations = highlights.filter {
            it.description?.contains("Did you mean") == true
        }
        assertTrue("Valid diagram type should not trigger annotation", customAnnotations.isEmpty())
    }

    fun testMultipleSuggestions() {
        myFixture.configureByText("test.mmd", "classDiagra\n    class Animal")
        val fixes = myFixture.getAllQuickFixes()
        assertTrue("Should have at least one fix", fixes.isNotEmpty())
        assertTrue("Should suggest classDiagram", fixes.any { it.text.contains("classDiagram") })
    }
}
