package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional test for the `distributeCapsuleVideo` task (CAP-ARCH-7).
 *
 * Verifies the task copies WebM videos from the build output directory to a
 * versioned subdirectory under the configured `output.videoDestinationDir`,
 * enabling the user to compare capsule versions side-by-side.
 */
class DistributeCapsuleVideoFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun setupWithVideos(outputDir: String = "capsules") {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                outputDir = "$outputDir"
            }
        """.trimIndent())

        val capDir = projectDir.resolve("build/$outputDir").also { it.mkdirs() }
        capDir.resolve("demo.webm").writeText("fake-webm-content-for-distrib-test")
    }

    @Test
    fun `distributeCapsuleVideo is registered and described`() {
        setupWithVideos()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "distribute")
            .withProjectDir(projectDir)
            .build()

        assertTrue(result.output.contains("distributeCapsuleVideo"), "Task should be listed. Output: ${result.output.take(500)}")
    }

    @Test
    fun `distributeCapsuleVideo copies webm to versioned destination`() {
        setupWithVideos(outputDir = "capsules")
        val destRoot = projectDir.resolve("office/videos").absolutePath

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "distributeCapsuleVideo",
                "-Poutput.videoDestinationDir=$destRoot",
                "-Poutput.versioning=incremental",
            )
            .withProjectDir(projectDir)
            .build()

        assertTrue(result.output.contains("CAPSULE DISTRIB"), "Expected summary log. Output: ${result.output.take(500)}")

        val versionDirs: List<File> = File(destRoot).listFiles { f -> f.isDirectory && f.name.startsWith("v") }?.toList() ?: emptyList()
        assertTrue(versionDirs.isNotEmpty(), "Should have at least one versioned dir under $destRoot")

        val firstVersionDir = versionDirs.first()
        val destWebm = File(firstVersionDir, "demo.webm")
        assertTrue(destWebm.exists(), "Expected ${destWebm.absolutePath} to exist")
        assertTrue(destWebm.length() > 0, "Copied WebM should not be empty")
    }

    @Test
    fun `distributeCapsuleVideo incremental skips existing valid copy`() {
        setupWithVideos(outputDir = "capsules")
        val destRoot = projectDir.resolve("office/videos").absolutePath

        val args = arrayOf(
            "distributeCapsuleVideo",
            "-Poutput.videoDestinationDir=$destRoot",
            "-Poutput.versioning=incremental",
        )

        val firstRun = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)
            .build()

        assertTrue(firstRun.output.contains("copied"), "First run should copy. Output: ${firstRun.output.take(500)}")

        val secondRun = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir)
            .build()

        assertTrue(secondRun.output.contains("skipped"), "Second run should skip existing valid copy. Output: ${secondRun.output.take(500)}")
    }
}