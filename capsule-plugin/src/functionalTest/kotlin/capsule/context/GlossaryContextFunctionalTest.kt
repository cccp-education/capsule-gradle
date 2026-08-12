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
 * Functional tests for the `collectCapsuleAugmentedContext` glossary channel
 * extension (CAP-GLOSSARY-2).
 *
 * Verifies that:
 * - `-Pcapsule.context.glossaryFile` resolves a glossary AsciiDoc file
 *   (`== Glossary` section + `- term: definition` bullets) and feeds the
 *   `==== Official Glossary (glossary)` section of the augmented context.
 * - No glossaryFile configured = no glossary section (backward compatible).
 */
class GlossaryContextFunctionalTest {

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

    private fun writeGlossaryFile(): File {
        val glossaryFile = projectDir.resolve("glossary.adoc")
        glossaryFile.writeText(
            """
            = Formation FPA — Glossaire AFNOR/REAC

            == Glossary

            - compétence transversale: capacité mobilisable across contexts, not domain-specific
            - évaluation formative: assessment during learning to improve, not just measure
            - socle de compétences: foundational skill set required before advanced modules

            == Other Section

            - unrelated: this bullet must be ignored by the glossary parser
            """.trimIndent(),
        )
        return glossaryFile
    }

    private fun runner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)

    @Test
    fun `glossaryFile CLI param resolves glossary adoc and feeds the glossary section`() {
        writeBuildFile()
        writeEagerFiles()
        val glossaryFile = writeGlossaryFile()
        val result = runner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.glossaryFile=${glossaryFile.absolutePath}",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertTrue(content.contains("Official Glossary"), "Expected glossary section header")
        assertTrue(content.contains("compétence transversale"), "Expected glossary term from adoc")
        assertTrue(content.contains("évaluation formative"), "Expected second glossary term")
        assertFalse(content.contains("unrelated"), "Non-Glossary section bullet must be ignored")
    }

    @Test
    fun `no glossaryFile configured produces no glossary section`() {
        writeBuildFile()
        writeEagerFiles()
        val result = runner("collectCapsuleAugmentedContext").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":collectCapsuleAugmentedContext")?.outcome)
        val artefact = projectDir.resolve("build/capsule/augmented-context.txt")
        assertTrue(artefact.exists(), "Expected augmented context artefact")
        val content = artefact.readText()
        assertFalse(content.contains("Official Glossary"), "No glossary section when glossaryFile not configured")
    }
}