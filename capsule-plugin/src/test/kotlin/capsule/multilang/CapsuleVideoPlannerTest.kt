package capsule.multilang

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CapsuleVideoPlannerTest {

    @TempDir
    lateinit var tempDir: Path

    private fun deckDir() = tempDir.resolve("decks").toFile().apply { mkdirs() }
    private fun scriptDir() = tempDir.resolve("scripts").toFile().apply { mkdirs() }
    private fun outputDir() = tempDir.resolve("out").toFile().apply { mkdirs() }

    private fun writeDeck(deckName: String, code: String) {
        deckDir().resolve("${deckName}_$code-deck.html").writeText("<html></html>")
    }

    private fun writeScript(deckName: String, code: String) {
        scriptDir().resolve("${deckName}_$code-script.txt").writeText("script")
    }

    private fun languages(vararg codes: String): List<SupportedLanguage> =
        codes.map { LanguageCatalog.findByCode(it)!! }

    @Test
    fun `plan discovers decks and scripts for three languages`() {
        writeDeck("kotlin-basics", "fr")
        writeScript("kotlin-basics", "fr")
        writeDeck("kotlin-basics", "en")
        writeScript("kotlin-basics", "en")
        writeDeck("kotlin-basics", "ar")
        writeScript("kotlin-basics", "ar")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr", "en", "ar"),
        )

        assertEquals(3, plan.size())
        val codes = plan.languages().map { it.code }.toSet()
        assertEquals(setOf("fr", "en", "ar"), codes)
    }

    @Test
    fun `plan skips language when deck file is missing`() {
        writeScript("kotlin-basics", "fr")
        writeDeck("kotlin-basics", "en")
        writeScript("kotlin-basics", "en")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr", "en"),
        )

        assertEquals(1, plan.size())
        assertEquals("en", plan.languages().first().code)
    }

    @Test
    fun `plan skips language when script file is missing`() {
        writeDeck("kotlin-basics", "fr")
        writeDeck("kotlin-basics", "en")
        writeScript("kotlin-basics", "en")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr", "en"),
        )

        assertEquals(1, plan.size())
        assertEquals("en", plan.languages().first().code)
    }

    @Test
    fun `plan throws when no language produces matching files`() {
        writeDeck("kotlin-basics", "fr")
        writeScript("kotlin-basics", "fr")

        assertThrows(IllegalArgumentException::class.java) {
            CapsuleVideoPlanner.plan(
                deckDir = deckDir(),
                scriptDir = scriptDir(),
                outputDir = outputDir(),
                targetLanguages = languages("en", "ar"),
            )
        }
    }

    @Test
    fun `plan output video naming follows deckName_lang webm`() {
        writeDeck("kotlin-basics", "fr")
        writeScript("kotlin-basics", "fr")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr"),
        )

        val entry = plan.entries.first()
        assertEquals("kotlin-basics_fr.webm", entry.outputVideo.name)
    }

    @Test
    fun `plan preserves deck file path in entry`() {
        writeDeck("kotlin-basics", "fr")
        writeScript("kotlin-basics", "fr")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr"),
        )

        val entry = plan.entries.first()
        assertTrue(entry.deckFile.exists())
        assertTrue(entry.deckFile.name.endsWith("_fr-deck.html"))
    }

    @Test
    fun `plan preserves script file path in entry`() {
        writeDeck("kotlin-basics", "fr")
        writeScript("kotlin-basics", "fr")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("fr"),
        )

        val entry = plan.entries.first()
        assertTrue(entry.scriptFile.exists())
        assertTrue(entry.scriptFile.name.endsWith("_fr-script.txt"))
    }

    @Test
    fun `plan handles RTL language Arabic`() {
        writeDeck("kotlin-basics", "ar")
        writeScript("kotlin-basics", "ar")

        val plan = CapsuleVideoPlanner.plan(
            deckDir = deckDir(),
            scriptDir = scriptDir(),
            outputDir = outputDir(),
            targetLanguages = languages("ar"),
        )

        val entry = plan.entries.first()
        assertTrue(entry.language.rtl, "Arabic should be RTL")
        assertEquals("kotlin-basics_ar.webm", entry.outputVideo.name)
    }
}