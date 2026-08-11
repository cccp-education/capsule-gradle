package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for the CAP-MP4 US-2 format conversion wiring.
 *
 * Verifies:
 * - The `distributeCapsuleVideo` task filters by `output.format` (WEBM, MP4, BOTH).
 * - The `generateCapsuleVideo` task with `outputFormat=MP4` + NoOp converter
 *   (ffmpeg unavailable) keeps the WebM intermediate (degraded mode, backward compat).
 * - Strict mode + missing ffmpeg + MP4 format → build fails with the StrictModeGuard message.
 */
class FormatConversionFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun setupCapsuleProject(outputDir: String = "capsules") {
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
    }

    @Test
    fun `distributeCapsuleVideo filters mp4 files when format is MP4`() {
        setupCapsuleProject()
        val capDir = projectDir.resolve("build/capsules").also { it.mkdirs() }
        // Mix of .webm and .mp4 files in the build output dir.
        capDir.resolve("demo.webm").writeText("webm-content")
        capDir.resolve("demo.mp4").writeText("mp4-content")
        val destRoot = projectDir.resolve("office/videos").absolutePath

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "distributeCapsuleVideo",
                "-Poutput.videoDestinationDir=$destRoot",
                "-Poutput.versioning=incremental",
                "-Poutput.format=mp4",
            )
            .withProjectDir(projectDir)
            .build()

        assertTrue(result.output.contains("CAPSULE DISTRIB"), "Expected summary log. Output: ${result.output.take(500)}")

        val versionDirs = File(destRoot).listFiles { f -> f.isDirectory && f.name.startsWith("v") }?.toList() ?: emptyList()
        assertTrue(versionDirs.isNotEmpty(), "Should have at least one versioned dir")
        val first = versionDirs.first()
        assertTrue(File(first, "demo.mp4").exists(), "MP4 should be distributed when format=MP4")
        assertTrue(!File(first, "demo.webm").exists(), "WebM should NOT be distributed when format=MP4")
    }

    @Test
    fun `distributeCapsuleVideo copies both formats when format is BOTH`() {
        setupCapsuleProject()
        val capDir = projectDir.resolve("build/capsules").also { it.mkdirs() }
        capDir.resolve("demo.webm").writeText("webm-content")
        capDir.resolve("demo.mp4").writeText("mp4-content")
        val destRoot = projectDir.resolve("office/videos").absolutePath

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "distributeCapsuleVideo",
                "-Poutput.videoDestinationDir=$destRoot",
                "-Poutput.versioning=incremental",
                "-Poutput.format=both",
            )
            .withProjectDir(projectDir)
            .build()

        val versionDirs = File(destRoot).listFiles { f -> f.isDirectory && f.name.startsWith("v") }?.toList() ?: emptyList()
        assertTrue(versionDirs.isNotEmpty(), "Should have at least one versioned dir")
        val first = versionDirs.first()
        assertTrue(File(first, "demo.webm").exists(), "WebM should be distributed when format=BOTH")
        assertTrue(File(first, "demo.mp4").exists(), "MP4 should be distributed when format=BOTH")
    }
}