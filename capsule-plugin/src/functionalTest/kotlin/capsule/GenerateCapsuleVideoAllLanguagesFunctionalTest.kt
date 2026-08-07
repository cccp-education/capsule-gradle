package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class GenerateCapsuleVideoAllLanguagesFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val targetLanguages = listOf("fr", "en", "ar")

    private fun setup() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
        """.trimIndent())

        val scriptDir = projectDir.resolve("build/capsule").also { it.mkdirs() }
        targetLanguages.forEach { lang ->
            scriptDir.resolve("demo_$lang-script.txt").writeText("""
=== CAPSULE SCRIPT : demo_$lang ===
--- SLIDE 1 : Title ---
Note content in $lang.
            """.trimIndent())
        }

        val decksDir = projectDir.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        targetLanguages.forEach { lang ->
            decksDir.resolve("demo_$lang-deck.html").writeText("""
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Title</h2></section>
  </div>
</div>
</body></html>
            """.trimIndent())
        }
    }

    @Test
    fun `produces one webm per language with noop engines`() {
        setup()

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("generateCapsuleVideoAllLanguages")
        runner.withProjectDir(projectDir)
        val result = runner.build()

        assertTrue(result.output.contains("CAPSULE MULTILANG"), "Expected summary log line")
        targetLanguages.forEach { lang ->
            val webm = projectDir.resolve("build/capsules/demo_$lang.webm")
            assertTrue(webm.exists(), "Expected ${webm.absolutePath} to exist")
        }
    }
}
