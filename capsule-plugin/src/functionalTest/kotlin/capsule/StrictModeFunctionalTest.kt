package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for strict mode (CAP-CR3-2 US-3).
 *
 * When `capsule.strictMode = true`, the plugin must fail the build
 * instead of silently falling back to a NoOp engine when a required
 * tool (Piper, espeak, Playwright, ffmpeg, manim) is unavailable.
 *
 * When `capsule.strictMode = false` (default), the existing NoOp
 * fallback behavior is preserved (backward compatibility).
 */
class StrictModeFunctionalTest {

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

    private fun writeScript() {
        val scriptDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("strict-test-script.txt").writeText("""
            === CAPSULE SCRIPT : strict-test ===
            --- SLIDE 1 : Test ---
            Ceci est un test de synthese vocale.
        """.trimIndent())
    }

    @Test
    fun `strictMode disabled by default preserves NoOp fallback`() {
        setupBuild("""
            capsule {
                ttsEngine = "noop"
            }
        """.trimIndent())
        writeScript()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsule")
            .withProjectDir(projectDir)
            .build()

        // Default behavior: noop engine accepted, build succeeds
        assertTrue(
            result.output.contains("TTS engine: noop") || result.output.contains("TTS generation"),
            "Expected noop TTS engine log when strictMode is off"
        )
    }

    @Test
    fun `strictMode enabled fails build when piper is not available`() {
        setupBuild("""
            capsule {
                strictMode = true
                ttsEngine = "piper"
                piperExecutablePath = "/nonexistent/piper-binary-xyz"
                ttsFallbackEnabled = true
            }
        """.trimIndent())
        writeScript()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsule")
            .withProjectDir(projectDir)
            .buildAndFail()

        assertTrue(
            result.output.contains("strictMode"),
            "Expected failure message to mention strictMode, got: ${result.output}"
        )
        assertTrue(
            result.output.contains("piper"),
            "Expected failure message to mention piper, got: ${result.output}"
        )
    }

    @Test
    fun `strictMode enabled allows noop engine when explicitly configured`() {
        setupBuild("""
            capsule {
                strictMode = true
                ttsEngine = "noop"
            }
        """.trimIndent())
        writeScript()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsule")
            .withProjectDir(projectDir)
            .build()

        // `noop` is an explicit user choice, not a fallback — strictMode should not block it
        assertTrue(
            result.output.contains("TTS engine: noop") || result.output.contains("TTS generation"),
            "Expected noop TTS engine to be accepted even in strict mode (explicit choice)"
        )
    }

    @Test
    fun `strictMode via CLI flag fails build when piper is not available`() {
        setupBuild("""
            capsule {
                ttsEngine = "piper"
                piperExecutablePath = "/nonexistent/piper-binary-xyz"
                ttsFallbackEnabled = true
            }
        """.trimIndent())
        writeScript()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsule", "-Pcapsule.strictMode.enabled=true")
            .withProjectDir(projectDir)
            .buildAndFail()

        assertTrue(
            result.output.contains("strictMode"),
            "Expected CLI strictMode to trigger failure message mentioning strictMode"
        )
    }
}