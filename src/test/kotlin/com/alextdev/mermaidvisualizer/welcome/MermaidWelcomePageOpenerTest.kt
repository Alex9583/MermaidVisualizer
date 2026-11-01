package com.alextdev.mermaidvisualizer.welcome

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Key
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.plugins.markdown.lang.MarkdownFileType

class MermaidWelcomePageOpenerTest : BasePlatformTestCase() {

    private val fileEditorManager get() = FileEditorManager.getInstance(project)

    override fun tearDown() {
        try {
            fileEditorManager.openFiles.forEach { fileEditorManager.closeFile(it) }
        } finally {
            super.tearDown()
        }
    }

    private fun open(version: String = "1.14.0", previousShown: String? = null) =
        openWelcomePage(project, version, previousShown, previewSupported = true)

    fun testOpensMarkdownFile() {
        open()
        val file = fileEditorManager.openFiles.single()
        assertEquals(MarkdownFileType.INSTANCE, file.fileType)
        assertEquals("Mermaid Visualizer – Welcome.md", file.name)
    }

    fun testOpenedFileHasPreviewOnlyLayoutUserData() {
        open()
        val file = fileEditorManager.openFiles.single()
        // The platform key is Kotlin-internal but public in bytecode — reflection is the only test access.
        val layoutKey = TextEditorWithPreview::class.java
            .getField("DEFAULT_LAYOUT_FOR_FILE")
            .get(null) as Key<*>
        assertEquals(TextEditorWithPreview.Layout.SHOW_PREVIEW, file.getUserData(layoutKey))
    }

    fun testContentContainsVersionAndTwoDiagrams() {
        open(version = "1.9.1")
        val text = String(fileEditorManager.openFiles.single().contentsToByteArray(), Charsets.UTF_8)
        assertTrue(text.startsWith("# Welcome to Mermaid Visualizer 1.9.1"))
        assertTrue(text.contains("## What's new in 1.9.1"))
        assertEquals(2, Regex("^```mermaid$", RegexOption.MULTILINE).findAll(text).count())
    }

    fun testFileIsReadOnly() {
        open()
        assertFalse(fileEditorManager.openFiles.single().isWritable)
    }

    fun testOpenTwiceFocusesExistingTab() {
        open()
        val first = fileEditorManager.openFiles.single()
        open()
        assertSame(first, fileEditorManager.openFiles.single())
    }

    fun testReopenAfterCloseCreatesFreshTab() {
        open()
        val first = fileEditorManager.openFiles.single()
        fileEditorManager.closeFile(first)
        open(version = "1.10.0")
        val second = fileEditorManager.openFiles.single()
        assertNotSame(first, second)
        assertTrue(String(second.contentsToByteArray(), Charsets.UTF_8).contains("Mermaid Visualizer 1.10.0"))
    }

    fun testWithoutPreviewSupportNothingIsOpened() {
        openWelcomePage(project, "1.14.0", null, previewSupported = false)
        assertEquals(0, fileEditorManager.openFiles.size)
    }

    fun testCreateWelcomePageFileKeepsContent() {
        val file = createWelcomePageFile("# Hello")
        assertEquals("# Hello", String(file.contentsToByteArray(), Charsets.UTF_8))
        assertEquals(MarkdownFileType.INSTANCE, file.fileType)
    }
}
