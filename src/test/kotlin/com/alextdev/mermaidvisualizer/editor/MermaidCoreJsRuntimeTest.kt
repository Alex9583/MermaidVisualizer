package com.alextdev.mermaidvisualizer.editor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.opentest4j.TestAbortedException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val JS_TEST_DIR = "src/test/js"
private const val NODE_TIMEOUT_SECONDS = 60L

/**
 * Runs the JavaScript runtime tests of `src/test/js` (node:test, mermaid-core.js and the Mermaid bundle loaded
 * in node:vm sandboxes) as part of `./gradlew test`. Without Node.js on the PATH the test is skipped on a
 * developer machine but fails on CI (`CI=true`, set by GitHub Actions), so that coverage is never lost silently.
 * The test files are passed explicitly: `node --test <dir>` does not search a directory on Node 22+.
 */
class MermaidCoreJsRuntimeTest {

    @Test
    fun `mermaid-core JavaScript runtime tests pass under Node`() {
        val node = nodeExecutable() ?: skipOrFailWithoutNode()

        val testFiles = File(JS_TEST_DIR).listFiles { file -> file.name.endsWith(".test.mjs") }
            ?.sortedBy { it.name }
            .orEmpty()
        assertTrue(testFiles.isNotEmpty(), "No *.test.mjs file found in $JS_TEST_DIR (working dir: ${File(".").absolutePath})")

        val command = listOf(node, "--test") + testFiles.map { it.path }
        val result = run(command)
        assertEquals(0, result.exitCode) { "`${command.joinToString(" ")}` failed:\n${result.output}" }
    }

    private fun skipOrFailWithoutNode(): Nothing {
        val message = "Node.js not found on the PATH"
        if (System.getenv("CI").equals("true", ignoreCase = true)) {
            fail<Nothing>("$message: the JavaScript runtime tests must run on CI (CI=true), install Node.js on the runner")
        }
        throw TestAbortedException("$message: JavaScript runtime tests skipped")
    }

    private fun nodeExecutable(): String? = try {
        if (run(listOf("node", "--version")).exitCode == 0) "node" else null
    } catch (e: IOException) {
        null
    }

    private fun run(command: List<String>): ProcessResult {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = StringBuilder()
        // Drain the output concurrently so that a verbose run cannot block on a full pipe.
        val reader = thread(name = "node-output-reader") {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { output.appendLine(it) } }
        }
        if (!process.waitFor(NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            reader.join(TimeUnit.SECONDS.toMillis(5))
            fail<Unit>("`${command.joinToString(" ")}` timed out after ${NODE_TIMEOUT_SECONDS}s:\n$output")
        }
        reader.join(TimeUnit.SECONDS.toMillis(5))
        return ProcessResult(process.exitValue(), output.toString())
    }

    private data class ProcessResult(val exitCode: Int, val output: String)
}
