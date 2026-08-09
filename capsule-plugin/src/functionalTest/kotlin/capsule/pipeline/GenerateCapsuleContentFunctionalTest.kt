package capsule.pipeline

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.assertTrue

/**
 * Functional tests for the `generateCapsuleContent` Gradle task (CAP-ARCH-3).
 *
 * The task orchestrates the koog [CapsulePipelineGraph] through a real
 * langchain4j ChatModel wired to a mock Ollama-compatible HTTP server
 * (`-Pollama.baseUrl`): propose-context → validate-context →
 * generate-speaker-notes, then writes the enriched speaker notes `.adoc` and
 * the derived TTS script `.txt` to `build/capsule`.
 *
 * The mock routes on the prompt body: a propose request returns the content
 * plan JSON, a generate request returns enriched AsciiDoc. Zero network, zero
 * LLM pool.
 */
class GenerateCapsuleContentFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun startRoutingMockLlm(): Pair<Int, () -> Int> {
        val counters = mutableMapOf("propose" to 0, "generate" to 0)
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val port = server.address.port
        val plan = """
            {
              "deckName": "kotlin-coroutines",
              "languageCode": "fr",
              "slideCount": 2,
              "speakerNotesFile": "kotlin-coroutines-speaker-notes.adoc",
              "ttsScriptFile": "kotlin-coroutines-script.txt"
            }
        """.trimIndent()
        val notes = """
            == Introduction

            [NOTE.speaker]
            --
            Enriched intro narration about coroutines.
            --

            == Details

            [NOTE.speaker]
            --
            Enriched details narration about async.
            --
        """.trimIndent()
        server.createContext("/api/chat") { exchange ->
            val body = exchange.requestBody.bufferedReader().use { it.readText() }
            val responseContent = if (body.contains("Propose a content plan")) {
                counters["propose"] = counters["propose"]!! + 1
                plan
            } else {
                counters["generate"] = counters["generate"]!! + 1
                notes
            }
            val escaped = responseContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
            val response = """
                {
                  "model": "smollm:135m",
                  "message": { "role": "assistant", "content": "$escaped" },
                  "done": true
                }
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.executor = null
        server.start()
        return port to { counters["propose"]!! + counters["generate"]!! }
    }

    private fun writeBuildFile() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Test
    fun `generateCapsuleContent task is registered`() {
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--quiet")
            .withProjectDir(projectDir)
            .build()
        assertTrue(result.output.contains("generateCapsuleContent"), "Expected task in tasks list")
    }

    @Test
    fun `generateCapsuleContent writes enriched speaker notes and TTS script via the koog pipeline`() {
        val (port, calls) = startRoutingMockLlm()
        writeBuildFile()
        val deckDir = projectDir.resolve("slides").resolve("misc")
        deckDir.mkdirs()
        deckDir.resolve("kotlin-coroutines.adoc").writeText("""
            == Slide 1

            [NOTE.speaker]
            --
            Base narration.
            --

            == Slide 2

            [NOTE.speaker]
            --
            More narration.
            --
        """.trimIndent())

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateCapsuleContent",
                "-Pollama.baseUrl=http://localhost:$port",
            )
            .withProjectDir(projectDir)
            .build()

        val notes = projectDir.resolve("build").resolve("capsule")
            .resolve("kotlin-coroutines-speaker-notes.adoc")
        val script = projectDir.resolve("build").resolve("capsule")
            .resolve("kotlin-coroutines-script.txt")
        assertTrue(notes.exists(), "Expected enriched speaker notes output, got ${projectDir.resolve("build").listFiles()?.joinToString { it.name }}")
        assertTrue(script.exists(), "Expected TTS script output")
        assertTrue(notes.readText().contains("Enriched intro narration"), "Speaker notes must contain enriched content")
        assertTrue(result.output.contains("CAPSULE PIPELINE"), "Expected pipeline summary log line")
        assertTrue(calls() >= 2, "Expected at least propose + generate LLM calls, got ${calls()}")
    }

    @Test
    fun `generateCapsuleContent fails with a clear message when no deck is present`() {
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleContent", "-Pollama.baseUrl=http://localhost:1")
            .withProjectDir(projectDir)
            .buildAndFail()
        assertTrue(result.output.contains("No deck found"), "Expected clear deck resolution error")
    }
}
