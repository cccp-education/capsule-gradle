package capsule.feed

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GenerateCapsuleScriptIntegrationFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private fun setupBuild() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
            }
        """.trimIndent())
    }

    @Test
    fun `generateCapsuleScript depends on extractSpeakerNotes`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleScript", "--dry-run")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("generateCapsuleScript"))
        assertTrue(output.contains("extractSpeakerNotes"))
    }

    @Test
    fun `generateCapsuleScript consumes extractSpeakerNotes output end-to-end`() {
        setupBuild()
        // Write a deck with speaker notes so extractSpeakerNotes produces a script.
        val miscDir = projectDir.resolve("slides/misc").apply { mkdirs() }
        miscDir.resolve("demo-deck.adoc").writeText("""
            = Demo Deck

            == Intro

            [NOTE.speaker]
            --
            Welcome to the demo.
            --
        """.trimIndent())

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleScript")
            .withProjectDir(projectDir)
            .build()

        // extractSpeakerNotes writes build/capsule/demo-deck-script.txt,
        // then generateCapsuleScript reads it and logs the parsed deck name.
        val scriptFile = projectDir.resolve("build/capsule/demo-deck-script.txt")
        assertTrue(scriptFile.exists(), "Expected demo-deck-script.txt produced by extractSpeakerNotes")
        assertTrue(result.output.contains("demo-deck"))
    }
}