package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_audio_post.feature` (CAP-AUDIO US-4).
 *
 * Self-contained GradleRunner scenarios (pattern `CapsuleCaptureStrategySteps`):
 * each scenario sets up an isolated temp project with the capsule plugin,
 * a fake deck + script, then runs `generateCapsuleVideo` with the
 * appropriate `audioPost*` DSL config and asserts the audio post step
 * log + WebM output.
 *
 * Step texts are prefixed with "audio post" (and use unique sentence
 * shapes) to avoid `DuplicateStepDefinitionException` with the shared
 * `capsule.scenarios` glue (bug S-088 — glue is classpath-wide, step
 * texts must be unique).
 *
 * Uses a NoOp processor (no real FFmpeg — `ffmpegExecutablePath = "noop"`
 * routes through `NoOpAudioPostProcessor` which returns `false`,
 * keeping the original video). The scenarios validate the wiring +
 * economy-of-ink guard + factory dispatch, not the audio render itself
 * (covered by unit tests for `AudioPostProcessorImpl` / `AudioPostCommand`
 * from US-2).
 */
class CapsuleAudioPostSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""

    @Given("a Capsule audio post Gradle project with the capsule plugin applied")
    fun aCapsuleAudioPostGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-audio-post-${System.currentTimeMillis()}-${System.nanoTime()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        writeBuildGradle("")
    }

    @And("a demo deck and script are present for audio post")
    fun aDemoDeckAndScriptArePresentForAudioPost() {
        val scriptDir = projectDir!!.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("demo-script.txt").writeText("""
            === CAPSULE SCRIPT : demo ===
            --- SLIDE 1 : Title ---
            Note content for audio post test.
        """.trimIndent())

        val decksDir = projectDir!!.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        decksDir.resolve("demo-deck.html").writeText("""
            <html><body>
            <div class="reveal">
              <div class="slides">
                <section data-capsule-slide="1"><h2>Title</h2></section>
              </div>
            </div>
            </body></html>
        """.trimIndent())
    }

    private fun writeBuildGradle(capsuleBlock: String) {
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            $capsuleBlock
        """.trimIndent())
    }

    @When("I generate the capsule video with the default audio post config")
    fun iGenerateTheCapsuleVideoWithTheDefaultAudioPostConfig() {
        writeBuildGradle("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
        """.trimIndent())
        runAudioPostBuild()
    }

    @When("I generate the capsule video with BGM enabled and a NoOp ffmpeg path")
    fun iGenerateTheCapsuleVideoWithBgmEnabledAndANoOpFfmpegPath() {
        writeBuildGradle("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
                audioPostBgmEnabled = true
                audioPostBgmFile = "/nonexistent/bgm.mp3"
                ffmpegExecutablePath = "noop"
            }
        """.trimIndent())
        runAudioPostBuild()
    }

    @When("I generate the capsule video with ducking enabled and BGM disabled and a NoOp ffmpeg path")
    fun iGenerateTheCapsuleVideoWithDuckingEnabledAndBgmDisabledAndANoOpFfmpegPath() {
        writeBuildGradle("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
                audioPostDuckingEnabled = true
                audioPostBgmEnabled = false
                ffmpegExecutablePath = "noop"
            }
        """.trimIndent())
        runAudioPostBuild()
    }

    @When("I generate the capsule video with ducking and BGM enabled and a NoOp ffmpeg path")
    fun iGenerateTheCapsuleVideoWithDuckingAndBgmEnabledAndANoOpFfmpegPath() {
        writeBuildGradle("""
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
                audioPostBgmEnabled = true
                audioPostBgmFile = "/nonexistent/bgm.mp3"
                audioPostDuckingEnabled = true
                ffmpegExecutablePath = "noop"
            }
        """.trimIndent())
        runAudioPostBuild()
    }

    private fun runAudioPostBuild() {
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideo")
            .withProjectDir(projectDir!!)
            .build()
        buildOutput = result.output
    }

    @Then("the audio post build succeeds")
    fun theAudioPostBuildSucceeds() {
        // If we reach here, the build succeeded (GradleRunner.build() throws on failure)
    }

    @And("the audio post output mentions {string}")
    fun theAudioPostOutputMentions(text: String) {
        Assertions.assertTrue(
            buildOutput.contains(text),
            "Expected build output to mention '$text', got: ${buildOutput.take(2000)}",
        )
    }

    @And("the audio post output does not mention {string}")
    fun theAudioPostOutputDoesNotMention(text: String) {
        Assertions.assertFalse(
            buildOutput.contains(text),
            "Expected build output NOT to mention '$text' (economy of ink). Got: ${buildOutput.take(2000)}",
        )
    }

    @And("an audio post WebM file is produced")
    fun anAudioPostWebmFileIsProduced() {
        val capFile = projectDir!!.resolve("build/capsules/demo.webm")
        Assertions.assertTrue(
            capFile.exists(),
            "Expected WebM file at ${capFile.absolutePath}, but it was not produced",
        )
    }
}