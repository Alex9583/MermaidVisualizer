package com.alextdev.mermaidvisualizer.markdown

import com.alextdev.mermaidvisualizer.MermaidWebResources
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MermaidIntegrationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/resources/testData"

    fun testProviderCreatesBrowserExtension() {
        val provider = MermaidBrowserExtension.Provider()
        assertNotNull("Provider should create a non-null extension", provider)
    }

    fun testFullResourceServingPipeline() {
        val extension = MermaidBrowserExtension()
        try {
            val scripts = extension.scripts
            assertTrue("scripts should not be empty", scripts.isNotEmpty())

            for (scriptUrl in scripts) {
                val fileName = scriptUrl.substringAfterLast('/')
                assertTrue(
                    "canProvide should return true for script URL: $scriptUrl",
                    extension.canProvide(scriptUrl)
                )
                val resource = extension.loadResource(scriptUrl)
                assertNotNull("loadResource should return non-null for: $fileName", resource)
                assertEquals(
                    "MIME type should be application/javascript for: $fileName",
                    "application/javascript",
                    resource!!.type
                )
            }

            val styles = extension.styles
            assertTrue("styles should not be empty", styles.isNotEmpty())

            for (styleUrl in styles) {
                val fileName = styleUrl.substringAfterLast('/')
                assertTrue(
                    "canProvide should return true for style URL: $styleUrl",
                    extension.canProvide(styleUrl)
                )
                val resource = extension.loadResource(styleUrl)
                assertNotNull("loadResource should return non-null for: $fileName", resource)
                assertEquals(
                    "MIME type should be text/css for: $fileName",
                    "text/css",
                    resource!!.type
                )
            }
        } finally {
            extension.dispose()
        }
    }

    fun testScriptOrderIsCorrect() {
        val extension = MermaidBrowserExtension()
        try {
            val names = extension.scripts.map { it.substringAfterLast('/') }
            // Library first, then the generated shadow CSS and config scripts the core reads at load time,
            // then the core modules in dependency order, the zoom module and finally the adapter.
            assertEquals("mermaid.min.js", names.first())
            assertEquals("mermaid-shadow-css-init.js", names[1])
            assertEquals("mermaid-config-init.js", names[2])
            assertEquals(MermaidWebResources.CORE_SCRIPTS, names.subList(3, 3 + MermaidWebResources.CORE_SCRIPTS.size))
            assertEquals("mermaid-zoom.js", names[names.size - 2])
            assertEquals("mermaid-render.js", names.last())
            assertEquals(3 + MermaidWebResources.MARKDOWN_SCRIPTS.size, names.size)
        } finally {
            extension.dispose()
        }
    }

    fun testExtensionLifecycle() {
        // First instance
        val ext1 = MermaidBrowserExtension()
        assertTrue("First instance scripts should not be empty", ext1.scripts.isNotEmpty())
        val resource1 = ext1.loadResource(ext1.scripts[0])
        assertNotNull("First instance should load resources", resource1)
        ext1.dispose()

        // Second instance after dispose — resources should still be available
        val ext2 = MermaidBrowserExtension()
        assertTrue("Second instance scripts should not be empty", ext2.scripts.isNotEmpty())
        val resource2 = ext2.loadResource(ext2.scripts[0])
        assertNotNull("Second instance should load resources after first was disposed", resource2)
        ext2.dispose()
    }

    fun testAllDiagramsFileIsParseable() {
        val psiFile = myFixture.configureByFile("all-diagrams.md")
        assertNotNull("all-diagrams.md should be loaded as a PsiFile", psiFile)
        assertTrue(
            "File should contain substantial content",
            psiFile.text.length > 1000
        )
        assertTrue(
            "File should contain mermaid code blocks",
            psiFile.text.contains("```mermaid")
        )
    }
}
