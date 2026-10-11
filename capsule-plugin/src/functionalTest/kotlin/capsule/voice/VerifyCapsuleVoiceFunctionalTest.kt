package capsule.voice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for `verifyCapsuleVoice` (CAP-QC-VOICE US-2).
 *
 * Verifies task registration, the disabled-by-default no-op, the no-take skip,
 * and the ASR-less degradation. The degradation assertion only runs when the
 * ASR binary is absent (`assumeFalse`) — the QC is never a prerequisite of the
 * build, and a host with a real ASR would exercise the full path elsewhere.
 */
class VerifyCapsuleVoiceFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private fun setupBuild(extraConfig: String = "") {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            $extraConfig
            """.trimIndent()
        )
    }

    /** Writes a capsule script and its synthesised slide audio under `build/capsule`. */
    private fun writeScriptAndAudio() {
        val capsuleDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        capsuleDir.resolve("demo-script.txt").writeText(
            """
            === CAPSULE SCRIPT : demo ===

            --- SLIDE 1 : Intro ---
            bonjour le monde
            """.trimIndent()
        )
        val deckDir = capsuleDir.resolve("demo").also { it.mkdirs() }
        deckDir.resolve("slide-01.mp3").writeText("fake audio")
    }

    @Test
    fun `task is registered with verification group`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "verification")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("verifyCapsuleVoice"),
            "Expected verifyCapsuleVoice in verification group, got: ${result.output}"
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("verifyCapsuleVoice")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("voice.verifyEnabled=false"),
            "Expected a skip log when disabled by default, got: ${result.output}"
        )
    }

    @Test
    fun `enabled with no audio take skips gracefully`() {
        setupBuild(
            """
            capsule {
                voiceVerifyEnabled = true
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("verifyCapsuleVoice")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("no audio take found"),
            "Expected a graceful skip when no take exists, got: ${result.output}"
        )
    }

    private fun whisperOnPath(): Boolean =
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { entry ->
            File(entry, "whisper").let { it.isFile && it.canExecute() }
        }

    @Test
    fun `enabled with an unavailable ASR degrades without failing`() {
        assumeFalse(whisperOnPath()) {
            "'whisper' is installed on this host — degradation path not exercised here"
        }
        setupBuild(
            """
            capsule {
                voiceVerifyEnabled = true
                voiceAsrExecutablePath = "whisper"
            }
            """.trimIndent()
        )
        writeScriptAndAudio()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("verifyCapsuleVoice")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("degraded") || result.output.contains("CAPSULE VOICE"),
            "Expected a degraded warning when the ASR is absent, got: ${result.output}"
        )
        assertTrue(
            projectDir.resolve("build/capsule/voice-report.json").isFile,
            "Expected the voice report to be written"
        )
    }
}
