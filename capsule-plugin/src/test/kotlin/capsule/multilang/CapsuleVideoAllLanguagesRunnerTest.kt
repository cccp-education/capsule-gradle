package capsule.multilang

import contracts.i18n.LanguageCatalog
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CapsuleVideoAllLanguagesRunnerTest {

    @TempDir
    lateinit var tmp: File

    private fun entry(lang: String, outputExists: Boolean = false): CapsuleVideoEntry {
        val language = LanguageCatalog.findByCode(lang)!!
        val deck = tmp.resolve("demo_$lang-deck.html")
        val script = tmp.resolve("demo_$lang-script.txt")
        deck.writeText("<html></html>")
        script.writeText("=== CAPSULE SCRIPT : demo_$lang ===\n--- SLIDE 1 : Title ---\nnote\n")
        val output = tmp.resolve("demo_$lang.webm")
        if (outputExists) {
            output.writeBytes(byteArrayOf(0x1a.toByte(), 0x45.toByte(), 0xdf.toByte(), 0xa3.toByte()))
        }
        return CapsuleVideoEntry(deckFile = deck, scriptFile = script, language = language, outputVideo = output)
    }

    private fun planOf(vararg languages: Pair<String, Boolean>): CapsuleVideoPlan =
        CapsuleVideoPlan(languages.map { (lang, exists) -> entry(lang, exists) })

    private val zeroProbe: (File) -> Double = { 0.0 }

    @Test
    fun `renders every entry when no output exists`() {
        val plan = planOf("fr" to false, "en" to false, "ar" to false)
        val rendered = mutableListOf<CapsuleVideoEntry>()

        val result = CapsuleVideoAllLanguagesRunner.run(plan, zeroProbe) { rendered.add(it) }

        assertEquals(3, result.size)
        assertEquals(3, rendered.size)
        assertEquals(listOf("fr", "en", "ar"), rendered.map { it.language.code })
    }

    @Test
    fun `skips entry whose output webm already exists and probes valid`() {
        val plan = planOf("fr" to true)
        val rendered = mutableListOf<CapsuleVideoEntry>()
        val validProbe: (File) -> Double = { 4.2 }

        val result = CapsuleVideoAllLanguagesRunner.run(plan, validProbe) { rendered.add(it) }

        assertTrue(result.isEmpty())
        assertTrue(rendered.isEmpty())
    }

    @Test
    fun `re-renders entry whose output webm exists but probes invalid`() {
        val plan = planOf("fr" to true)
        val rendered = mutableListOf<CapsuleVideoEntry>()

        val result = CapsuleVideoAllLanguagesRunner.run(plan, zeroProbe) { rendered.add(it) }

        assertEquals(1, result.size)
        assertEquals("fr", rendered.single().language.code)
    }

    @Test
    fun `mixed plan renders only missing or invalid entries preserving order`() {
        val plan = planOf("fr" to true, "en" to false, "ar" to true, "ru" to false)
        val rendered = mutableListOf<CapsuleVideoEntry>()
        val probeByLang: (File) -> Double = { file ->
            if (file.name.contains("_fr")) 2.0 else 0.0
        }

        val result = CapsuleVideoAllLanguagesRunner.run(plan, probeByLang) { rendered.add(it) }

        assertEquals(listOf("en", "ar", "ru"), result.map { it.language.code })
        assertEquals(listOf("en", "ar", "ru"), rendered.map { it.language.code })
    }

    @Test
    fun `shouldRender is true when output is missing`() {
        val plan = planOf("fr" to false)
        assertTrue(CapsuleVideoAllLanguagesRunner.shouldRender(plan.entries.first(), zeroProbe))
    }

    @Test
    fun `shouldRender is false when output exists and probes valid`() {
        val plan = planOf("fr" to true)
        val validProbe: (File) -> Double = { 3.7 }
        assertTrue(!CapsuleVideoAllLanguagesRunner.shouldRender(plan.entries.first(), validProbe))
    }
}
