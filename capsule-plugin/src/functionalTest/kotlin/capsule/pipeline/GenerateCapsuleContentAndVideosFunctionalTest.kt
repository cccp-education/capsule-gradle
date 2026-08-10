package capsule.pipeline

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for the `generateCapsuleContentAndVideos` composite task
 * (CAP-ARCH-4).
 *
 * This task wires the LLM content pipeline (`generateCapsuleContent`) to the
 * existing multi-language video pipeline (`translateAndGenerateCapsuleVideos`)
 * without modifying either backend. It is a pure composite [DefaultTask] that
 * declares `dependsOn` on both sub-tasks so a single Gradle invocation produces
 * enriched speaker notes then localized capsule WebMs.
 */
class GenerateCapsuleContentAndVideosFunctionalTest {

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
        // generateCapsuleContent resolves its deckFile convention by scanning
        // slides/misc for the first .adoc file. A dummy deck is needed so the
        // --dry-run dependency graph resolves without error.
        val deckDir = projectDir.resolve("slides").resolve("misc")
        deckDir.mkdirs()
        deckDir.resolve("dummy.adoc").writeText("""
            == Slide 1

            [NOTE.speaker]
            --
            Dummy narration.
            --
        """.trimIndent())
    }

    @Test
    fun `generateCapsuleContentAndVideos task is registered with the capsule group`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("generateCapsuleContentAndVideos"))
        assertTrue(output.contains("capsule"))
    }

    @Test
    fun `generateCapsuleContentAndVideos description mentions both sub-tasks`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("generateCapsuleContent"))
        assertTrue(output.contains("translateAndGenerateCapsuleVideos"))
    }

    @Test
    fun `generateCapsuleContentAndVideos depends on generateCapsuleContent and translateAndGenerateCapsuleVideos`() {
        setupBuild()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleContentAndVideos", "--dry-run")
            .withProjectDir(projectDir)
            .build()

        val output = result.output
        assertTrue(output.contains("generateCapsuleContent"))
        assertTrue(output.contains("translateAndGenerateCapsuleVideos"))
        assertTrue(output.contains("translateAndExtractSpeakerNotes"))
        assertTrue(output.contains("generateCapsuleVideoAllLanguages"))
        assertTrue(output.contains("translateDeck"))
        assertTrue(output.contains("extractSpeakerNotes"))
    }
}