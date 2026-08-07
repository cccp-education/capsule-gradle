package capsule.multilang

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class CapsuleVideoPlanTest {

    @TempDir
    lateinit var tempDir: Path

    private fun touch(relativePath: String): File {
        val file = tempDir.resolve(relativePath).toFile()
        file.parentFile?.mkdirs()
        file.writeText("dummy")
        return file
    }

    private fun supportedLanguage(code: String): SupportedLanguage =
        LanguageCatalog.findByCode(code)!!

    private fun entry(
        code: String = "fr",
        deckName: String = "kotlin-basics",
    ): CapsuleVideoEntry {
        val deckFile = touch("decks/${deckName}_$code-deck.html")
        val scriptFile = touch("scripts/${deckName}_$code-script.txt")
        val outputVideo = tempDir.resolve("out/${deckName}_$code.webm").toFile()
        return CapsuleVideoEntry(
            deckFile = deckFile,
            scriptFile = scriptFile,
            language = supportedLanguage(code),
            outputVideo = outputVideo,
        )
    }

    @Test
    fun `plan with single entry is valid`() {
        val plan = CapsuleVideoPlan(listOf(entry("fr")))
        assertEquals(1, plan.size())
    }

    @Test
    fun `plan with multiple entries is valid`() {
        val plan = CapsuleVideoPlan(listOf(
            entry("fr"),
            entry("en"),
            entry("ar"),
        ))
        assertEquals(3, plan.size())
    }

    @Test
    fun `plan with empty entries throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoPlan(emptyList())
        }
        assertTrue(ex.message!!.contains("at least one", ignoreCase = true))
    }

    @Test
    fun `plan languages returns list of entry languages`() {
        val plan = CapsuleVideoPlan(listOf(entry("fr"), entry("en")))
        val codes = plan.languages().map { it.code }
        assertEquals(listOf("fr", "en"), codes)
    }

    @Test
    fun `plan isEmpty returns false for non-empty plan`() {
        val plan = CapsuleVideoPlan(listOf(entry("fr")))
        assertFalse(plan.isEmpty())
    }

    @Test
    fun `plan isEmpty returns true for empty list before invariant throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoPlan(emptyList()).isEmpty()
        }
    }

    @Test
    fun `entry with non-existent deck file throws IllegalArgumentException`() {
        val scriptFile = touch("scripts/kotlin-basics_fr-script.txt")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoEntry(
                deckFile = tempDir.resolve("decks/missing_fr-deck.html").toFile(),
                scriptFile = scriptFile,
                language = supportedLanguage("fr"),
                outputVideo = tempDir.resolve("out/kotlin-basics_fr.webm").toFile(),
            )
        }
        assertTrue(ex.message!!.contains("deck", ignoreCase = true))
    }

    @Test
    fun `entry with non-existent script file throws IllegalArgumentException`() {
        val deckFile = touch("decks/kotlin-basics_fr-deck.html")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoEntry(
                deckFile = deckFile,
                scriptFile = tempDir.resolve("scripts/missing_fr-script.txt").toFile(),
                language = supportedLanguage("fr"),
                outputVideo = tempDir.resolve("out/kotlin-basics_fr.webm").toFile(),
            )
        }
        assertTrue(ex.message!!.contains("script", ignoreCase = true))
    }

    @Test
    fun `entry with unknown language code throws IllegalArgumentException`() {
        val deckFile = touch("decks/kotlin-basics_xx-deck.html")
        val scriptFile = touch("scripts/kotlin-basics_xx-script.txt")
        val ex = assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoEntry(
                deckFile = deckFile,
                scriptFile = scriptFile,
                language = SupportedLanguage(code = "xx", name = "Unknown", nativeName = "Unknown"),
                outputVideo = tempDir.resolve("out/kotlin-basics_xx.webm").toFile(),
            )
        }
        assertTrue(ex.message!!.contains("catalog", ignoreCase = true))
    }

    @Test
    fun `entry preserves all fields`() {
        val deckFile = touch("decks/kotlin-basics_fr-deck.html")
        val scriptFile = touch("scripts/kotlin-basics_fr-script.txt")
        val outputVideo = tempDir.resolve("out/kotlin-basics_fr.webm").toFile()
        val language = supportedLanguage("fr")
        val entry = CapsuleVideoEntry(deckFile, scriptFile, language, outputVideo)
        assertEquals(deckFile, entry.deckFile)
        assertEquals(scriptFile, entry.scriptFile)
        assertEquals(language, entry.language)
        assertEquals(outputVideo, entry.outputVideo)
    }
}