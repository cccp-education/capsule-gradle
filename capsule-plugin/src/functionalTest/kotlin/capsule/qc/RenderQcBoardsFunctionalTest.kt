package capsule.qc

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for `renderQcBoards` (PRT-08).
 *
 * Verifies registration, the disabled-by-default no-op, and that an enabled run
 * writes the boards (Java2D, no external binary).
 */
class RenderQcBoardsFunctionalTest {

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

    private fun writeScript() {
        val capsuleDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        capsuleDir.resolve("demo-script.txt").writeText(
            """
            === CAPSULE SCRIPT : demo ===

            --- SLIDE 1 : Intro ---
            bonjour le monde
            """.trimIndent()
        )
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
            result.output.contains("renderQcBoards"),
            "Expected renderQcBoards in verification group, got: ${result.output}"
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("renderQcBoards")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("qc.boardsEnabled=false"),
            "Expected a skip log when disabled by default, got: ${result.output}"
        )
    }

    @Test
    fun `enabled writes the boards`() {
        setupBuild(
            """
            capsule {
                qcBoardsEnabled = true
            }
            """.trimIndent()
        )
        writeScript()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("renderQcBoards")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("CAPSULE QC BOARDS"),
            "Expected the boards to be rendered, got: ${result.output}"
        )
        assertTrue(
            projectDir.resolve("build/capsule/qc/demo-beats.png").isFile,
            "Expected the beats board to be written"
        )
    }
}
