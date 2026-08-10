package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.test.assertTrue

/**
 * BDD steps for the CAP-CR3-2 `capsule_strict_mode.feature`.
 *
 * Self-contained GradleRunner scenarios (pattern `CapsuleDistribSteps`):
 * each scenario sets up an isolated temp project with the capsule plugin,
 * a fake capsule script, then runs `generateCapsule` with or without
 * strict mode and asserts success/failure + message content.
 *
 * Step names are scoped with "strict" to avoid duplicate definitions with
 * other step classes in the same glue package.
 */
class CapsuleStrictModeSteps {

    private var projectDir: File? = null
    private var lastBuildOutput: String = ""
    private var lastBuildFailed: Boolean = false
    private var ttsEngine: String = "piper"
    private var piperPath: String = "piper"
    private var strictEnabled: Boolean = false
    private var extraCliArgs: List<String> = emptyList()

    @Given("a capsule strict project with ttsEngine {string} and piperExecutablePath {string}")
    fun aCapsuleStrictProject(ttsEngine: String, piperPath: String) {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-strict-${System.currentTimeMillis()}")
            .also { it.mkdirs() }

        projectDir!!.resolve("settings.gradle").writeText("")
        this.ttsEngine = ttsEngine
        this.piperPath = piperPath
        writeBuildFile()
    }

    @And("strict mode is disabled")
    fun strictModeIsDisabled() {
        strictEnabled = false
        writeBuildFile()
    }

    @And("strict mode is enabled")
    fun strictModeIsEnabled() {
        strictEnabled = true
        writeBuildFile()
    }

    @And("a capsule script {string} with one slide")
    fun aCapsuleScriptWithOneSlide(scriptName: String) {
        val scriptDir = projectDir!!.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("$scriptName-script.txt").writeText("""
            === CAPSULE SCRIPT : $scriptName ===
            --- SLIDE 1 : Test ---
            This is a test of strict mode.
        """.trimIndent())
    }

    @When("the generateCapsule task is run")
    fun theGenerateCapsuleTaskIsRun() {
        val args = mutableListOf("generateCapsule")
        args.addAll(extraCliArgs)
        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir!!)

        try {
            val result = runner.build()
            lastBuildOutput = result.output
            lastBuildFailed = false
        } catch (e: Exception) {
            lastBuildOutput = e.message ?: ""
            lastBuildFailed = true
        }
    }

    @When("the generateCapsule task is run with CLI flag {string}")
    fun theGenerateCapsuleTaskIsRunWithCliFlag(flag: String) {
        extraCliArgs = listOf(flag)
        theGenerateCapsuleTaskIsRun()
    }

    @Then("the strict build succeeds")
    fun theStrictBuildSucceeds() {
        assertTrue(!lastBuildFailed, "Expected build to succeed but it failed. Output: $lastBuildOutput")
    }

    @Then("the strict build fails")
    fun theStrictBuildFails() {
        assertTrue(lastBuildFailed, "Expected build to fail but it succeeded. Output: $lastBuildOutput")
    }

    @And("the strict build output contains {string}")
    fun theStrictBuildOutputContains(expected: String) {
        assertTrue(
            lastBuildOutput.contains(expected),
            "Expected build output to contain '$expected' but got: $lastBuildOutput"
        )
    }

    private fun writeBuildFile() {
        val strictLine = if (strictEnabled) "strictMode = true" else ""
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "$ttsEngine"
                piperExecutablePath = "$piperPath"
                ttsFallbackEnabled = true
                $strictLine
            }
        """.trimIndent())
    }
}