package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for CAP-AUDIO US-3 wiring — `applyAudioPostIfEnabled`
 * in `CapsuleVideoTask` (post-burnIn, pre-convertFormat).
 *
 * Three scenarios (NoOp processor — no real FFmpeg):
 * 1. Default config (bgmEnabled=false, duckingEnabled=false) → audio post
 *    step skipped entirely (economy of ink), no "Audio post" log, WebM produced.
 * 2. BGM enabled + NoOp processor (ffmpegPath="noop") → step triggered,
 *    NoOp `process()` returns `false` → degraded (keeps original), WebM produced.
 * 3. Ducking enabled + NoOp processor → step triggered (duckingEnabled=true
 *    passes the economy-of-ink guard), NoOp `process()` returns `false` →
 *    degraded (keeps original), WebM produced.
 *
 * Pattern: [CaptureStrategyFunctionalTest] (NoOp capture, `generateCapsuleVideo`
 * task, assert WebM + log output).
 */
class AudioPostFunctionalTest {

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

    private fun writeScriptAndDeck() {
        val scriptDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("audiopost-test-script.txt").writeText("""
            === CAPSULE SCRIPT : audiopost-test ===
            --- SLIDE 1 : Title ---
            Note content for audio post test.
        """.trimIndent())

        val decksDir = projectDir.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        decksDir.resolve("audiopost-test-deck.html").writeText("""
            <html><body>
            <div class="reveal">
              <div class="slides">
                <section data-capsule-slide="1"><h2>Title</h2></section>
              </div>
            </div>
            </body></html>
        """.trimIndent())
    }

    @Test
    fun `audio post default config skips step and produces WebM`() {
        setupBuild("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
        """.trimIndent())
        writeScriptAndDeck()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideo")
            .withProjectDir(projectDir)
            .build()

        val capFile = projectDir.resolve("build/capsules/audiopost-test.webm")
        assertTrue(capFile.exists(), "Video must be produced with default audio post config")
        assertFalse(
            result.output.contains("Audio post:"),
            "Audio post step must be skipped by default (economy of ink). Output: ${result.output.take(800)}"
        )
    }

    @Test
    fun `audio post BGM enabled with NoOp processor degrades and keeps original WebM`() {
        setupBuild("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
                audioPostBgmEnabled = true
                audioPostBgmFile = "/nonexistent/bgm.mp3"
                ffmpegExecutablePath = "noop"
            }
        """.trimIndent())
        writeScriptAndDeck()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideo")
            .withProjectDir(projectDir)
            .build()

        val capFile = projectDir.resolve("build/capsules/audiopost-test.webm")
        assertTrue(
            capFile.exists(),
            "Video must be produced (degraded — original kept) when NoOp audio post processor returns false"
        )
        assertTrue(
            result.output.contains("Audio post:"),
            "Expected audio post step log when bgmEnabled=true. Output: ${result.output.take(800)}"
        )
    }

    @Test
    fun `audio post ducking enabled with NoOp processor degrades and keeps original WebM`() {
        setupBuild("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
                audioPostDuckingEnabled = true
                ffmpegExecutablePath = "noop"
            }
        """.trimIndent())
        writeScriptAndDeck()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideo")
            .withProjectDir(projectDir)
            .build()

        val capFile = projectDir.resolve("build/capsules/audiopost-test.webm")
        assertTrue(
            capFile.exists(),
            "Video must be produced (degraded — original kept) when NoOp audio post processor returns false"
        )
        assertTrue(
            result.output.contains("Audio post:"),
            "Expected audio post step log when duckingEnabled=true. Output: ${result.output.take(800)}"
        )
    }
}