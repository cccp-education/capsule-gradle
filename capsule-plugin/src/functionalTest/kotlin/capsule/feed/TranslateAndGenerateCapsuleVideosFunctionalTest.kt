package capsule.feed

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class TranslateAndGenerateCapsuleVideosFunctionalTest {

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
            // slider is not applied in this isolated project; provide a stub so
            // the composite task graph (which depends on slider translateDeck)
            // resolves during --dry-run.
            tasks.register('translateDeck') {
                doLast { logger.lifecycle('FAKE translateDeck') }
            }
        """.trimIndent())
    }

    @Test
    fun `translateAndGenerateCapsuleVideos task is registered with the capsule group`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("translateAndGenerateCapsuleVideos"))
        assertTrue(output.contains("capsule"))
    }

    @Test
    fun `translateAndGenerateCapsuleVideos description mentions both sub-tasks`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        // The composite task description must mention both sub-tasks.
        assertTrue(output.contains("translateAndExtractSpeakerNotes"))
        assertTrue(output.contains("generateCapsuleVideoAllLanguages"))
    }

    @Test
    fun `translateAndGenerateCapsuleVideos depends on translateAndExtractSpeakerNotes and generateCapsuleVideoAllLanguages`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("translateAndGenerateCapsuleVideos", "--dry-run")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("translateAndExtractSpeakerNotes"))
        assertTrue(output.contains("generateCapsuleVideoAllLanguages"))
        assertTrue(output.contains("translateDeck"))
        assertTrue(output.contains("extractSpeakerNotes"))
    }
}
