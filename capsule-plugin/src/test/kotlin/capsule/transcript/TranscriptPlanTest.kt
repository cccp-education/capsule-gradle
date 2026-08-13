package capsule.transcript

import capsule.feed.SlideSegment
import capsule.feed.SlideType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TDD unit tests for [TranscriptPlan] — CAP-TRANSCRIPT US-2.
 *
 * [TranscriptPlan] is an immutable data class aggregating the inputs needed
 * to build a transcript AsciiDoc article: the deck name, the ordered slide
 * segments (reusing [capsule.feed.SlideSegment] — no new model), the target
 * language code, and the output path. Invariants fail-fast on blank
 * `deckName`, `language`, and `outputPath`. An empty segment list is allowed
 * (a deck with zero narrated slides still produces an intro+outro article).
 */
class TranscriptPlanTest {

    private fun segment(
        index: Int = 1,
        title: String = "Introduction",
        speakerNote: String = "Welcome to the capsule.",
        type: SlideType = SlideType.HTML,
    ): SlideSegment = SlideSegment(
        index = index,
        title = title,
        speakerNote = speakerNote,
        type = type,
    )

    @Test
    fun `plan preserves all fields with non-empty segments`() {
        val segments = listOf(
            segment(1, "Intro", "Welcome."),
            segment(2, "Core", "The core concept."),
        )
        val plan = TranscriptPlan(
            deckName = "kotlin-basics",
            segments = segments,
            language = "fr",
            outputPath = "build/capsule/kotlin-basics-transcript.adoc",
        )
        assertEquals("kotlin-basics", plan.deckName)
        assertEquals(segments, plan.segments)
        assertEquals("fr", plan.language)
        assertEquals("build/capsule/kotlin-basics-transcript.adoc", plan.outputPath)
    }

    @Test
    fun `plan with empty segments is valid`() {
        val plan = TranscriptPlan(
            deckName = "empty-deck",
            segments = emptyList(),
            language = "en",
            outputPath = "build/capsule/empty-deck-transcript.adoc",
        )
        assertTrue(plan.segments.isEmpty(), "empty segments list should be allowed")
    }

    @Test
    fun `plan with blank deckName throws IllegalArgumentException`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            TranscriptPlan(
                deckName = "  ",
                segments = listOf(segment()),
                language = "fr",
                outputPath = "out.adoc",
            )
        }
        assertTrue(ex.message!!.contains("deckName", ignoreCase = true))
    }

    @Test
    fun `plan with blank language throws IllegalArgumentException`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            TranscriptPlan(
                deckName = "deck",
                segments = listOf(segment()),
                language = "",
                outputPath = "out.adoc",
            )
        }
        assertTrue(ex.message!!.contains("language", ignoreCase = true))
    }

    @Test
    fun `plan with blank outputPath throws IllegalArgumentException`() {
        val ex = assertFailsWith<IllegalArgumentException> {
            TranscriptPlan(
                deckName = "deck",
                segments = listOf(segment()),
                language = "fr",
                outputPath = "",
            )
        }
        assertTrue(ex.message!!.contains("outputPath", ignoreCase = true))
    }

    @Test
    fun `plan is a data class with equals by value`() {
        val segments = listOf(segment())
        val a = TranscriptPlan("deck", segments, "fr", "out.adoc")
        val b = TranscriptPlan("deck", segments, "fr", "out.adoc")
        assertEquals(a, b, "data class equals should be by value")
    }

    @Test
    fun `plan copy preserves unmodified fields`() {
        val plan = TranscriptPlan("deck", listOf(segment()), "fr", "out.adoc")
        val copy = plan.copy(language = "en")
        assertEquals("en", copy.language)
        assertEquals("deck", copy.deckName, "deckName preserved on copy")
        assertEquals("out.adoc", copy.outputPath, "outputPath preserved on copy")
    }
}