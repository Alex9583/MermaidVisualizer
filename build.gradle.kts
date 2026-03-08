import java.net.URI

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "com.alextdev"
version = "1.1.0"

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

tasks.register<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateMermaidLexer") {
    sourceFile.set(file("src/main/grammars/Mermaid.flex"))
    targetOutputDir.set(file("src/main/gen/com/alextdev/mermaidvisualizer/lang"))
    purgeOldFiles.set(true)
}

tasks.named("compileKotlin") {
    dependsOn("generateMermaidLexer")
}

tasks.named("compileJava") {
    dependsOn("generateMermaidLexer")
}

tasks.register("updateMermaid") {
    group = "mermaid"
    description = "Downloads the latest mermaid.min.js from npm/jsdelivr, replaces the bundled copy, and updates the version file"

    val webDir = layout.projectDirectory.dir("src/main/resources/web")
    val versionFile = webDir.file("mermaid.version")
    val targetFile = webDir.file("mermaid.min.js")

    doLast {
        val latestVersion: String
        try {
            val registryUrl = URI("https://registry.npmjs.org/mermaid/latest").toURL()
            val conn = registryUrl.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            try {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val versionMatch = Regex(""""version"\s*:\s*"([^"]+)"""").find(json)
                latestVersion = versionMatch?.groupValues?.get(1)
                    ?: error("Could not parse version from npm registry response: ${json.take(500)}")
            } finally {
                conn.disconnect()
            }
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

        val cdnUrl = URI("https://cdn.jsdelivr.net/npm/mermaid@$latestVersion/dist/mermaid.min.js").toURL()
        val tFile = targetFile.asFile
        val tmpFile = File(tFile.parentFile, "${tFile.name}.tmp")

        try {
            println("Downloading from $cdnUrl ...")
            val conn = cdnUrl.openConnection() as java.net.HttpURLConnection
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

        tmpFile.renameTo(tFile)
        vFile.writeText(latestVersion)
        println("Updated mermaid.min.js to v$latestVersion (${tFile.length() / 1024} KB)")
    }
}
