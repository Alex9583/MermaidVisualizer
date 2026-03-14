package com.alextdev.mermaidvisualizer.markdown

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
            val scripts = extension.scripts
            assertEquals("Should have exactly 5 scripts", 5, scripts.size)

            val first = scripts[0].substringAfterLast('/')
            val second = scripts[1].substringAfterLast('/')
            val third = scripts[2].substringAfterLast('/')
            val fourth = scripts[3].substringAfterLast('/')
            val fifth = scripts[4].substringAfterLast('/')

            assertEquals(
                "mermaid.min.js must be loaded first (library before bootstrap)",
                "mermaid.min.js",
                first
            )
            assertEquals(
                "mermaid-shadow-css-init.js must be loaded second (shadow CSS before render)",
                "mermaid-shadow-css-init.js",
                second
            )
            assertEquals(
                "mermaid-config-init.js must be loaded third (config before render)",
                "mermaid-config-init.js",
                third
            )
            assertEquals(
                "mermaid-zoom.js must be loaded fourth (zoom module before render)",
                "mermaid-zoom.js",
                fourth
            )
            assertEquals(
                "mermaid-render.js must be loaded fifth (bootstrap after library, CSS, config, and zoom)",
                "mermaid-render.js",
                fifth
            )
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
