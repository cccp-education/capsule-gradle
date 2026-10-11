package capsule.voice

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for `selectBestVoiceTake` (CAP-QC-VOICE US-3).
 *
 * Verifies task registration, the disabled-by-default no-op, the no-candidate
 * skip, and the ASR-less degradation. The degradation assertion only runs when
 * the ASR binary is absent (`assumeFalse`).
 */
class SelectBestVoiceTakeFunctionalTest {

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

    private fun writeScriptAndCandidates() {
        val capsuleDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        capsuleDir.resolve("demo-script.txt").writeText(
            """
            === CAPSULE SCRIPT : demo ===

            --- SLIDE 1 : Intro ---
            bonjour le monde
            """.trimIndent()
        )
        val deckDir = capsuleDir.resolve("demo").also { it.mkdirs() }
        deckDir.resolve("slide-01-1.mp3").writeText("candidate one")
        deckDir.resolve("slide-01-2.mp3").writeText("candidate two")
    }

    private fun whisperOnPath(): Boolean =
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { entry ->
            File(entry, "whisper").let { it.isFile && it.canExecute() }
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
            result.output.contains("selectBestVoiceTake"),
            "Expected selectBestVoiceTake in verification group, got: ${result.output}"
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("selectBestVoiceTake")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("voice.selectBestTake=false"),
            "Expected a skip log when disabled by default, got: ${result.output}"
        )
    }

    @Test
    fun `enabled with no candidate skips gracefully`() {
        setupBuild(
            """
            capsule {
                voiceSelectBestTake = true
            }
            """.trimIndent()
        )
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("selectBestVoiceTake")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("no candidate take found"),
            "Expected a graceful skip when no candidate exists, got: ${result.output}"
        )
    }

    @Test
    fun `enabled with an unavailable ASR degrades without failing`() {
        assumeFalse(whisperOnPath()) {
            "'whisper' is installed on this host — degradation path not exercised here"
        }
        setupBuild(
            """
            capsule {
                voiceSelectBestTake = true
                voiceAsrExecutablePath = "whisper"
            }
            """.trimIndent()
        )
        writeScriptAndCandidates()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("selectBestVoiceTake")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("degraded") || result.output.contains("CAPSULE VOICE SELECT"),
            "Expected a degraded warning when the ASR is absent, got: ${result.output}"
        )
        assertTrue(
            projectDir.resolve("build/capsule/voice-selection.json").isFile,
            "Expected the voice selection report to be written"
        )
    }
}
