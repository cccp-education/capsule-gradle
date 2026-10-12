package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for ASR word timings (CAP-QC-VOICE US-5): the whisper JSON
 * parser and the karaoke planner overload that uses real timings with a
 * proportional fallback. Pure — no I/O.
 */
class WordTimingsTest {

    private fun beat(intent: String, duration: Double, role: String = "development") =
        StoryboardBeat(role = role, intent = intent, durationSeconds = duration)

    @Test
    fun `parses the whisper segments words shape`() {
        val json = """
            { "segments": [ { "words": [
              { "word": "Bonjour", "start": 0.1, "end": 0.6 },
              { "word": "le", "start": 0.7, "end": 0.9 }
            ] } ] }
        """.trimIndent()

        val words = WordTimingsJson.parse(json)

        assertEquals(listOf("bonjour", "le"), words.map { it.text })
        assertEquals(0.1, words[0].start, 1e-9)
        assertEquals(0.9, words[1].end, 1e-9)
    }

    @Test
    fun `a malformed or blank document yields no timing`() {
        assertTrue(WordTimingsJson.parse("").isEmpty())
        assertTrue(WordTimingsJson.parse("not json").isEmpty())
        assertTrue(WordTimingsJson.parse("{\"segments\":[]}").isEmpty())
    }

    @Test
    fun `the planner uses measured timings when every word matches`() {
        val beats = listOf(beat("Bonjour le monde", 3.0))
        val timings = listOf(
            TimedWord("bonjour", 0.1, 0.6),
            TimedWord("le", 0.7, 0.9),
            TimedWord("monde", 1.0, 1.8),
        )

        val line = KaraokeWordPlanner.plan(beats, timings)[0]

        assertEquals(0.1, line.words[0].startTime, 1e-9)
        assertEquals(0.6, line.words[0].endTime, 1e-9)
        assertEquals(1.8, line.words[2].endTime, 1e-9)
    }

    @Test
    fun `measured timings are clamped to the beat window`() {
        val beats = listOf(beat("Bonjour le monde", 1.0))
        val timings = listOf(
            TimedWord("bonjour", 0.1, 0.6),
            TimedWord("le", 0.7, 0.9),
            TimedWord("monde", 1.0, 1.8),
        )

        val line = KaraokeWordPlanner.plan(beats, timings)[0]

        assertEquals(line.endTime, line.words[2].endTime, 1e-9)
        assertTrue(line.words.all { it.startTime >= line.startTime && it.endTime <= line.endTime })
    }

    @Test
    fun `an unmatched word falls back to proportional timing`() {
        val beats = listOf(beat("Bonjour le monde", 3.0))
        val timings = listOf(TimedWord("bonjour", 0.1, 0.6), TimedWord("monde", 1.0, 1.8))

        val line = KaraokeWordPlanner.plan(beats, timings)[0]

        // Fallback: contiguous, covering the whole beat window.
        assertEquals(line.startTime, line.words.first().startTime, 1e-9)
        assertEquals(line.endTime, line.words.last().endTime, 1e-9)
        assertEquals(3.0, line.words.sumOf { it.durationSeconds }, 1e-9)
    }
}
