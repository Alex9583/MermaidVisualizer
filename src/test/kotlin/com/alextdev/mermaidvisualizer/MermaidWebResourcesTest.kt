package com.alextdev.mermaidvisualizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** The script lists both preview contexts load, and their agreement with the core module graph and the Node tests. */
class MermaidWebResourcesTest {

    private val defineCall = Regex("""window\.__mermaidCore\.define\('([^']+)',\s*\[([^]]*)]""")

    private fun resource(name: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(MermaidWebResources.path(name))
        assertNotNull(stream, "${MermaidWebResources.path(name)} should be on the classpath")
        return stream!!.use { it.reader(Charsets.UTF_8).readText() }
    }

    @Test
    fun `every listed resource exists`() {
        val names = listOf(MermaidWebResources.MERMAID_JS, MermaidWebResources.MERMAID_VERSION, MermaidWebResources.SHADOW_CSS,
            MermaidWebResources.PREVIEW_CSS, MermaidWebResources.STANDALONE_CSS) +
            MermaidWebResources.STANDALONE_SCRIPTS + MermaidWebResources.MARKDOWN_SCRIPTS
        names.toSet().forEach { assertTrue(resource(it).isNotBlank(), "$it should not be empty") }
    }

    @Test
    fun `both contexts load the core, then the zoom module, then their adapter`() {
        assertEquals(MermaidWebResources.CORE_SCRIPTS + listOf("mermaid-zoom.js", "mermaid-standalone.js"), MermaidWebResources.STANDALONE_SCRIPTS)
        assertEquals(MermaidWebResources.CORE_SCRIPTS + listOf("mermaid-zoom.js", "mermaid-render.js"), MermaidWebResources.MARKDOWN_SCRIPTS)
        assertTrue(MermaidWebResources.MARKDOWN_RESOURCE_NAMES.containsAll(MermaidWebResources.MARKDOWN_SCRIPTS))
        assertTrue(MermaidWebResources.SHADOW_CSS_INIT_JS in MermaidWebResources.MARKDOWN_RESOURCE_NAMES)
        assertTrue(MermaidWebResources.CONFIG_INIT_JS in MermaidWebResources.MARKDOWN_RESOURCE_NAMES)
    }

    @Test
    fun `the registry comes first and each core module only depends on modules loaded before it`() {
        val scripts = MermaidWebResources.CORE_SCRIPTS
        assertEquals("mermaid-core.js", scripts.first())
        assertTrue(resource(scripts.first()).contains("window.__mermaidCore = core;"), "mermaid-core.js should create the facade")

        val loaded = mutableListOf<String>()
        for (script in scripts.drop(1)) {
            val content = resource(script)
            val defines = defineCall.findAll(content).toList()
            assertEquals(1, defines.size, "$script should register exactly one module")
            val (name, deps) = defines.single().destructured
            deps.split(',').map { it.trim().trim('\'') }.filter { it.isNotEmpty() }.forEach { dep ->
                assertTrue(dep in loaded, "$script depends on \"$dep\", which must be loaded before it (order: $scripts)")
            }
            loaded += name
        }
        assertEquals(listOf("text", "shadow", "runtime", "export", "errorPositions", "errorLabels", "errors", "banner", "pipeline"), loaded)
    }

    @Test
    fun `the Node test sandbox loads the same core scripts in the same order`() {
        val sandbox = File("src/test/js/mermaid-core-sandbox.mjs")
        assertTrue(sandbox.isFile, "Missing ${sandbox.absolutePath}")
        val array = Regex("""export const CORE_SCRIPTS = \[([^]]*)]""").find(sandbox.readText())
        assertNotNull(array, "mermaid-core-sandbox.mjs should export CORE_SCRIPTS")
        val nodeList = array!!.groupValues[1].split(',').map { it.trim().trim('\'') }.filter { it.isNotEmpty() }
        assertEquals(MermaidWebResources.CORE_SCRIPTS, nodeList)
    }
}
