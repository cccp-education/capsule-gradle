package capsule.viral

import capsule.feed.SlideType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * TDD unit tests for the storyboard parser (CAP-VIRAL wiring):
 * [ViralStoryboardParser].
 *
 * The parser is the inverse of [ViralStoryboardBuilder]: it reads the
 * human-authored/validated storyboard AsciiDoc back into a [ViralStoryboard]
 * so the Gradle task drives the rendering from a document, not from props.
 *
 * Round-trip invariant: `parse(build(sb)) == sb` for every valid storyboard.
 */
class ViralStoryboardParserTest {

    private fun storyboard() = ViralStoryboard(
        message = "Devenir formateur professionnel",
        angle = "Le métier en 30 secondes",
        beats = listOf(
            StoryboardBeat("hook", "Et si 30 secondes suffisaient ?", 3.0),
            StoryboardBeat("development", "Les 4 activités types", 10.0),
            StoryboardBeat("development", "Le rôle du formateur", 10.0),
            StoryboardBeat("cta", "Découvrez FPA", 7.0),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 30,
    )

    @Test
    fun `parse is the inverse of build for a plain storyboard`() {
        val source = storyboard()
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source, parsed)
    }

    @Test
    fun `parse round-trips a manim beat with its scene`() {
        val source = storyboard().copy(
            beats = listOf(
                StoryboardBeat("hook", "h", 3.0),
                StoryboardBeat("development", "d", 24.0, type = SlideType.MANIM, manimScene = "MoveSquare"),
                StoryboardBeat("cta", "c", 3.0),
            ),
        )
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source, parsed)
        assertEquals(SlideType.MANIM, parsed.beats[1].type)
        assertEquals("MoveSquare", parsed.beats[1].manimScene)
    }

    @Test
    fun `parse round-trips a beat asset`() {
        val source = storyboard().copy(
            beats = listOf(
                StoryboardBeat("hook", "h", 3.0),
                StoryboardBeat("development", "d", 20.0, asset = "mind-map.png"),
                StoryboardBeat("cta", "c", 7.0),
            ),
        )
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source, parsed)
        assertEquals("mind-map.png", parsed.beats[1].asset)
    }

    @Test
    fun `parse returns null on a document with no storyboard header`() {
        assertNull(ViralStoryboardParser.parse("just some text\nwith no header"))
    }

    @Test
    fun `parse returns null when the target duration is out of the short range`() {
        val broken = ViralStoryboardBuilder.build(storyboard()).replace(":target-duration: 30", ":target-duration: 300")
        assertNull(ViralStoryboardParser.parse(broken))
    }

    @Test
    fun `parse returns null when there is no beat`() {
        val broken = """
            = Storyboard — Devenir formateur professionnel
            :platform: TIKTOK
            :language: fr
            :target-duration: 30

            == Angle

            Le métier en 30 secondes

            == Beats
        """.trimIndent()
        assertNull(ViralStoryboardParser.parse(broken))
    }
}
