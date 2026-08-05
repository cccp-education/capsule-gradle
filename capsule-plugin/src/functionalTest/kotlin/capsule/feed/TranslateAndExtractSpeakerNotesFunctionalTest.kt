package capsule.feed

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class TranslateAndExtractSpeakerNotesFunctionalTest {

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

    @Test
    fun `translateAndExtractSpeakerNotes task is registered with the capsule group`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("translateAndExtractSpeakerNotes"))
        assertTrue(output.contains("capsule"))
    }

    @Test
    fun `translateAndExtractSpeakerNotes description mentions both sub-tasks`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        // The composite task description must mention both sub-tasks.
        assertTrue(output.contains("translateDeck"))
        assertTrue(output.contains("extractSpeakerNotes"))
    }
}