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
version = "1.5.2"

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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<org.jetbrains.grammarkit.tasks.GenerateParserTask>("generateMermaidParser") {
    sourceFile.set(file("src/main/grammars/Mermaid.bnf"))
    targetRootOutputDir.set(file("src/main/gen"))
    pathToParser.set("com/alextdev/mermaidvisualizer/lang/parser/MermaidParser.java")
    pathToPsiRoot.set("com/alextdev/mermaidvisualizer/lang/psi")
    purgeOldFiles.set(true)
}

tasks.register<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateMermaidLexer") {
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
