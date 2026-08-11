package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_duration_validation.feature` (CAP-CR3-1 US-3).
 *
 * Self-contained GradleRunner scenarios (pattern `CapsuleFormatSteps`):
 * each scenario sets up an isolated temp project with the capsule plugin,
 * optional fake video/audio files in `build/capsules/`, then runs
 * `validateCapsuleVideoDuration` with the appropriate CLI flags.
 *
 * Step texts are prefixed with "duration validation" (and use unique
 * sentence shapes) to avoid `DuplicateStepDefinitionException` with the
 * shared `capsule.scenarios` glue (bug S-088 — glue is classpath-wide,
 * step texts must be unique).
 *
 * Uses NoOp probe (no real ffprobe — `MediaProbeUtil.probeDuration`
 * returns 0.0 for fake text files, so the validator sees delta=0.0
 * which is within any tolerance).
 */
class CapsuleDurationValidationSteps {

    private var projectDir: File? = null
    private var lastBuildOutput: String = ""
    private var buildFailed: Boolean = false

    @Given("a capsule duration validation project with no video and no audio files")
    fun aCapsuleDurationValidationProjectWithNoFiles() {
        setupDurationProject()
    }

    @Given("a capsule duration validation project with a fake video {string} and {int} fake audio files in the build output directory")
    fun aCapsuleDurationValidationProjectWithFakeVideoAndAudio(videoName: String, audioCount: Int) {
        setupDurationProject()
        val capDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve(videoName).writeText("fake-video-content-for-duration-cucumber-test")
        val audioDir = capDir.resolve("demo").also { it.mkdirs() }
        for (i in 1..audioCount) {
            val idx = String.format("%02d", i)
            audioDir.resolve("slide-$idx.mp3").writeText("fake-audio-$idx-for-duration-cucumber-test")
        }
    }

    private fun setupDurationProject() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-duration-${System.currentTimeMillis()}-${System.nanoTime()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
        """.trimIndent())
    }

    @When("the validateCapsuleVideoDuration task is run with duration validation disabled")
    fun theValidateCapsuleVideoDurationTaskIsRunWithValidationDisabled() {
        runDurationTask(enabled = false, tolerance = null)
    }

    @When("the validateCapsuleVideoDuration task is run with duration validation enabled")
    fun theValidateCapsuleVideoDurationTaskIsRunWithValidationEnabled() {
        runDurationTask(enabled = true, tolerance = null)
    }

    @When("the validateCapsuleVideoDuration task is run with duration validation enabled and tolerance {string}")
    fun theValidateCapsuleVideoDurationTaskIsRunWithValidationEnabledAndTolerance(tolerance: String) {
        runDurationTask(enabled = true, tolerance = tolerance)
    }

    private fun runDurationTask(enabled: Boolean, tolerance: String?) {
        val args = mutableListOf("validateCapsuleVideoDuration")
        args.add("-Pcapsule.validation.durationEnabled=$enabled")
        if (tolerance != null) {
            args.add("-Pcapsule.validation.toleranceSecs=$tolerance")
        }
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir!!)
        val result = runner.build()
        lastBuildOutput = result.output
        buildFailed = false
    }

    @Then("the duration validation build output contains {string}")
    fun theDurationValidationBuildOutputContains(fragment: String) {
        Assertions.assertTrue(
            lastBuildOutput.contains(fragment),
            "Expected build output to contain '$fragment'. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @And("the duration validation build does not fail")
    fun theDurationValidationBuildDoesNotFail() {
        Assertions.assertFalse(
            buildFailed,
            "Expected the duration validation build to succeed. Output: ${lastBuildOutput.take(2000)}",
        )
    }
}