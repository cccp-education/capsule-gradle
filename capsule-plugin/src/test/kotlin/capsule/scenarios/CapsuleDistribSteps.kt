package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for the CAP-ARCH-7 US-4 `distributeCapsuleVideo` task.
 *
 * Self-contained GradleRunner scenarios (pattern `MultiLanguageCapsuleSteps`):
 * each scenario sets up an isolated temp project with the capsule plugin, a
 * fake WebM in `build/capsules/`, then runs `distributeCapsuleVideo` with CLI
 * overrides (`-Poutput.*`) and asserts the versioned destination structure.
 *
 * Step names are scoped with "distrib" to avoid duplicate definitions with
 * [CapsuleStepDefinitions] in the same glue package.
 *
 * Economy of ink is validated by running the task twice and asserting the
 * second run skips the existing valid copy.
 */
class CapsuleDistribSteps {

    private var projectDir: File? = null
    private var destRoot: File? = null
    private var lastBuildOutput: String = ""
    private var lastCliArgs: List<String> = emptyList()

    @Given("a capsule distrib project with a fake WebM {string} in the build output directory")
    fun aCapsuleDistribProjectWithAFakeWebM(webmName: String) {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-distrib-${System.currentTimeMillis()}")
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

        val capDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve(webmName).writeText("fake-webm-content-for-distrib-cucumber-test")

        destRoot = projectDir!!.resolve("office/videos").also { it.mkdirs() }
    }

    @Given("an existing versioned directory {string} under the destination root")
    fun anExistingVersionedDirectoryUnderTheDestinationRoot(dirName: String) {
        val root = destRoot ?: error("Destination root not set up")
        File(root, dirName).also { it.mkdirs() }
    }

    @When("the distrib tasks are listed in the {string} group")
    fun theDistribTasksAreListedInTheGroup(group: String) {
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", group)
            .withProjectDir(projectDir!!)
            .build()
        lastBuildOutput = result.output
    }

    @When("the distributeCapsuleVideo task is run with versioning {string}")
    fun theDistributeCapsuleVideoTaskIsRunWithVersioning(versioning: String) {
        val destPath = destRoot!!.absolutePath
        val args = listOf(
            "distributeCapsuleVideo",
            "-Poutput.videoDestinationDir=$destPath",
            "-Poutput.versioning=$versioning",
        )
        lastCliArgs = args
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(args)
            .withProjectDir(projectDir!!)
            .build()
        lastBuildOutput = result.output
    }

    @When("the distributeCapsuleVideo task is run again with the same arguments")
    fun theDistributeCapsuleVideoTaskIsRunAgainWithTheSameArguments() {
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(lastCliArgs)
            .withProjectDir(projectDir!!)
            .build()
        lastBuildOutput = result.output
    }

    @Then("the distrib task list contains {string}")
    fun theDistribTaskListContains(taskName: String) {
        Assertions.assertTrue(
            lastBuildOutput.contains(taskName),
            "Expected task list to contain '$taskName'. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @Then("the distrib build output contains {string}")
    fun theDistribBuildOutputContains(fragment: String) {
        Assertions.assertTrue(
            lastBuildOutput.contains(fragment),
            "Expected build output to contain '$fragment'. Output: ${lastBuildOutput.take(2000)}",
        )
    }

    @And("a versioned directory matching prefix {string} is created under the destination root")
    fun aVersionedDirectoryMatchingPrefixIsCreatedUnderTheDestinationRoot(prefix: String) {
        val root = destRoot ?: error("Destination root not set up")
        val versionDirs = root.listFiles { f -> f.isDirectory && f.name.startsWith(prefix) }?.toList() ?: emptyList()
        Assertions.assertTrue(versionDirs.isNotEmpty(), "Expected at least one versioned dir with prefix '$prefix' under ${root.absolutePath}")
    }

    @And("a versioned directory named {string} is created under the destination root")
    fun aVersionedDirectoryNamedIsCreatedUnderTheDestinationRoot(dirName: String) {
        val root = destRoot ?: error("Destination root not set up")
        val dir = File(root, dirName)
        Assertions.assertTrue(dir.exists() && dir.isDirectory, "Expected versioned directory ${dir.absolutePath} to exist")
    }

    @And("the video file {string} is copied to the versioned destination")
    fun theVideoFileIsCopiedToTheVersionedDestination(videoFileName: String) {
        val root = destRoot ?: error("Destination root not set up")
        val versionDirs = root.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
        val found = versionDirs.any { dir ->
            val video = File(dir, videoFileName)
            video.exists() && video.length() > 0
        }
        Assertions.assertTrue(found, "Expected $videoFileName to be copied to a versioned dir under ${root.absolutePath}")
    }
}