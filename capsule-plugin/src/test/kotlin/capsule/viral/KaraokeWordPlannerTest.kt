package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the karaoke word planner (CAP-VIRAL-E2E US-3): the pure
 * timeline that turns storyboard beats into word-by-word karaoke lines.
 */
class KaraokeWordPlannerTest {

    private fun beat(intent: String, duration: Double, role: String = "development") =
        StoryboardBeat(role = role, intent = intent, durationSeconds = duration)

    @Test
    fun `produces one line per beat spanning its window`() {
        val lines = KaraokeWordPlanner.plan(
            listOf(beat("Bonjour le monde", 3.0), beat("Au revoir", 2.0, role = "cta")),
        )
        assertEquals(2, lines.size)
        assertEquals(0.0, lines[0].startTime)
        assertEquals(3.0, lines[0].endTime, absoluteTolerance = 1e-9)
        assertEquals(3.0, lines[1].startTime, absoluteTolerance = 1e-9)
        assertEquals(5.0, lines[1].endTime, absoluteTolerance = 1e-9)
    }

    @Test
    fun `splits the beat intent into words`() {
        val lines = KaraokeWordPlanner.plan(listOf(beat("Bonjour le monde", 3.0)))
        assertEquals(listOf("Bonjour", "le", "monde"), lines[0].words.map { it.text })
    }

    @Test
    fun `word times are contiguous and cover the beat window`() {
        val line = KaraokeWordPlanner.plan(listOf(beat("Bonjour le monde", 3.0)))[0]
        assertEquals(line.startTime, line.words.first().startTime, absoluteTolerance = 1e-9)
        assertEquals(line.endTime, line.words.last().endTime, absoluteTolerance = 1e-9)
        line.words.zipWithNext().forEach { (a, b) ->
            assertEquals(a.endTime, b.startTime, absoluteTolerance = 1e-9)
        }
        assertEquals(3.0, line.words.sumOf { it.durationSeconds }, absoluteTolerance = 1e-9)
    }

    @Test
    fun `longer words get more time`() {
        val line = KaraokeWordPlanner.plan(listOf(beat("a loooooooong", 10.0)))[0]
        assertTrue(line.words[1].durationSeconds > line.words[0].durationSeconds)
    }

    @Test
    fun `is deterministic`() {
        val beats = listOf(beat("Bonjour le monde", 3.0))
        assertEquals(KaraokeWordPlanner.plan(beats), KaraokeWordPlanner.plan(beats))
    }
}
