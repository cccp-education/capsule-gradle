package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_format.feature` (CAP-MP4 US-3).
 *
 * Self-contained GradleRunner scenarios (pattern `CapsuleDistribSteps`):
 * each scenario sets up an isolated temp project with the capsule plugin,
 * fake WebM/MP4 files in `build/capsules/`, then runs `distributeCapsuleVideo`
 * with `-Poutput.format=<webm|mp4|both>` and asserts which files are copied
 * to the versioned destination.
 *
 * Step texts are prefixed with "format distribution" (and use unique
 * sentence shapes) to avoid `DuplicateStepDefinitionException` with the
 * shared `capsule.scenarios` glue (bug S-088 — glue is classpath-wide,
 * step texts must be unique).
 *
 * Uses a NoOp converter (no real FFmpeg) — the scenarios only validate the
 * distribution filter, not the transcode itself (covered by unit/functional
 * tests for `FormatConversion` / `VideoFormatConverter` from US-2).
 */
class CapsuleFormatSteps {

    private var projectDir: File? = null
    private var destRoot: File? = null
    private var lastBuildOutput: String = ""

    @Given("a capsule format distribution project with a fake WebM {string} in the build output directory")
    fun aCapsuleFormatProjectWithAFakeWebM(webmName: String) {
        setupFormatProject()
        val capDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve(webmName).writeText("fake-webm-content-for-format-cucumber-test")
    }

    @Given("a capsule format distribution project with a fake WebM {string} and a fake MP4 {string} in the build output directory")
    fun aCapsuleFormatProjectWithAFakeWebMAndMp4(webmName: String, mp4Name: String) {
        setupFormatProject()
        val capDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve(webmName).writeText("fake-webm-content-for-format-cucumber-test")
        capDir.resolve(mp4Name).writeText("fake-mp4-content-for-format-cucumber-test")
    }

    private fun setupFormatProject() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-format-${System.currentTimeMillis()}")
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
        destRoot = projectDir!!.resolve("office/videos").also { it.mkdirs() }
    }

    @When("the distributeCapsuleVideo task is run with format {string}")
    fun theDistributeCapsuleVideoTaskIsRunWithFormat(format: String) {
        val destPath = destRoot!!.absolutePath
        val args = listOf(
            "distributeCapsuleVideo",
            "-Poutput.videoDestinationDir=$destPath",
            "-Poutput.versioning=timestamp",
            "-Poutput.format=$format",
        )
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir!!)
            .build()
        lastBuildOutput = result.output
    }

    @Then("the format distribution build output contains {string}")
    fun theFormatDistributionBuildOutputContains(fragment: String) {
        Assertions.assertTrue(
            lastBuildOutput.contains(fragment),
            "Expected build output to contain '$fragment'. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @And("the format distribution copies only video file {string} to the versioned destination")
    fun theFormatDistributionCopiesOnlyVideoFileToVersionedDestination(videoFileName: String) {
        val copied = copiedFiles()
        Assertions.assertTrue(
            copied.any { it.name == videoFileName },
            "Expected $videoFileName to be copied to a versioned dir. Found: $copied",
        )
        // Only one file should be present — the other format must be filtered out.
        Assertions.assertEquals(
            1, copied.size,
            "Expected only $videoFileName in the destination, but found: ${copied.map { it.name }}",
        )
    }

    @And("the format distribution copies video file {string} to the versioned destination")
    fun theFormatDistributionCopiesVideoFileToVersionedDestination(videoFileName: String) {
        val copied = copiedFiles()
        Assertions.assertTrue(
            copied.any { it.name == videoFileName },
            "Expected $videoFileName to be copied to a versioned dir. Found: $copied",
        )
    }

    private fun copiedFiles(): List<File> {
        val root = destRoot ?: error("Destination root not set up")
        val versionDirs = root.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
        return versionDirs.flatMap { dir ->
            dir.listFiles { f -> f.isFile && f.length() > 0 }?.toList() ?: emptyList()
        }
    }
}