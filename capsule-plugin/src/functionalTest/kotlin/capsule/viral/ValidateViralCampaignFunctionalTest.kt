package capsule.viral

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `validateViralCampaign` Gradle task (CAP-VIRAL-E2E US-1).
 *
 * Verifies:
 *  - task registration with the `verification` group
 *  - disabled-by-default no-op skip (economy of ink)
 *  - enabled + manifest whose assets exist → OK
 *  - enabled + manifest with a missing asset → warn (degraded) or fail (strict)
 *
 * The chain (`generateViralCampaign` → `renderViralVariants`) is excluded so the
 * gate is exercised in isolation against a hand-written manifest.
 */
class ValidateViralCampaignFunctionalTest {

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

    /** Writes a hand-crafted `viral-campaign.json` referencing [videoFile]/[thumbnailFile]. */
    private fun writeManifest(videoFile: String, thumbnailFile: String = "") {
        val dir = projectDir.resolve("build/capsule/viral").also { it.mkdirs() }
        dir.resolve("viral-campaign.json").writeText(
            """
            [ {
              "language" : "fr",
              "platform" : "TIKTOK",
              "videoFile" : "$videoFile",
              "thumbnailFile" : "$thumbnailFile",
              "metadata" : { "hook" : "h", "title" : "t", "caption" : "c", "hashtags" : [ "#content" ] }
            } ]
            """.trimIndent(),
        )
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments(*args, "-x", "renderViralVariants", "-x", "generateViralCampaign")
        .withProjectDir(projectDir)

    @Test
    fun `validateViralCampaign task is registered in the verification group`() {
        setupBuild()
        val result = runner("tasks", "--group", "verification").build()
        assertTrue(
            result.output.contains("validateViralCampaign"),
            "Expected validateViralCampaign in verification group, got: ${result.output}",
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = runner("validateViralCampaign").build()
        assertTrue(
            result.output.contains("viralEnabled=false") || result.output.contains("skipped"),
            "Expected skip log when viralEnabled defaults to false, got: ${result.output}",
        )
    }

    @Test
    fun `enabled with present assets passes the gate`() {
        setupBuild()
        val video = projectDir.resolve("build/capsule/viral/fr-tiktok-30.mp4").also {
            it.parentFile.mkdirs()
            it.writeText("mp4")
        }
        writeManifest(video.absolutePath)

        val result = runner(
            "validateViralCampaign",
            "-Pcapsule.viral.enabled=true",
        ).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":validateViralCampaign")?.outcome)
        assertTrue(
            result.output.contains("CAPSULE VIRAL GATE") && result.output.contains("OK"),
            "Expected the gate OK summary, got: ${result.output}",
        )
    }

    @Test
    fun `enabled with a missing asset fails when strict`() {
        setupBuild(
            """
            capsule {
                strictMode = true
            }
            """.trimIndent(),
        )
        writeManifest("build/capsule/viral/missing.mp4")

        val exception = assertThrows<UnexpectedBuildFailure> {
            runner("validateViralCampaign", "-Pcapsule.viral.enabled=true").build()
        }
        assertTrue(
            exception.message!!.contains("missing asset"),
            "Expected the gate to report the missing asset, got: ${exception.message}",
        )
    }

    @Test
    fun `enabled with a missing asset warns when not strict`() {
        setupBuild()
        writeManifest("build/capsule/viral/missing.mp4")

        val result = runner("validateViralCampaign", "-Pcapsule.viral.enabled=true").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":validateViralCampaign")?.outcome)
        assertTrue(
            result.output.contains("missing asset"),
            "Expected a degraded warning on the missing asset, got: ${result.output}",
        )
    }
}
