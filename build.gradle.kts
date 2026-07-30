import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Base64

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.grammarkit") version "2023.3.0.3"
}

group = "com.alextdev"
version = "1.11.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("org.intellij.plugins.markdown")
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
        }

        changeNotes = """
            See <a href="https://github.com/Alex9583/MermaidVisualizer/blob/master/CHANGELOG.md">CHANGELOG.md</a>
        """.trimIndent()
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/gen")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<org.jetbrains.grammarkit.tasks.GenerateParserTask>("generateMermaidParser") {
    group = "mermaid"
    description = "Regenerates the Grammar-Kit parser + PSI sources from Mermaid.bnf"
    sourceFile.set(file("src/main/grammars/Mermaid.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("com/alextdev/mermaidvisualizer/lang/parser/MermaidParser.java")
    pathToPsiRoot.set("com/alextdev/mermaidvisualizer/lang/psi")
    purgeOldFiles.set(true)
}

tasks.register<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateMermaidLexer") {
    group = "mermaid"
    description = "Regenerates the JFlex lexer from Mermaid.flex (depends on generateMermaidParser)"
    dependsOn("generateMermaidParser")
    sourceFile.set(file("src/main/grammars/Mermaid.flex"))
    targetOutputDir.set(file("src/main/gen/com/alextdev/mermaidvisualizer/lang"))
    purgeOldFiles.set(false)
}

tasks.named("compileKotlin") {
    dependsOn("generateMermaidLexer")
}

tasks.named("compileJava") {
    dependsOn("generateMermaidLexer")
}

tasks.register("updateMermaidElk") {
    group = "mermaid"
    description = "Downloads @mermaid-js/layout-elk from npm/jsdelivr, verifies integrity, converts the ESM bundle to a classic script (web/mermaid-elk.js), and updates the version file"

    val webDir = layout.projectDirectory.dir("src/main/resources/web")
    val versionFile = webDir.file("mermaid-elk.version")
    val targetFile = webDir.file("mermaid-elk.js")
    val mermaidVersionFile = webDir.file("mermaid.version")

    doLast {
        fun fetchText(url: String, errorContext: String, accept: String): ByteArray {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", accept)
            try {
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    error("$errorContext returned HTTP $responseCode")
                }
                return conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }

        fun fetchJson(url: String, errorContext: String): String =
            fetchText(url, errorContext, "application/json").toString(Charsets.UTF_8)

        val latestVersion: String
        val npmJson: String
        try {
            npmJson = fetchJson("https://registry.npmjs.org/@mermaid-js/layout-elk/latest", "npm registry")
            latestVersion = Regex(""""version"\s*:\s*"([^"]+)"""").find(npmJson)?.groupValues?.get(1)
                ?: error("Could not parse version from npm registry response: ${npmJson.take(500)}")
        } catch (e: Exception) {
            throw GradleException(
                "Failed to fetch latest @mermaid-js/layout-elk version from npm registry. " +
                "Check your network connection and try again. Error: ${e.message}", e
            )
        }
        println("Latest @mermaid-js/layout-elk version: $latestVersion")

        // Peer compatibility check against the bundled mermaid major version
        val peerRange = Regex(""""peerDependencies"\s*:\s*\{[^}]*"mermaid"\s*:\s*"([^"]+)"""")
            .find(npmJson)?.groupValues?.get(1)
        val bundledMermaid = mermaidVersionFile.asFile.takeIf { it.exists() }?.readText()?.trim()
        if (peerRange != null && bundledMermaid != null) {
            val peerMajor = Regex("""(\d+)""").find(peerRange)?.groupValues?.get(1)
            val bundledMajor = bundledMermaid.substringBefore('.')
            if (peerMajor != null && peerMajor != bundledMajor) {
                throw GradleException(
                    "@mermaid-js/layout-elk $latestVersion requires mermaid $peerRange " +
                    "but the bundled mermaid is v$bundledMermaid. Run updateMermaid first or pin a compatible layout-elk version."
                )
            }
        }

        val vFile = versionFile.asFile
        if (vFile.exists() && vFile.readText().trim() == latestVersion && targetFile.asFile.exists()) {
            println("Already at v$latestVersion, skipping download.")
            return@doLast
        }

        // File hashes from the jsdelivr data API for integrity verification
        val dataJson = try {
            fetchJson(
                "https://data.jsdelivr.com/v1/packages/npm/@mermaid-js/layout-elk@$latestVersion?structure=flat",
                "jsdelivr data API",
            )
        } catch (e: Exception) {
            throw GradleException("Failed to fetch file listing from jsdelivr data API. Error: ${e.message}", e)
        }

        fun downloadVerified(distPath: String): String {
            val expectedHash = Regex(""""name"\s*:\s*"${Regex.escape(distPath)}"\s*,\s*"hash"\s*:\s*"([^"]+)"""")
                .find(dataJson)?.groupValues?.get(1)
                ?: error("Could not find hash for $distPath in jsdelivr data API response")
            val bytes = fetchText(
                "https://cdn.jsdelivr.net/npm/@mermaid-js/layout-elk@$latestVersion$distPath",
                "jsdelivr CDN ($distPath)",
                "application/javascript",
            )
            val actualHash = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(bytes)
            )
            if (actualHash != expectedHash) {
                error(
                    "Integrity check FAILED for $distPath!\n" +
                    "  Expected (SHA-256): $expectedHash\n" +
                    "  Actual   (SHA-256): $actualHash\n" +
                    "This could indicate a supply-chain attack or CDN issue."
                )
            }
            return bytes.toString(Charsets.UTF_8)
        }

        // The esm.min build is an entry + 2 hashed chunks (helper + self-contained render bundle).
        // Chunk file names change on every release, so they are parsed from the entry's imports.
        val entry = downloadVerified("/dist/mermaid-layout-elk.esm.min.mjs")
        val helperRel = Regex("""import\{[^{}]*\}from"(\./chunks/[^"]+)"""").find(entry)?.groupValues?.get(1)
            ?: error("Could not find helper chunk import in entry — the dist layout may have changed, update this task")
        val renderRel = Regex("""import\("(\./chunks/[^"]+)"\)""").find(entry)?.groupValues?.get(1)
            ?: error("Could not find render chunk dynamic import in entry — the dist layout may have changed, update this task")
        fun rel2abs(rel: String) = "/dist/" + rel.removePrefix("./")
        println("Chunks: ${rel2abs(helperRel)}, ${rel2abs(renderRel)}")
        val helperSrc = downloadVerified(rel2abs(helperRel))
        val renderSrc = downloadVerified(rel2abs(renderRel))

        // --- ESM → classic-script conversion ---
        // Each chunk was a separate module scope with minified single-letter names, so each body is
        // wrapped in its own IIFE and the import/export bindings are re-created explicitly.

        // Splits "x as y" / "x" specs of an import/export clause into pairs (first name, alias-or-first).
        fun parseSpecs(clause: String): List<Pair<String, String>> =
            clause.split(',').filter { it.isNotBlank() }.map { spec ->
                val parts = spec.trim().split(Regex("""\s+as\s+"""))
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to parts[0]
            }

        // Removes the trailing export statement, returns (body, exportedName -> localName).
        fun stripExport(src: String, ctx: String): Pair<String, Map<String, String>> {
            val match = Regex("""export\{([^{}]*)\};?\s*$""").find(src)
                ?: error("No trailing export statement found in $ctx — the dist format may have changed, update this task")
            val body = src.removeRange(match.range)
            val exports = parseSpecs(match.groupValues[1]).associate { (local, exported) -> exported to local }
            return body to exports
        }

        val (helperBody, helperExports) = stripExport(helperSrc, "helper chunk")

        val importMatch = Regex("""^import\{([^{}]*)\}from"[^"]+";?""").find(renderSrc)
            ?: error("No leading import statement found in render chunk — the dist format may have changed, update this task")
        val importDecls = parseSpecs(importMatch.groupValues[1])
            .joinToString(",") { (imported, local) -> "$local=__elkExports.$imported" }
        val (renderBody, renderExports) = stripExport(renderSrc.removeRange(importMatch.range), "render chunk")

        if ("render" !in renderExports) {
            error("Render chunk does not export a 'render' function — the dist format may have changed, update this task")
        }
        if (renderBody.contains("./chunk") || Regex("""\bimport\s*\(""").containsMatchIn(renderBody)) {
            error("Render chunk still references other module files after conversion — the dist format may have changed, update this task")
        }

        // Registered layout names, parsed from the entry so future upstream additions are picked up
        val baseAlgorithm = Regex("""name:"elk",loader:\w+,algorithm:"([^"]+)"""").find(entry)?.groupValues?.get(1)
            ?: "elk.layered"
        val extraNames = Regex(""""(elk\.[A-Za-z]+)"""").findAll(entry)
            .map { it.groupValues[1] }.toSortedSet() - baseAlgorithm
        val loaderEntries = (
            listOf("""{name:"elk",loader:__elkLoader,algorithm:"$baseAlgorithm"}""") +
            extraNames.map { """{name:"$it",loader:__elkLoader,algorithm:"$it"}""" }
        ).joinToString(",")
        println("Registering layouts: elk ($baseAlgorithm), ${extraNames.joinToString(", ")}")

        val output = buildString(helperBody.length + renderBody.length + 2048) {
            append("/* mermaid-elk.js v$latestVersion — generated by `./gradlew updateMermaidElk` from ")
            append("@mermaid-js/layout-elk (ESM dist converted to a classic script). Do not edit manually. */\n")
            append("(function(){\n\"use strict\";\ntry{\n")
            append("const __elkExports=(function(){\n")
            append(helperBody)
            append("\nreturn{")
            append(helperExports.entries.joinToString(",") { "${it.key}:${it.value}" })
            append("};})();\n")
            append("const __elkModule=(function(){\n")
            append("const ").append(importDecls).append(";\n")
            append(renderBody)
            append("\nreturn{")
            append(renderExports.entries.joinToString(",") { "${it.key}:${it.value}" })
            append("};})();\n")
            append("const __elkLoader=async function(){return __elkModule;};\n")
            append("if(globalThis.mermaid&&typeof globalThis.mermaid.registerLayoutLoaders===\"function\"){\n")
            append("globalThis.mermaid.registerLayoutLoaders([").append(loaderEntries).append("]);\n")
            append("}else{console.error(\"[MermaidVisualizer] mermaid global not found; ELK layouts not registered\");}\n")
            append("}catch(e){console.error(\"[MermaidVisualizer] Failed to initialize ELK layout engine:\",e);}\n")
            append("})();\n")
        }

        if (output.length < 1_000_000) {
            throw GradleException(
                "Generated mermaid-elk.js is suspiciously small (${output.length} bytes). " +
                "Expected >1MB. The existing file has NOT been modified."
            )
        }
        if (output.contains("</script", ignoreCase = true)) {
            throw GradleException(
                "Generated mermaid-elk.js contains '</script' which would break inline embedding " +
                "in the standalone preview HTML. The existing file has NOT been modified."
            )
        }

        val tFile = targetFile.asFile
        val tmpFile = File(tFile.parentFile, "${tFile.name}.tmp")
        try {
            tmpFile.writeText(output, Charsets.UTF_8)
            try {
                Files.move(
                    tmpFile.toPath(),
                    tFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                println("Atomic move not supported, using standard move")
                Files.move(
                    tmpFile.toPath(),
                    tFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            vFile.writeText(latestVersion)
            println("Updated mermaid-elk.js to v$latestVersion (${tFile.length() / 1024} KB)")
        } catch (e: Exception) {
            tmpFile.delete()
            throw GradleException(
                "Failed to write mermaid-elk.js. The existing file may NOT have been modified. " +
                "Error: ${e.message}", e
            )
        }
    }
}

tasks.register("updateMermaid") {
    group = "mermaid"
    description = "Downloads the latest mermaid.min.js from npm/jsdelivr, verifies integrity, replaces the bundled copy, and updates the version file"

    val webDir = layout.projectDirectory.dir("src/main/resources/web")
    val versionFile = webDir.file("mermaid.version")
    val targetFile = webDir.file("mermaid.min.js")

    doLast {
        fun fetchJson(url: String, errorContext: String): String {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            try {
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    error("$errorContext returned HTTP $responseCode")
                }
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }

        val latestVersion: String
        try {
            val json = fetchJson(
                "https://registry.npmjs.org/mermaid/latest",
                "npm registry",
            )
            val versionMatch = Regex(""""version"\s*:\s*"([^"]+)"""").find(json)
            latestVersion = versionMatch?.groupValues?.get(1)
                ?: error("Could not parse version from npm registry response: ${json.take(500)}")
        } catch (e: Exception) {
            throw GradleException(
                "Failed to fetch latest Mermaid version from npm registry. " +
                "Check your network connection and try again. Error: ${e.message}", e
            )
        }

        println("Latest mermaid version: $latestVersion")

        val vFile = versionFile.asFile
        if (vFile.exists() && vFile.readText().trim() == latestVersion) {
            println("Already at v$latestVersion, skipping download.")
            return@doLast
        }

        // Fetch expected file hash from jsdelivr data API for integrity verification
        val expectedHash: String
        try {
            val dataJson = fetchJson(
                "https://data.jsdelivr.com/v1/packages/npm/mermaid@$latestVersion?structure=flat",
                "jsdelivr data API",
            )
            val hashPattern = Regex(""""name"\s*:\s*"/dist/mermaid\.min\.js"\s*,\s*"hash"\s*:\s*"([^"]+)"""")
            val hashMatch = hashPattern.find(dataJson)
            expectedHash = hashMatch?.groupValues?.get(1)
                ?: error("Could not find hash for /dist/mermaid.min.js in jsdelivr data API response")
        } catch (e: Exception) {
            throw GradleException(
                "Failed to fetch file hash from jsdelivr data API for integrity verification. " +
                "Error: ${e.message}", e
            )
        }
        println("Expected SHA-256 hash: $expectedHash")

        val cdnUrl = URI("https://cdn.jsdelivr.net/npm/mermaid@$latestVersion/dist/mermaid.min.js").toURL()
        val tFile = targetFile.asFile
        val tmpFile = File(tFile.parentFile, "${tFile.name}.tmp")

        try {
            println("Downloading from $cdnUrl ...")
            val conn = cdnUrl.openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            try {
                conn.inputStream.use { input ->
                    tmpFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            tmpFile.delete()
            throw GradleException(
                "Failed to download mermaid.min.js from CDN ($cdnUrl). " +
                "The existing file has NOT been modified. Error: ${e.message}", e
            )
        }

        if (tmpFile.length() < 100_000) {
            val size = tmpFile.length()
            tmpFile.delete()
            throw GradleException(
                "Downloaded file is suspiciously small ($size bytes). " +
                "Expected >100KB for mermaid.min.js. The existing file has NOT been modified."
            )
        }

        // Verify integrity against expected hash from jsdelivr data API
        val digest = MessageDigest.getInstance("SHA-256")
        tmpFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val actualHash = Base64.getEncoder().encodeToString(digest.digest())

        if (actualHash != expectedHash) {
            tmpFile.delete()
            throw GradleException(
                "Integrity check FAILED for mermaid.min.js!\n" +
                "  Expected (SHA-256): $expectedHash\n" +
                "  Actual   (SHA-256): $actualHash\n" +
                "The existing file has NOT been modified. " +
                "This could indicate a supply-chain attack or CDN issue."
            )
        }
        println("Integrity check passed (SHA-256)")

        // Atomic file replacement — version written only after confirmed move
        try {
            try {
                Files.move(
                    tmpFile.toPath(),
                    tFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                println("Atomic move not supported, using standard move")

                Files.move(
                    tmpFile.toPath(),
                    tFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
            vFile.writeText(latestVersion)
            println("Updated mermaid.min.js to v$latestVersion (${tFile.length() / 1024} KB)")
        } catch (e: Exception) {
            tmpFile.delete()
            throw GradleException(
                "Failed to replace mermaid.min.js. The existing file may NOT have been modified. " +
                "Error: ${e.message}", e
            )
        }
    }
}
