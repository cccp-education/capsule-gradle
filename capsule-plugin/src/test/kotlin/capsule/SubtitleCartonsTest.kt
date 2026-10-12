package capsule

import capsule.feed.SlideSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TDD unit tests for the PRT-03 subtitle helpers: [SubtitleTextNormalizer],
 * [SubtitleCartoner], [SubtitleTimingDistributor] and
 * [SubtitleTimingCalculator.calculateFromCartons].
 *
 * All pure — no I/O, no ffmpeg, no Gradle.
 */
class SubtitleCartonsTest {

    // ─── SubtitleTextNormalizer ────────────────────────────────────

    @Test
    fun `display strips markdown emphasis`() {
        assertEquals("des classes", SubtitleTextNormalizer.spokenToDisplay("des **classes**"))
    }

    @Test
    fun `display re-collects a spelled acronym`() {
        assertEquals("the API is ready", SubtitleTextNormalizer.spokenToDisplay("the A P I is ready"))
    }

    @Test
    fun `display collapses whitespace and trims`() {
        assertEquals("a b", SubtitleTextNormalizer.spokenToDisplay("  a   b  "))
    }

    @Test
    fun `display applies reversals first`() {
        assertEquals("n", SubtitleTextNormalizer.spokenToDisplay("ène", mapOf("ène" to "n")))
    }

    @Test
    fun `display keeps text unchanged without reversals`() {
        assertEquals("ène", SubtitleTextNormalizer.spokenToDisplay("ène"))
    }

    // ─── SubtitleCartoner ──────────────────────────────────────────

    @Test
    fun `blank text yields no carton`() {
        assertTrue(SubtitleCartoner.split("   ").isEmpty())
    }

    @Test
    fun `short text is a single carton`() {
        assertEquals(listOf("bonjour le monde"), SubtitleCartoner.split("bonjour le monde"))
    }

    @Test
    fun `long text wraps at the character limit`() {
        val cartons = SubtitleCartoner.split("un deux trois quatre cinq six sept huit neuf dix", maxChars = 15)
        assertTrue(cartons.size > 1, "expected several cartons, got $cartons")
        assertTrue(cartons.all { it.length <= 15 }, "every carton must be <= 15 chars: $cartons")
    }

    @Test
    fun `a word longer than the limit is never split`() {
        val cartons = SubtitleCartoner.split("supercalifragilistic court", maxChars = 5)
        assertTrue(cartons.contains("supercalifragilistic"), "the long word must stay whole: $cartons")
    }

    @Test
    fun `cartons rejoin the input words in order`() {
        val text = "un deux trois quatre cinq six sept huit neuf dix onze"
        val cartons = SubtitleCartoner.split(text, maxChars = 18)
        assertEquals(text, cartons.joinToString(" "))
    }

    @Test
    fun `sentence end breaks a long carton`() {
        val cartons = SubtitleCartoner.split("voici une phrase complete. puis une autre", maxChars = 42)
        assertEquals(listOf("voici une phrase complete.", "puis une autre"), cartons)
    }

    @Test
    fun `non positive limit is rejected`() {
        assertFailsWith<IllegalArgumentException> { SubtitleCartoner.split("x", maxChars = 0) }
    }

    // ─── SubtitleTimingDistributor ─────────────────────────────────

    @Test
    fun `distribution is proportional to length`() {
        val ranges = SubtitleTimingDistributor.distribute(listOf("aa", "bbbb"), start = 10.0, duration = 6.0)
        assertEquals(2, ranges.size)
        assertEquals(10.0, ranges[0].start, 1e-9)
        assertEquals(12.0, ranges[0].endInclusive, 1e-9)
        assertEquals(12.0, ranges[1].start, 1e-9)
        assertEquals(16.0, ranges[1].endInclusive, 1e-9)
    }

    @Test
    fun `distribution is contiguous and covers the take`() {
        val ranges = SubtitleTimingDistributor.distribute(listOf("a", "bb", "ccc"), start = 0.0, duration = 9.0)
        assertEquals(0.0, ranges.first().start, 1e-9)
        assertEquals(9.0, ranges.last().endInclusive, 1e-9)
        for (i in 0 until ranges.size - 1) {
            assertEquals(ranges[i].endInclusive, ranges[i + 1].start, 1e-9)
        }
    }

    @Test
    fun `empty cartons yield no range`() {
        assertTrue(SubtitleTimingDistributor.distribute(emptyList(), 0.0, 5.0).isEmpty())
    }

    @Test
    fun `zero duration yields zero length ranges at start`() {
        val ranges = SubtitleTimingDistributor.distribute(listOf("a", "b"), start = 3.0, duration = 0.0)
        assertTrue(ranges.all { it.start == 3.0 && it.endInclusive == 3.0 }, "got $ranges")
    }

    @Test
    fun `negative start is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            SubtitleTimingDistributor.distribute(listOf("a"), start = -1.0, duration = 1.0)
        }
    }

    // ─── SubtitleTimingCalculator.calculateFromCartons ─────────────

    private fun slide(index: Int, note: String) = SlideSegment(index = index, title = "s$index", narration = note)

    @Test
    fun `one cue per carton with sequential indices`() {
        val note = "Un deux trois quatre cinq six sept huit neuf dix onze douze"
        val entries = SubtitleTimingCalculator.calculateFromCartons(
            listOf(slide(0, note)),
            listOf(10.0),
            maxChars = 12,
        )
        assertTrue(entries.size > 1, "expected several cues, got ${entries.size}")
        assertEquals((1..entries.size).toList(), entries.map { it.index })
        assertEquals(10.0, entries.last().endTime, 1e-9)
        assertTrue(entries.all { it.startTime <= it.endTime })
    }

    @Test
    fun `cues are renumbered across slides`() {
        val entries = SubtitleTimingCalculator.calculateFromCartons(
            listOf(slide(0, "court"), slide(1, "Un deux trois quatre cinq six sept huit")),
            listOf(0.5, 0.5),
            maxChars = 12,
        )
        assertEquals((1..entries.size).toList(), entries.map { it.index })
        assertEquals(1.0, entries.last().endTime, 1e-9)
        assertTrue(entries.all { it.startTime >= 0.0 && it.endTime <= 1.0 })
    }

    @Test
    fun `cue text is normalised for display`() {
        val entries = SubtitleTimingCalculator.calculateFromCartons(
            listOf(slide(0, "des **classes** et l' A P I")),
            listOf(4.0),
            maxChars = 100,
        )
        assertEquals(1, entries.size)
        assertEquals("des classes et l' API", entries.single().text)
    }

    @Test
    fun `empty slides yield no cue`() {
        assertTrue(SubtitleTimingCalculator.calculateFromCartons(emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun `a note with no displayable text consumes its duration without a cue`() {
        // SlideSegment forbids a blank speakerNote, so the reachable "no cue"
        // case is a note that normalises to nothing (markdown emphasis only):
        // it still consumes its duration, the next slide must not start earlier.
        val entries = SubtitleTimingCalculator.calculateFromCartons(
            listOf(slide(0, "**"), slide(1, "bonjour")),
            listOf(2.0, 3.0),
            maxChars = 42,
        )
        assertEquals(1, entries.size)
        assertEquals(2.0, entries.single().startTime, 1e-9)
        assertEquals(5.0, entries.single().endTime, 1e-9)
    }
}
