package capsule.context

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Functional tests for the `collectCapsuleAugmentedContext` scenario channel
 * extension (CAP-SPD-3).
 *
 * Verifies that:
 * - `-Pcapsule.context.scenarioFile` resolves a scenario directory
 *   (metadata.json + companion AsciiDoc) and feeds the scenario section.
 * - No scenarioFile configured = no scenario section (backward compatible).
 */
class ScenarioContextFunctionalTest {

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
        agents.resolve("INDEX.adoc").writeText("= INDEX — Demo\nEPIC DEMO-1 : in progress")
        projectDir.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir.resolve("AGENT.adoc").writeText("Rules : English only, no commits without permission.")
    }

    private fun writeScenarioDir(): File {
        val scenarioDir = projectDir.resolve("scenario").apply { mkdirs() }
        scenarioDir.resolve("metadata.json").writeText(
            """{"source":"producer","type":"SPD","sessions":1,"generatedAt":"2026-08-11T10:00:00Z","model":"convention","version":"1.0","dependencies":[]}""",
        )
        scenarioDir.resolve("001_bienvenue.adoc").writeText(
            """
            = Bienvenue dans la Formation FPA
            :module: accueil

            == Objectifs
            - Comprendre le cadre de la formation
            - Connaître les modalités d'évaluation

            == Prérequis
            - Connaissances de base en programmation

            == Modalités d'évaluation
            QCM final + mise en situation
            """.trimIndent(),
        )
        return scenarioDir
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)

    @Test
    fun `scenarioFile CLI param resolves scenario directory and feeds the scenario section`() {
        writeBuildFile()
        writeEagerFiles()
        val scenarioDir = writeScenarioDir()
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.scenarioFile=${scenarioDir.absolutePath}",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertTrue(content.contains("Pedagogical Scenario"), "Expected scenario section header")
        assertTrue(content.contains("Comprendre le cadre de la formation"), "Expected objective from scenario adoc")
        assertTrue(content.contains("Session: Bienvenue dans la Formation FPA"), "Expected session title")
    }

    @Test
    fun `no scenarioFile configured produces no scenario section`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner("collectCapsuleAugmentedContext").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertFalse(content.contains("Pedagogical Scenario"), "No scenario section when scenarioFile not configured")
    }
}