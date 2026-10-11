package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional test for the PRT-07 timeline wiring in `CapsuleVideoTask`.
 *
 * Uses the NoOp TTS engine and the default capture engine; asserts the timeline
 * is computed and logged for the deck (no behaviour change — the timeline is
 * observable here).
 */
class TimelineFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    private fun setupBuild() {
        settingsFile.writeText("")
        buildFile.writeText(
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

    private fun writeScriptAndDeck() {
        val scriptDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        scriptDir.resolve("timeline-test-script.txt").writeText(
            """
            === CAPSULE SCRIPT : timeline-test ===
            --- SLIDE 1 : Title ---
            Note content for the timeline test.
            """.trimIndent()
        )
        val decksDir = projectDir.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        decksDir.resolve("timeline-test-deck.html").writeText(
            """
            <html><body>
            <div class="reveal">
              <div class="slides">
                <section data-capsule-slide="1"><h2>Title</h2></section>
              </div>
            </div>
            </body></html>
            """.trimIndent()
        )
    }

    @Test
    fun `generateCapsuleVideo logs a deterministic timeline`() {
        setupBuild()
        writeScriptAndDeck()

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideo")
            .withProjectDir(projectDir)
            .build()

        assertTrue(
            result.output.contains("Timeline:"),
            "Expected a timeline log line. Output: ${result.output.take(1200)}"
        )
    }
}
