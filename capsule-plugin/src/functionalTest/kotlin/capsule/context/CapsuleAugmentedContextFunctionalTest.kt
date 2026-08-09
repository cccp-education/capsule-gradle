package capsule.context

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `collectCapsuleAugmentedContext` Gradle task
 * (CAP-ARCH-2c).
 *
 * Verifies the Gradle surface: task registration, artefact production from
 * the consumer EAGER governance files + injected RAG content, and the
 * UP-TO-DATE skip on an unchanged build (law of economy of ink — AGENT.adoc).
 */
class CapsuleAugmentedContextFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

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
        agents.resolve("INDEX.adoc").writeText(
            """
            = INDEX — Demo consumer
            EPIC DEMO-1 : in progress
            """.trimIndent(),
        )
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

    @Test
    fun `collectCapsuleAugmentedContext task is registered`() {
        writeBuildFile()
        val result = runner("tasks", "--all", "--quiet").build()
        assertTrue(
            result.output.contains("collectCapsuleAugmentedContext"),
            "Expected task in tasks list",
        )
    }

    @Test
    fun `collectCapsuleAugmentedContext produces the augmented context artefact`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=similarity: 0.98 — RAG nugget about grading",
            "-Pcontext.graphifyContent=node: capsule -> edge: feeds -> node: video",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        assertTrue(result.output.contains("CAPSULE CONTEXT"), "Expected summary log line")

        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertTrue(content.contains("RÈGLES_EAGER"), "Expected EAGER section header")
        assertTrue(content.contains("INDEX.adoc"), "Expected EAGER governance file content")
        assertTrue(content.contains("CONTEXTE_RAG"), "Expected RAG section header")
        assertTrue(content.contains("RAG nugget about grading"), "Expected injected RAG content")
        assertTrue(content.contains("RELATIONS_GRAPHIFY"), "Expected Graphify section header")
    }

    @Test
    fun `collectCapsuleAugmentedContext is up-to-date when inputs are unchanged`() {
        writeBuildFile()
        writeEagerFiles()
        runner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=unchanged",
        ).build()
        val second = runner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=unchanged",
        ).build()
        assertEquals(
            TaskOutcome.UP_TO_DATE,
            second.task(":collectCapsuleAugmentedContext")?.outcome,
            "Second run must skip the work (economy of ink)",
        )
    }
}
