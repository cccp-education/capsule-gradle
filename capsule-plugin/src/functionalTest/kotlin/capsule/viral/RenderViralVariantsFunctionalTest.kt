package capsule.viral

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `renderViralVariants` Gradle task (CAP-SHORT US-1b).
 *
 * Verifies:
 *  - task registration with the `generate` group
 *  - disabled-by-default no-op skip (economy of ink)
 *  - already-rendered variants are skipped (economy of ink) — no capture needed
 *
 * The real Playwright + FFmpeg capture is not exercised here (heavy/flaky);
 * the orchestration itself is covered by [ViralVariantRendererTest].
 */
class RenderViralVariantsFunctionalTest {

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
        projectDir.resolve("storyboard.adoc").writeText(
            """
            = Storyboard — Devenir formateur professionnel
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

            === Beat 2 — development

            Intent: Les 4 activités types

            Duration: 10.0s
            Render: HTML

            === Beat 3 — development

            Intent: Le rôle du formateur

            Duration: 10.0s
            Render: HTML

            === Beat 4 — cta

            Intent: Découvrez FPA

            Duration: 7.0s
            Render: HTML
            """.trimIndent(),
        )
    }

    @Test
    fun `renderViralVariants task is registered in the generate group`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "generate")
            .withProjectDir(projectDir)
            .build()
        assertTrue(
            result.output.contains("renderViralVariants"),
            "Expected renderViralVariants in generate group, got: ${result.output}",
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("renderViralVariants")
            .withProjectDir(projectDir)
            .build()
        assertTrue(
            result.output.contains("viralEnabled=false") || result.output.contains("skipped"),
            "Expected skip log when viralEnabled defaults to false, got: ${result.output}",
        )
    }

    @Test
    fun `already rendered variants are skipped`() {
        setupBuild()
        writeStoryboard()
        val videosDir = projectDir.resolve("build/capsule/viral/videos").also { it.mkdirs() }
        videosDir.resolve("fr-tiktok-30.mp4").writeText("mp4")
        videosDir.resolve("en-tiktok-30.mp4").writeText("mp4")

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "renderViralVariants",
                "-Pcapsule.viral.enabled=true",
                "-Pcapsule.viral.storyboardFile=storyboard.adoc",
                "-Pcapsule.viral.languages=fr,en",
                "-Pcapsule.viral.platforms=TIKTOK",
            )
            .withProjectDir(projectDir)
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":renderViralVariants")?.outcome)
        assertTrue(
            result.output.contains("0 rendered, 2 skipped"),
            "Expected the economy-of-ink skip summary, got: ${result.output}",
        )
    }
}
