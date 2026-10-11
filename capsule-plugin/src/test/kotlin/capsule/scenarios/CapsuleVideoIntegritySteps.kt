package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_video_integrity.feature` (PRT-04).
 *
 * Self-contained GradleRunner scenarios (pattern [CapsuleDurationValidationSteps]):
 * each scenario sets up an isolated temp project with the capsule plugin, an
 * optional *corrupt* video file in `build/capsules/`, then runs
 * `validateCapsuleVideoDuration` with the integrity flag.
 *
 * The corrupt path is robust whether ffmpeg is present (the decode fails) or
 * absent (the probe cannot be run) — in both cases the verdict is Corrupt, so
 * the task fails with the same actionable message.
 *
 * Step texts are prefixed with "video integrity" (S-088) to avoid
 * `DuplicateStepDefinitionException` with the classpath-wide glue.
 */
class CapsuleVideoIntegritySteps {

    private var projectDir: File? = null
    private var lastBuildOutput: String = ""
    private var buildFailed: Boolean = false

    @Given("a capsule video integrity project with a corrupt video {string}")
    fun aCapsuleVideoIntegrityProjectWithCorruptVideo(videoName: String) {
        setupIntegrityProject()
        val capDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve(videoName).writeText("not-a-real-media-container")
    }

    @Given("a capsule video integrity project with no video file")
    fun aCapsuleVideoIntegrityProjectWithNoVideo() {
        setupIntegrityProject()
    }

    private fun setupIntegrityProject() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-integrity-${System.currentTimeMillis()}-${System.nanoTime()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
            """.trimIndent()
        )
    }

    @When("the validateCapsuleVideoDuration task is run with integrity validation disabled")
    fun theTaskIsRunWithIntegrityValidationDisabled() {
        runIntegrityTask(enabled = false, expectFailure = false)
    }

    @When("the validateCapsuleVideoDuration task is run with integrity validation enabled")
    fun theTaskIsRunWithIntegrityValidationEnabled() {
        runIntegrityTask(enabled = true, expectFailure = false)
    }

    @When("the validateCapsuleVideoDuration task is run with integrity validation enabled and the build is expected to fail")
    fun theTaskIsRunWithIntegrityValidationEnabledExpectingFailure() {
        runIntegrityTask(enabled = true, expectFailure = true)
    }

    private fun runIntegrityTask(enabled: Boolean, expectFailure: Boolean) {
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "validateCapsuleVideoDuration",
                "-Pcapsule.validation.integrityEnabled=$enabled",
            )
            .withProjectDir(projectDir!!)
        if (expectFailure) {
            lastBuildOutput = runner.buildAndFail().output
            buildFailed = true
        } else {
            lastBuildOutput = runner.build().output
            buildFailed = false
        }
    }

    @Then("the video integrity build output contains {string}")
    fun theVideoIntegrityBuildOutputContains(fragment: String) {
        Assertions.assertTrue(
            lastBuildOutput.contains(fragment),
            "Expected build output to contain '$fragment'. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @And("the video integrity build does not fail")
    fun theVideoIntegrityBuildDoesNotFail() {
        Assertions.assertFalse(
            buildFailed,
            "Expected the integrity build to succeed. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @And("the video integrity build fails")
    fun theVideoIntegrityBuildFails() {
        Assertions.assertTrue(
            buildFailed,
            "Expected the integrity build to fail. Output: ${lastBuildOutput.take(2000)}",
        )
    }
}
