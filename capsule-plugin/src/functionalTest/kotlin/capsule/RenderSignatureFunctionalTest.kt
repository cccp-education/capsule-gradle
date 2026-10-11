package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Functional tests for the PRT-06 render-signature wiring in `CapsuleVideoTask`.
 *
 * Uses the NoOp TTS engine (no real audio) and the default capture engine; the
 * NoOp capture writes a placeholder WebM, so the reuse DECODE gate stays closed
 * here — these tests assert the observable contract: the signature sidecar is
 * written next to the produced video and stays stable for unchanged inputs.
 */
class RenderSignatureFunctionalTest {

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
        scriptDir.resolve("sig-test-script.txt").writeText(
            """
            === CAPSULE SCRIPT : sig-test ===
            --- SLIDE 1 : Title ---
            Note content for the render signature test.
            """.trimIndent()
        )
        val decksDir = projectDir.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        decksDir.resolve("sig-test-deck.html").writeText(
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

    private fun run(vararg extra: String) =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(listOf("generateCapsuleVideo") + extra)
            .withProjectDir(projectDir)
            .build()

    @Test
    fun `render signature sidecar is written and stays stable across reuse runs`() {
        setupBuild()
        writeScriptAndDeck()

        run()

        val sigFile = projectDir.resolve("build/capsules/sig-test.webm.sig")
        assertTrue(sigFile.exists(), "PRT-06 signature sidecar must be written after a capture")
        val first = sigFile.readText()
        assertTrue(first.isNotBlank(), "signature must not be blank")

        run("-Pcapsule.capture.reuseCapturedVideoEnabled=true")

        assertEquals(first, sigFile.readText(), "signature must be stable for unchanged inputs")
    }
}
