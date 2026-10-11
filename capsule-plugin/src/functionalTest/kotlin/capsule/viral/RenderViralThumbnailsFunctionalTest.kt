package capsule.viral

import capsule.test.PlaywrightTestAssumption.assumePlaywrightAvailable
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `renderViralThumbnails` Gradle task (CAP-VIRAL-E2E US-2).
 *
 * Verifies:
 *  - task registration with the `generate` group
 *  - disabled-by-default no-op skip (economy of ink)
 *  - already-present covers are skipped (economy of ink) — no capture needed
 *  - a missing cover HTML warns (degraded, non-blocking)
 *  - the real capture produces `<variant.id>-thumb.png` (gated on Chromium)
 */
class RenderViralThumbnailsFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun setupBuild(extraConfig: String = "") {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            $extraConfig
            """.trimIndent(),
        )
    }

    private fun writeCover() {
        val dir = projectDir.resolve("build/capsule/viral").also { it.mkdirs() }
        dir.resolve("viral-thumbnail.html").writeText(
            "<!DOCTYPE html><html><body><section class=\"cover\">Et si 30 secondes suffisaient ?</section></body></html>",
        )
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments(*args, "-x", "generateViralCampaign")
        .withProjectDir(projectDir)

    @Test
    fun `renderViralThumbnails task is registered in the generate group`() {
        setupBuild()
        val result = runner("tasks", "--group", "generate").build()
        assertTrue(
            result.output.contains("renderViralThumbnails"),
            "Expected renderViralThumbnails in generate group, got: ${result.output}",
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = runner("renderViralThumbnails").build()
        assertTrue(
            result.output.contains("viralEnabled=false") || result.output.contains("skipped"),
            "Expected skip log when viralEnabled defaults to false, got: ${result.output}",
        )
    }

    @Test
    fun `already present covers are skipped by the economy of ink`() {
        setupBuild()
        writeCover()
        val dir = projectDir.resolve("build/capsule/viral/videos").also { it.mkdirs() }
        dir.resolve("fr-tiktok-30-thumb.png").writeText("png")

        val result = runner(
            "renderViralThumbnails",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralThumbnails")?.outcome)
        assertTrue(
            result.output.contains("economy of ink") || result.output.contains("skipping"),
            "Expected the economy-of-ink skip, got: ${result.output}",
        )
    }

    @Test
    fun `a missing cover warns when not strict`() {
        setupBuild()
        val result = runner(
            "renderViralThumbnails",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralThumbnails")?.outcome)
        assertTrue(
            result.output.contains("no cover"),
            "Expected a degraded warning on the missing cover, got: ${result.output}",
        )
    }

    @Test
    fun `the cover is captured into a per-variant portrait PNG`() {
        assumePlaywrightAvailable()
        setupBuild()
        writeCover()

        val result = runner(
            "renderViralThumbnails",
            "-Pcapsule.viral.enabled=true",
            "-Pcapsule.viral.languages=fr,en",
            "-Pcapsule.viral.platforms=TIKTOK",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralThumbnails")?.outcome)
        val dir = projectDir.resolve("build/capsule/viral/videos")
        for (id in listOf("fr-tiktok-30", "en-tiktok-30")) {
            val png = dir.resolve("$id-thumb.png")
            assertTrue(png.exists() && png.length() > 0L, "Expected the cover PNG for $id, got: ${dir.listFiles()?.joinToString { it.name }}")
        }
    }
}
