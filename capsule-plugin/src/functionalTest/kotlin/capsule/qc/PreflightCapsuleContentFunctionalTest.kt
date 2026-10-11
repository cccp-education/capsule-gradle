package capsule.qc

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for `preflightCapsuleContent` (PRT-05).
 *
 * Uses the NoOp TTS engine (no real audio): the task only reads the capsule
 * scripts and reports findings.
 */
class PreflightCapsuleContentFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private fun setupBuild(preflightEnabled: Boolean) {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                qcPreflightEnabled = $preflightEnabled
            }
            """.trimIndent()
        )
    }

    private fun writeScript(note: String) {
        val scriptDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("preflight-script.txt").writeText(
            """
            === CAPSULE SCRIPT : preflight ===
            --- SLIDE 1 : Title ---
            $note
            """.trimIndent()
        )
    }

    private fun run() = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments("preflightCapsuleContent")
        .withProjectDir(projectDir)
        .build()

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild(preflightEnabled = false)
        writeScript("a clean note")

        val result = run()

        assertTrue(
            result.output.contains("skipped"),
            "Expected a skip log when qcPreflightEnabled=false. Output: ${result.output.take(800)}"
        )
    }

    @Test
    fun `enabled with a clean script succeeds`() {
        setupBuild(preflightEnabled = true)
        writeScript("a clean note")

        val result = run()

        assertTrue(
            result.output.contains("CAPSULE PREFLIGHT"),
            "Expected a pre-flight log. Output: ${result.output.take(800)}"
        )
    }

    @Test
    fun `enabled with a forbidden form fails the build`() {
        setupBuild(preflightEnabled = true)
        writeScript("des **classes**")

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("preflightCapsuleContent")
            .withProjectDir(projectDir)
            .buildAndFail()

        assertTrue(
            result.output.contains("Content pre-flight failed"),
            "Expected a pre-flight failure. Output: ${result.output.take(800)}"
        )
    }
}
