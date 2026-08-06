package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * TDD baby-step — CR-2.5 functional (GradleTestKit).
 *
 * Verifies that the captureTimeoutMinutes DSL is resolved end-to-end:
 * - explicit DSL value survives plugin wiring
 * - CLI -P override beats the DSL value (4-source merge: ENV < props < YAML < CLI)
 */
class CaptureTimeoutFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun writeBuild(extraConfig: String = "") {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            import capsule.CapsuleExtension

            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                $extraConfig
            }
            tasks.register('printCaptureTimeout') {
                doLast {
                    def ext = project.extensions.findByType(CapsuleExtension)
                    println "CAPTURE_TIMEOUT_MINUTES=" + ext.captureTimeoutMinutes.get()
                }
            }
        """.trimIndent())
    }

    private fun runner(vararg args: String): GradleRunner = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(projectDir)
        .withArguments(*args)

    @Test
    fun `captureTimeoutMinutes DSL value is resolved by the plugin`() {
        writeBuild("parallelCaptureEnabled = true\n    captureTimeoutMinutes = 2")

        val result = runner("printCaptureTimeout").build()

        assertTrue(
            result.output.contains("CAPTURE_TIMEOUT_MINUTES=2"),
            "DSL captureTimeoutMinutes should be resolved to 2, got: ${result.output.lines().find { it.contains("CAPTURE_TIMEOUT_MINUTES") }}"
        )
    }

    @Test
    fun `CLI -P captureTimeoutMinutes overrides default value`() {
        // No DSL: extension holds the convention (5), so the merged CLI value wins.
        writeBuild("parallelCaptureEnabled = true")

        val result = runner("printCaptureTimeout", "-Pcapsule.capture.captureTimeoutMinutes=3").build()

        assertTrue(
            result.output.contains("CAPTURE_TIMEOUT_MINUTES=3"),
            "CLI -P should override the default captureTimeoutMinutes (expected 3), got: ${result.output.lines().find { it.contains("CAPTURE_TIMEOUT_MINUTES") }}"
        )
    }

    @Test
    fun `captureTimeoutMinutes defaults to 5 when not configured`() {
        writeBuild("parallelCaptureEnabled = true")

        val result = runner("printCaptureTimeout").build()

        assertTrue(
            result.output.contains("CAPTURE_TIMEOUT_MINUTES=5"),
            "Unconfigured captureTimeoutMinutes should default to 5, got: ${result.output.lines().find { it.contains("CAPTURE_TIMEOUT_MINUTES") }}"
        )
    }
}
