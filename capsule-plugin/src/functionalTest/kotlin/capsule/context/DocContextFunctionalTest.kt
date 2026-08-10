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
 * Functional tests for the `collectCapsuleAugmentedContext` Docs channel
 * extension (CAP-DOCCONTEXT-3).
 *
 * Verifies that:
 * - Default param CLI `-Pcontext.docsContent` still works (rétrocompat).
 * - `context.docsGlobs` config resolves files and feeds the Docs channel.
 * - Empty globs + empty docsContent = no-op (Docs channel dropped).
 */
class DocContextFunctionalTest {

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

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)

    @Test
    fun `docsContent CLI param still works when no globs configured (retrocompat)`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.docsContent=AFNOR grading manual content via CLI",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertTrue(content.contains("CONTEXTE_DOCS"), "Expected Docs section header")
        assertTrue(content.contains("AFNOR grading manual content via CLI"), "Expected CLI docsContent")
    }

    @Test
    fun `docsGlobs resolves files and feeds the Docs channel`() {
        writeBuildFile()
        writeEagerFiles()
        val docsDir = projectDir.resolve("docs/afnor").apply { mkdirs() }
        docsDir.resolve("grading-rules.adoc").writeText("AFNOR grading rules for FPA formation.")
        docsDir.resolve("evaluation-criteria.adoc").writeText("Evaluation criteria for competency blocks.")
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.docsGlobs=docs/afnor/**/*.adoc",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertTrue(content.contains("CONTEXTE_DOCS"), "Expected Docs section header from globs")
        assertTrue(content.contains("AFNOR grading rules"), "Expected first glob file content")
        assertTrue(content.contains("Evaluation criteria"), "Expected second glob file content")
    }

    @Test
    fun `empty globs and empty docsContent produce no Docs channel`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner("collectCapsuleAugmentedContext").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertFalse(content.contains("CONTEXTE_DOCS"), "No Docs section when globs and docsContent empty")
    }
}