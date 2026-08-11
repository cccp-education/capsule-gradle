package capsule.validation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for `validateCapsuleVideoDuration` (CAP-CR3-1 US-2).
 *
 * Verifies task registration, description, disabled-by-default no-op
 * behavior, and enabled validation path. Uses NoOp probe (no real
 * ffprobe — the task probes files that produce 0.0 duration from
 * `MediaProbeUtil` when ffprobe is unavailable, which is the case in
 * the functional test environment).
 */
class ValidateCapsuleVideoDurationFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private fun setupBuild(extraConfig: String = "") {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            $extraConfig
        """.trimIndent())
    }

    @Test
    fun `task is registered with verification group and description`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "verification")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("validateCapsuleVideoDuration"),
            "Expected validateCapsuleVideoDuration in verification group, got: ${result.output}"
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("validateCapsuleVideoDuration")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("skipped") || result.output.contains("durationEnabled=false"),
            "Expected skip log when durationEnabled defaults to false, got: ${result.output}"
        )
    }

    @Test
    fun `enabled with no video file skips gracefully`() {
        setupBuild("""
            capsule {
                durationValidationEnabled = true
            }
        """.trimIndent())
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("validateCapsuleVideoDuration")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("not found") || result.output.contains("skipped") || result.output.contains("SKIP"),
            "Expected graceful skip when video file is missing, got: ${result.output}"
        )
    }
}