package capsule.transcript

import capsule.feed.SlideSegment
import capsule.feed.SlideType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [TranscriptBuilder] — CAP-TRANSCRIPT US-2.
 *
 * [TranscriptBuilder] is a pure object that assembles a deterministic AsciiDoc
 * article from a [TranscriptPlan]: a level-0 title (`= <deckName>`), a
 * `:language:` attribute, an intro boilerplate, one `== <title>` section per
 * HTML segment with the speaker note as body, and an outro boilerplate. MANIM
 * segments are skipped (animation-only slides have no place in a text
 * article). Zero I/O, zero LLM — fully testable.
 */
class TranscriptBuilderTest {

    private fun plan(
        deckName: String = "kotlin-basics",
        language: String = "fr",
        segments: List<SlideSegment> = listOf(
            SlideSegment(1, "Introduction", "Welcome to the capsule."),
            SlideSegment(2, "Core Concept", "The core concept is simple."),
        ),
    ): TranscriptPlan = TranscriptPlan(
        deckName = deckName,
        segments = segments,
        language = language,
        outputPath = "build/capsule/$deckName-transcript.adoc",
    )

    @Test
    fun `build produces level-0 title with deck name`() {
        val adoc = TranscriptBuilder.build(plan(deckName = "kotlin-basics"))
        assertTrue(adoc.startsWith("= kotlin-basics"), "AsciiDoc should start with level-0 title '= <deckName>'")
    }

    @Test
    fun `build produces language attribute`() {
        val adoc = TranscriptBuilder.build(plan(language = "fr"))
        assertTrue(adoc.contains(":language: fr"), "AsciiDoc should contain ':language: <lang>' attribute")
    }

    @Test
    fun `build contains intro boilerplate`() {
        val adoc = TranscriptBuilder.build(plan())
        assertTrue(TranscriptBuilder.INTRO.isNotBlank(), "INTRO constant must not be blank")
        assertTrue(adoc.contains(TranscriptBuilder.INTRO), "AsciiDoc should contain intro boilerplate")
    }

    @Test
    fun `build contains outro boilerplate`() {
        val adoc = TranscriptBuilder.build(plan())
        assertTrue(TranscriptBuilder.OUTRO.isNotBlank(), "OUTRO constant must not be blank")
        assertTrue(adoc.contains(TranscriptBuilder.OUTRO), "AsciiDoc should contain outro boilerplate")
    }

    @Test
    fun `build produces one section per HTML segment with title and speaker note`() {
        val adoc = TranscriptBuilder.build(plan())
        assertTrue(adoc.contains("== Introduction"), "should contain '== Introduction' section")
        assertTrue(adoc.contains("Welcome to the capsule."), "should contain first speaker note")
        assertTrue(adoc.contains("== Core Concept"), "should contain '== Core Concept' section")
        assertTrue(adoc.contains("The core concept is simple."), "should contain second speaker note")
    }

    @Test
    fun `build with empty segments produces title + language + intro + outro only`() {
        val adoc = TranscriptBuilder.build(plan(segments = emptyList()))
        assertTrue(adoc.startsWith("= kotlin-basics"))
        assertTrue(adoc.contains(":language: fr"))
        assertTrue(adoc.contains(TranscriptBuilder.INTRO))
        assertTrue(adoc.contains(TranscriptBuilder.OUTRO))
        assertFalse(adoc.contains("== "), "empty segments should produce no '==' section")
    }

    @Test
    fun `build skips MANIM segments`() {
        val segments = listOf(
            SlideSegment(1, "HTML Slide", "HTML narration.", SlideType.HTML),
            SlideSegment(2, "Manim Animation", "Animation narration.", SlideType.MANIM, "SceneOne"),
            SlideSegment(3, "Second HTML", "Second HTML narration.", SlideType.HTML),
        )
        val adoc = TranscriptBuilder.build(plan(segments = segments))
        assertTrue(adoc.contains("== HTML Slide"), "HTML slide should be present")
        assertTrue(adoc.contains("== Second HTML"), "second HTML slide should be present")
        assertFalse(adoc.contains("== Manim Animation"), "MANIM segment should be skipped")
        assertFalse(adoc.contains("Animation narration."), "MANIM speaker note should be skipped")
    }

    @Test
    fun `build with only MANIM segments produces no sections`() {
        val segments = listOf(
            SlideSegment(1, "Pure Animation", "Animation only.", SlideType.MANIM, "SceneOne"),
        )
        val adoc = TranscriptBuilder.build(plan(segments = segments))
        assertFalse(adoc.contains("== Pure Animation"), "MANIM-only plan should produce no section")
        assertTrue(adoc.contains(TranscriptBuilder.INTRO), "intro should still be present")
        assertTrue(adoc.contains(TranscriptBuilder.OUTRO), "outro should still be present")
    }

    @Test
    fun `build preserves segment order`() {
        val segments = listOf(
            SlideSegment(1, "First", "First body."),
            SlideSegment(2, "Second", "Second body."),
            SlideSegment(3, "Third", "Third body."),
        )
        val adoc = TranscriptBuilder.build(plan(segments = segments))
        val firstIdx = adoc.indexOf("== First")
        val secondIdx = adoc.indexOf("== Second")
        val thirdIdx = adoc.indexOf("== Third")
        assertTrue(firstIdx < secondIdx, "First section should come before Second")
        assertTrue(secondIdx < thirdIdx, "Second section should come before Third")
    }

    @Test
    fun `build output ends with outro`() {
        val adoc = TranscriptBuilder.build(plan()).trimEnd()
        assertTrue(adoc.endsWith(TranscriptBuilder.OUTRO), "AsciiDoc should end with outro boilerplate")
    }

    @Test
    fun `build is deterministic - same plan produces same output`() {
        val p = plan()
        assertEquals(TranscriptBuilder.build(p), TranscriptBuilder.build(p), "build should be deterministic")
    }
}