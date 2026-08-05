package capsule.feed

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class ExtractSpeakerNotesFunctionalTest {

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
        """.trimIndent())
    }

    private fun writeDeck(name: String, content: String) {
        val miscDir = projectDir.resolve("slides/misc").apply { mkdirs() }
        miscDir.resolve("$name.adoc").writeText(content.trimIndent())
    }

    @Test
    fun `extractSpeakerNotes writes a script file for a deck with speaker notes`() {
        setupBuild()
        writeDeck("demo", """
            = Demo Deck

            == Intro

            [NOTE.speaker]
            --
            Welcome to the demo.
            --

            == Topic

            [NOTE.speaker]
            --
            Deep dive on the topic.
            --
        """)

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("extractSpeakerNotes")
            .withProjectDir(projectDir)
            .build()

        val scriptFile = projectDir.resolve("build/capsule/demo-script.txt")
        assertTrue(scriptFile.exists(), "Expected script file: ${scriptFile.absolutePath}")
        val content = scriptFile.readText()
        assertTrue(content.startsWith("=== CAPSULE SCRIPT : demo ==="))
        assertTrue(content.contains("--- SLIDE 1 : Intro ---"))
        assertTrue(content.contains("--- SLIDE 2 : Topic ---"))
        assertTrue(content.contains("Welcome to the demo."))
        assertTrue(content.contains("Deep dive on the topic."))
        assertTrue(result.output.contains("demo-script.txt"))
    }

    @Test
    fun `extractSpeakerNotes skips a deck without speaker notes`() {
        setupBuild()
        writeDeck("noNotes", """
            = No Notes Deck

            == Slide

            No speaker note here.
        """)

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("extractSpeakerNotes")
            .withProjectDir(projectDir)
            .build()

        val scriptFile = projectDir.resolve("build/capsule/noNotes-script.txt")
        assertTrue(!scriptFile.exists(), "No script should be written for a deck without speaker notes")
        assertTrue(result.output.contains("skipping") || result.output.contains("No speaker notes"))
    }

    @Test
    fun `extractSpeakerNotes produces one script per deck when multiple decks are present`() {
        setupBuild()
        writeDeck("deck-a", """
            = Deck A

            == A

            [NOTE.speaker]
            --
            Note A.
            --
        """)
        writeDeck("deck-b", """
            = Deck B

            == B

            [NOTE.speaker]
            --
            Note B.
            --
        """)

        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("extractSpeakerNotes")
            .withProjectDir(projectDir)
            .build()

        val scriptA = projectDir.resolve("build/capsule/deck-a-script.txt")
        val scriptB = projectDir.resolve("build/capsule/deck-b-script.txt")
        assertTrue(scriptA.exists(), "Expected deck-a-script.txt")
        assertTrue(scriptB.exists(), "Expected deck-b-script.txt")
        assertTrue(scriptA.readText().contains("--- SLIDE 1 : A ---"))
        assertTrue(scriptB.readText().contains("--- SLIDE 1 : B ---"))
    }

    @Test
    fun `extractSpeakerNotes completes gracefully when slides_misc is missing`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("extractSpeakerNotes")
            .withProjectDir(projectDir)
            .build()

        // Task runs without error and produces no script files
        val capsuleDir = projectDir.resolve("build/capsule")
        assertTrue(!capsuleDir.exists() || capsuleDir.listFiles().isNullOrEmpty())
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}