package capsule.viral

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `normalizeViralStoryboard` Gradle task: a reproducible,
 * tested replacement for the ad-hoc transform used during the CAP-SHORT US-4
 * dogfooding (fill every missing per-beat entering transition).
 */
class NormalizeViralStoryboardFunctionalTest {

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

    /** Hand-authored storyboard WITHOUT transitions (the state the normalizer must fix). */
    private fun writeRawStoryboard() {
        projectDir.resolve("raw-storyboard.adoc").writeText(
            """
            = Storyboard — Maîtriser opencode
            :platform: TIKTOK
            :language: fr
            :target-duration: 30

            == Angle

            L'agent écrit, Gradle assemble

            == Beats

            === Beat 1 — hook

            Intent: Et si votre workspace se pilotait tout seul ?

            Duration: 3.0s
            Render: HTML

            === Beat 2 — development

            Intent: Décrivez l'intention

            Duration: 9.0s
            Render: HTML

            === Beat 3 — cta

            Intent: Essayez talaria.school

            Duration: 6.0s
            Render: HTML
            """.trimIndent(),
        )
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments(*args)
        .withProjectDir(projectDir)

    @Test
    fun `normalizeViralStoryboard task is registered in the generate group`() {
        setupBuild()
        val result = runner("tasks", "--group", "generate").build()
        assertTrue(
            result.output.contains("normalizeViralStoryboard"),
            "Expected normalizeViralStoryboard in generate group, got: ${result.output}",
        )
    }

    @Test
    fun `without a storyboard it warns and does nothing`() {
        setupBuild()
        val result = runner("normalizeViralStoryboard").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":normalizeViralStoryboard")?.outcome)
        assertTrue(
            result.output.contains("no storyboard"),
            "Expected a degraded warning, got: ${result.output}",
        )
    }

    @Test
    fun `fills every missing per-beat transition`() {
        setupBuild()
        writeRawStoryboard()

        val result = runner(
            "normalizeViralStoryboard",
            "-Pcapsule.viral.storyboardFile=raw-storyboard.adoc",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":normalizeViralStoryboard")?.outcome)
        val out = projectDir.resolve("build/capsule/viral/storyboard.normalized.adoc")
        assertTrue(out.exists(), "Expected the normalized storyboard, got: ${out.path}")
        val content = out.readText()
        assertEquals(3, Regex("^Transition: ", RegexOption.MULTILINE).findAll(content).count(), "Every beat must carry a transition, got: $content")
        assertTrue(content.contains("Transition: — (ouverture)"), "Expected the hook opening transition")
        assertTrue(content.contains("Transition: cut"), "Expected the development cut transition")
        assertTrue(content.contains("Transition: fade"), "Expected the cta fade transition")
    }
}
