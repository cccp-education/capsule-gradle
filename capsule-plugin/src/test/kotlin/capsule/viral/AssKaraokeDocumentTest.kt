package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the ASS karaoke document builder (CAP-VIRAL-E2E US-3): the
 * full `.ass` file (script info, style, one dialogue per line).
 */
class AssKaraokeDocumentTest {

    private fun line(words: List<Pair<String, Pair<Double, Double>>>) = KaraokeLine(
        words = words.map { (text, span) -> KaraokeWord(text, span.first, span.second) },
        startTime = words.firstOrNull()?.second?.first ?: 0.0,
        endTime = words.lastOrNull()?.second?.second ?: 0.0,
    )

    @Test
    fun `document declares the portrait script info and a style`() {
        val doc = AssKaraokeDocument.build(emptyList())
        assertTrue(doc.startsWith("[Script Info]"))
        assertTrue(doc.contains("PlayResX: 1080"))
        assertTrue(doc.contains("PlayResY: 1920"))
        assertTrue(doc.contains("[V4+ Styles]"))
        assertTrue(doc.contains("Style: Viral,"))
        assertTrue(doc.contains("[Events]"))
    }

    @Test
    fun `one dialogue per non-empty line with karaoke overrides and timestamps`() {
        val doc = AssKaraokeDocument.build(
            listOf(
                line(listOf("Et" to (0.0 to 0.2), "si" to (0.2 to 0.5))),
                line(listOf("CTA" to (0.5 to 1.0))),
            ),
        )
        assertEquals(2, Regex("^Dialogue: ", RegexOption.MULTILINE).findAll(doc).count())
        assertTrue(doc.contains("Dialogue: 0,0:00:00.00,0:00:00.50,Viral,,0,0,0,,{\\k20}Et {\\k30}si"))
        assertTrue(doc.contains("Dialogue: 0,0:00:00.50,0:00:01.00,Viral,,0,0,0,,{\\k50}CTA"))
    }

    @Test
    fun `lines without words are skipped`() {
        val doc = AssKaraokeDocument.build(
            listOf(
                line(listOf("Mot" to (0.0 to 1.0))),
                KaraokeLine(words = emptyList(), startTime = 1.0, endTime = 2.0),
            ),
        )
        assertEquals(1, Regex("^Dialogue: ", RegexOption.MULTILINE).findAll(doc).count())
    }

    @Test
    fun `document is deterministic`() {
        val lines = listOf(line(listOf("Mot" to (0.0 to 1.0))))
        assertEquals(AssKaraokeDocument.build(lines), AssKaraokeDocument.build(lines))
    }

    @Test
    fun `file name follows the variant caption convention`() {
        assertEquals("fr-tiktok-30.ass", KaraokeSubtitleBuilder.fileNameFor("fr-tiktok-30"))
    }
}
