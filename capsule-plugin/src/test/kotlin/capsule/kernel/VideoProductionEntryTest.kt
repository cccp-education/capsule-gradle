package capsule.kernel

import capsule.multilang.deckFile
import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * TDD — engine-neutral [VideoProductionEntry] (CAP-ENGINE-DECOUPLING US-1).
 */
class VideoProductionEntryTest {

    @TempDir
    lateinit var tempDir: Path

    private fun touch(relativePath: String): File {
        val file = tempDir.resolve(relativePath).toFile()
        file.parentFile?.mkdirs()
        file.writeText("dummy")
        return file
    }

    private fun language(code: String): SupportedLanguage = LanguageCatalog.findByCode(code)!!

    @Test
    fun `binds a visual source, a script, a language and an output`() {
        val visualFile = touch("visuals/demo_en-deck.html")
        val scriptFile = touch("scripts/demo_en-script.txt")
        val output = tempDir.resolve("out/demo_en.webm").toFile()

        val entry = VideoProductionEntry(
            visual = FileVisualSource(visualFile),
            scriptFile = scriptFile,
            language = language("en"),
            outputVideo = output,
        )

        assertThat(entry.visual.file).isEqualTo(visualFile)
        assertThat(entry.scriptFile).isEqualTo(scriptFile)
        assertThat(entry.outputVideo).isEqualTo(output)
    }

    @Test
    fun `rejects a missing visual source`() {
        val scriptFile = touch("scripts/demo_en-script.txt")

        assertThrows<IllegalArgumentException> {
            VideoProductionEntry(
                visual = FileVisualSource(tempDir.resolve("visuals/missing-deck.html").toFile()),
                scriptFile = scriptFile,
                language = language("en"),
                outputVideo = tempDir.resolve("out/demo_en.webm").toFile(),
            )
        }
    }

    @Test
    fun `rejects a missing script file`() {
        val visualFile = touch("visuals/demo_en-deck.html")

        assertThrows<IllegalArgumentException> {
            VideoProductionEntry(
                visual = FileVisualSource(visualFile),
                scriptFile = tempDir.resolve("scripts/missing-script.txt").toFile(),
                language = language("en"),
                outputVideo = tempDir.resolve("out/demo_en.webm").toFile(),
            )
        }
    }

    @Test
    fun `rejects a language outside the N0 catalog`() {
        val visualFile = touch("visuals/demo_xx-deck.html")
        val scriptFile = touch("scripts/demo_xx-script.txt")

        assertThrows<IllegalArgumentException> {
            VideoProductionEntry(
                visual = FileVisualSource(visualFile),
                scriptFile = scriptFile,
                language = SupportedLanguage(code = "xx", name = "Unknown", nativeName = "Unknown"),
                outputVideo = tempDir.resolve("out/demo_xx.webm").toFile(),
            )
        }
    }

    @Test
    fun `legacy deck-file accessor reflects the visual source file`() {
        val visualFile = touch("visuals/demo_en-deck.html")
        val scriptFile = touch("scripts/demo_en-script.txt")

        val entry = VideoProductionEntry(
            visual = FileVisualSource(visualFile),
            scriptFile = scriptFile,
            language = language("en"),
            outputVideo = tempDir.resolve("out/demo_en.webm").toFile(),
        )

        @Suppress("DEPRECATION")
        assertThat(entry.deckFile).isEqualTo(visualFile)
    }
}
