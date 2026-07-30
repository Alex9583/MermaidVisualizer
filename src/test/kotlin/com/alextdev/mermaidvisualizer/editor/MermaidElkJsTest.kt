package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MermaidElkJsTest {

    private lateinit var jsContent: String

    @BeforeAll
    fun loadResources() {
        val jsStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-elk.js")
        ) { "web/mermaid-elk.js should be on the classpath" }
        jsContent = jsStream.use { it.reader(Charsets.UTF_8).readText() }
    }

    @Test
    fun `elk js registers layout loaders on the mermaid global`() {
        assertTrue(
            jsContent.contains("globalThis.mermaid.registerLayoutLoaders("),
            "Should call registerLayoutLoaders on the mermaid global"
        )
    }

    @Test
    fun `elk js registers all documented elk layout names`() {
        for (name in listOf("elk", "elk.stress", "elk.force", "elk.mrtree", "elk.sporeOverlap")) {
            assertTrue(
                jsContent.contains("{name:\"$name\","),
                "Should register the '$name' layout"
            )
        }
    }

    @Test
    fun `elk js maps the elk name to the layered algorithm`() {
        assertTrue(
            jsContent.contains("{name:\"elk\",loader:__elkLoader,algorithm:\"elk.layered\"}"),
            "The 'elk' layout name should map to the elk.layered algorithm"
        )
    }

    @Test
    fun `elk js is a classic script without ESM module syntax`() {
        assertFalse(
            Regex("""(?m)^import\{""").containsMatchIn(jsContent),
            "Converted script must not contain ESM import statements"
        )
        assertFalse(
            jsContent.contains("from\"./chunks/"),
            "Converted script must not reference ESM chunk files"
        )
        assertFalse(
            Regex("""export\{[^{}]*\};\s*$""").containsMatchIn(jsContent),
            "Converted script must not contain trailing ESM export statements"
        )
    }

    @Test
    fun `elk js is safe to inline in a script tag`() {
        assertFalse(
            jsContent.contains("</script", ignoreCase = true),
            "Must not contain a closing script tag (breaks standalone HTML inlining)"
        )
    }

    @Test
    fun `elk js guards registration against a missing mermaid global`() {
        assertTrue(
            jsContent.contains("typeof globalThis.mermaid.registerLayoutLoaders===\"function\""),
            "Should check registerLayoutLoaders availability before registering"
        )
        assertTrue(
            jsContent.contains("catch(e)"),
            "Should catch initialization failures so other scripts keep working"
        )
    }

    @Test
    fun `elk js bundles the elkjs engine`() {
        assertTrue(
            jsContent.contains("elk.layered"),
            "Should contain the layered algorithm identifier"
        )
        assertTrue(jsContent.length > 1_000_000, "Bundled ELK engine should be >1MB")
    }

    @Test
    fun `elk version file matches the version in the generated header`() {
        val versionStream = requireNotNull(
            javaClass.classLoader.getResourceAsStream("web/mermaid-elk.version")
        ) { "web/mermaid-elk.version should be on the classpath" }
        val version = versionStream.use { it.reader(Charsets.UTF_8).readText() }.trim()
        assertTrue(version.matches(Regex("""\d+\.\d+\.\d+.*""")), "Version file should contain a semver ($version)")

        val headerVersion = Regex("""mermaid-elk\.js v(\S+)""").find(jsContent)?.groupValues?.get(1)
        assertNotNull(headerVersion, "Generated file should declare its version in the header comment")
        assertEquals(version, headerVersion, "Header version should match mermaid-elk.version")
    }
}
