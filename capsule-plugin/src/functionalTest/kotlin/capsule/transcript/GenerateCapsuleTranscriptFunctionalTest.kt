package capsule.transcript

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.assertTrue

/**
 * Functional tests for the `generateCapsuleTranscript` Gradle task
 * (CAP-TRANSCRIPT US-4).
 *
 * Verifies:
 *  - task registration with the `generate` group
 *  - TEMPLATE strategy produces a deterministic AsciiDoc article from
 *    speaker notes (no LLM call)
 *  - LLM strategy enriches the template via a mock Ollama HTTP server
 *
 * The LLM mock routes on the prompt body: a transcript enhancement
 * request returns enriched AsciiDoc. Zero network, zero LLM pool.
 */
class GenerateCapsuleTranscriptFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun startMockLlm(): Pair<Int, () -> Int> {
        var calls = 0
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val port = server.address.port
        val enriched = """
            = demo

            :language: fr

            This article is the transcript of the training capsule.

            == Introduction

            Enriched intro with pedagogical transition.

            == Details

            Enriched details narration.
        """.trimIndent()
        server.createContext("/api/chat") { exchange ->
            calls++
            val escaped = enriched
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
        return port to { calls }
    }

    private fun writeBuildFile() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    private fun writeSpeakerNotes() {
        val notesDir = projectDir.resolve("build").resolve("capsule")
        notesDir.mkdirs()
        notesDir.resolve("demo-speaker-notes.adoc").writeText("""
            == Introduction

            [NOTE.speaker]
            --
            Base intro narration.
            --

            == Details

            [NOTE.speaker]
            --
            Base details narration.
            --
        """.trimIndent())
    }

    @Test
    fun `generateCapsuleTranscript task is registered in the generate group`() {
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "generate")
            .withProjectDir(projectDir)
            .build()
        assertTrue(
            result.output.contains("generateCapsuleTranscript"),
            "Expected generateCapsuleTranscript in generate group, got: ${result.output}"
        )
    }

    @Test
    fun `TEMPLATE strategy produces a deterministic AsciiDoc article from speaker notes`() {
        writeBuildFile()
        writeSpeakerNotes()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateCapsuleTranscript",
                "-x",
                "generateCapsuleContent",
                "-Pcapsule.transcript.enabled=true",
                "-Pcapsule.transcript.strategy=template",
            )
            .withProjectDir(projectDir)
            .build()

        val transcript = projectDir.resolve("build").resolve("capsule")
            .resolve("demo-transcript.adoc")
        assertTrue(transcript.exists(), "Expected transcript output, got: ${projectDir.resolve("build").resolve("capsule").listFiles()?.joinToString { it.name }}")
        val content = transcript.readText()
        assertTrue(content.contains("= demo"), "Expected level-0 title, got: $content")
        assertTrue(content.contains(":language:"), "Expected language attribute, got: $content")
        assertTrue(content.contains("== Introduction"), "Expected Introduction section, got: $content")
        assertTrue(content.contains("Base intro narration"), "Expected speaker note body, got: $content")
        assertTrue(content.contains("== Details"), "Expected Details section, got: $content")
        assertTrue(content.contains("End of the capsule transcript"), "Expected outro boilerplate, got: $content")
        assertTrue(result.output.contains("CAPSULE TRANSCRIPT"), "Expected summary log, got: ${result.output}")
    }

    @Test
    fun `LLM strategy enriches the template article via a mock Ollama server`() {
        val (port, calls) = startMockLlm()
        writeBuildFile()
        writeSpeakerNotes()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateCapsuleTranscript",
                "-x",
                "generateCapsuleContent",
                "-Pcapsule.transcript.enabled=true",
                "-Pcapsule.transcript.strategy=llm",
                "-Pollama.baseUrl=http://localhost:$port",
            )
            .withProjectDir(projectDir)
            .build()

        val transcript = projectDir.resolve("build").resolve("capsule")
            .resolve("demo-transcript.adoc")
        assertTrue(transcript.exists(), "Expected transcript output")
        val content = transcript.readText()
        assertTrue(content.contains("Enriched intro with pedagogical transition"), "Expected LLM-enriched content, got: $content")
        assertTrue(calls() >= 1, "Expected at least one LLM call, got ${calls()}")
    }
}