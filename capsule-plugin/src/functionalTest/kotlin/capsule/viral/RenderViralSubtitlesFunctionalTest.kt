package capsule.viral

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `renderViralSubtitles` Gradle task (CAP-VIRAL-E2E US-3).
 *
 * Verifies:
 *  - task registration with the `generate` group
 *  - disabled-by-default no-op skip (economy of ink)
 *  - enabled + storyboard → one `<variant.id>.ass` karaoke caption per variant
 *  - already-present captions are skipped (economy of ink)
 *  - a missing storyboard warns (degraded, non-blocking)
 *
 * Pure text: no Chromium, no FFmpeg.
 */
class RenderViralSubtitlesFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun setupBuild() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            """.trimIndent(),
        )
    }

    private fun writeStoryboard() {
        val dir = projectDir.resolve("build/capsule/viral").also { it.mkdirs() }
        dir.resolve("storyboard.adoc").writeText(
            """
            = Storyboard — Devenir développeur professionnel
            :platform: TIKTOK
            :language: fr
            :target-duration: 30

            == Angle

            Le métier en 30 secondes

            == Beats

            === Beat 1 — hook

            Intent: Et si 30 secondes suffisaient ?

            Duration: 3.0s
            Render: HTML

            === Beat 2 — cta

            Intent: Découvrez Content

            Duration: 5.0s
            Render: HTML
            """.trimIndent(),
        )
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments(*args, "-x", "generateViralCampaign")
        .withProjectDir(projectDir)

    @Test
    fun `renderViralSubtitles task is registered in the generate group`() {
        setupBuild()
        val result = runner("tasks", "--group", "generate").build()
        assertTrue(
            result.output.contains("renderViralSubtitles"),
            "Expected renderViralSubtitles in generate group, got: ${result.output}",
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = runner("renderViralSubtitles").build()
        assertTrue(
            result.output.contains("viralEnabled=false") || result.output.contains("skipped"),
            "Expected skip log when viralEnabled defaults to false, got: ${result.output}",
        )
    }

    @Test
    fun `writes one karaoke caption per variant`() {
        setupBuild()
        writeStoryboard()

        val result = runner(
            "renderViralSubtitles",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr,en",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralSubtitles")?.outcome)
        val dir = projectDir.resolve("build/capsule/viral/videos")
        for (id in listOf("fr-tiktok-30", "en-tiktok-30")) {
            val ass = dir.resolve("$id.ass")
            assertTrue(ass.exists(), "Expected $id.ass, got: ${dir.listFiles()?.joinToString { it.name }}")
            val content = ass.readText()
            assertTrue(content.contains("[Script Info]"), "Expected an ASS document, got: $content")
            assertTrue(content.contains("Dialogue:") && content.contains("\\k"), "Expected karaoke dialogue, got: $content")
            assertTrue(content.contains("Et") && content.contains("suffisaient"), "Expected the beat words")
        }
    }

    @Test
    fun `already present captions are skipped by the economy of ink`() {
        setupBuild()
        writeStoryboard()
        val dir = projectDir.resolve("build/capsule/viral/videos").also { it.mkdirs() }
        dir.resolve("fr-tiktok-30.ass").writeText("existing")

        val result = runner(
            "renderViralSubtitles",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralSubtitles")?.outcome)
        assertEquals("existing", dir.resolve("fr-tiktok-30.ass").readText())
        assertTrue(result.output.contains("1 skipped"), "Expected the skip count, got: ${result.output}")
    }

    @Test
    fun `a missing storyboard warns when enabled`() {
        setupBuild()
        val result = runner(
            "renderViralSubtitles",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralSubtitles")?.outcome)
        assertTrue(
            result.output.contains("no storyboard"),
            "Expected a degraded warning on the missing storyboard, got: ${result.output}",
        )
    }
}
