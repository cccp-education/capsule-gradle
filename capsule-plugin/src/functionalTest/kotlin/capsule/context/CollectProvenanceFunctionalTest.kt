package capsule.context

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `collectCapsuleAugmentedContext` provenance
 * artefact (CAP-PROVENANCE US-2).
 *
 * Verifies that the task writes the `context-provenance.json` artefact with
 * the EAGER / RAG / Graphify / Docs / Scenario channels that survived the
 * token budget, and that an empty context yields an empty provenance.
 */
class CollectProvenanceFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val mapper: ObjectMapper = ObjectMapper()

    private fun writeBuildFile() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    private fun writeEagerFiles() {
        val agents = projectDir.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText("""
            = INDEX — Demo consumer
            EPIC DEMO-1 : in progress
        """.trimIndent())
        projectDir.resolve("PROMPT_REPRISE.adoc").writeText(
            "Mission : generate a demo capsule.",
        )
        projectDir.resolve("AGENT.adoc").writeText(
            "Rules : English only, no commits without permission.",
        )
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)

    private fun provenance(): JsonNode {
        val file = projectDir.resolve("build/capsule/context-provenance.json")
        assertTrue(file.exists(), "Expected provenance artefact")
        return mapper.readTree(file.readText())
    }

    @Test
    fun `collectCapsuleAugmentedContext writes a provenance JSON with the surviving channels`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=similarity: 0.98 — RAG nugget about grading",
            "-Pcontext.graphifyContent=node: capsule -> edge: feeds -> node: video",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        assertTrue(result.output.contains("CAPSULE PROVENANCE"), "Expected provenance log line")

        val json = provenance()
        val channels = json["channels"]
        assertTrue(channels.isArray, "channels must be an array")
        assertEquals(3, channels.size(), "EAGER + RAG + Graphify must be tracked")

        val eager = channels.first { it["channel"].asText() == "EAGER" }
        assertEquals(3, eager["sources"].size(), "EAGER has 3 governance files")
        assertTrue(
            eager["sources"].all { it["fileName"].asText().endsWith(".adoc") },
            "EAGER source file names must be the governance files",
        )
        assertTrue(
            eager["sources"].all { it["chars"].asInt() > 0 && it["tokens"].asInt() > 0 },
            "EAGER sources must carry measured chars + estimated tokens",
        )

        val rag = channels.first { it["channel"].asText() == "RAG" }
        assertEquals("rag-injected", rag["sources"][0]["fileName"].asText())

        val graphify = channels.first { it["channel"].asText() == "GRAPHIFY" }
        assertEquals("graphify-injected", graphify["sources"][0]["fileName"].asText())

        assertTrue(json["totalChars"].asInt() > 0)
        assertTrue(json["totalTokens"].asInt() > 0)
    }

    @Test
    fun `collectCapsuleAugmentedContext writes an empty provenance for an empty context`() {
        writeBuildFile()
        runner("collectCapsuleAugmentedContext").build()
        val json = provenance()
        assertEquals(0, json["channels"].size())
        assertEquals(0, json["totalChars"].asInt())
        assertEquals(0, json["totalTokens"].asInt())
    }
}
