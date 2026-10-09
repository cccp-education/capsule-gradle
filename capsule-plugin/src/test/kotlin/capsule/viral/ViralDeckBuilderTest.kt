package capsule.viral

import capsule.feed.SlideType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral deck builder (CAP-SHORT US-1a):
 * [ViralDeckBuilder].
 *
 * The storyboard (CAP-VIRAL US-13) is a *document* the pilot validates; it is not
 * a capturable deck. US-1a bridges the two: it derives a self-contained portrait
 * 1080×1920 HTML deck — one `<section>` per beat — that the deterministic FFmpeg
 * screenshot socle (`ScreenshotPlanner` / `ScreenshotCaptureImpl`) can capture.
 *
 * Pure — no I/O, no browser, no FFmpeg.
 */
class ViralDeckBuilderTest {

    private fun storyboard(
        visualIdentity: VisualIdentity? = null,
        beats: List<StoryboardBeat> = listOf(
            StoryboardBeat("hook", "Un site perso, sans HTML", 3.0),
            StoryboardBeat("development", "Scaffold bakery", 10.0),
            StoryboardBeat("cta", "bakery-gradle + opencode", 7.0),
        ),
    ) = ViralStoryboard(
        message = "Créer son site perso avec bakery + opencode",
        angle = "L'agent écrit, Gradle bake",
        beats = beats,
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 20,
        visualIdentity = visualIdentity,
    )

    @Test
    fun `deck has exactly one section per beat`() {
        val deck = ViralDeckBuilder.build(storyboard())
        assertEquals(3, Regex("<section").findAll(deck).count())
    }

    @Test
    fun `deck is a portrait 1080x1920 document`() {
        val deck = ViralDeckBuilder.build(storyboard())
        assertTrue(deck.startsWith("<!DOCTYPE html>"), "expected an HTML document")
        assertTrue(deck.contains("width=1080, height=1920"), "expected the portrait viewport meta")
        assertTrue(deck.contains("--capsule-width: 1080px"))
        assertTrue(deck.contains("--capsule-height: 1920px"))
    }

    @Test
    fun `deck exposes the role and the intent of each beat`() {
        val deck = ViralDeckBuilder.build(storyboard())
        assertTrue(deck.contains("data-role=\"hook\""))
        assertTrue(deck.contains("data-role=\"development\""))
        assertTrue(deck.contains("data-role=\"cta\""))
        assertTrue(deck.contains("Un site perso, sans HTML"))
        assertTrue(deck.contains("bakery-gradle + opencode"))
    }

    @Test
    fun `deck carries the beat duration for the render`() {
        val deck = ViralDeckBuilder.build(storyboard())
        assertTrue(deck.contains("data-duration=\"3.0\""))
        assertTrue(deck.contains("data-duration=\"10.0\""))
    }

    @Test
    fun `deck marks a manim beat with its scene and asset`() {
        val deck = ViralDeckBuilder.build(
            storyboard(
                beats = listOf(
                    StoryboardBeat("hook", "h", 3.0),
                    StoryboardBeat("development", "d", 10.0, type = SlideType.MANIM, manimScene = "MoveSquare"),
                    StoryboardBeat("cta", "c", 7.0, asset = "logo.png"),
                ),
            ),
        )
        assertTrue(deck.contains("data-manim=\"MoveSquare\""))
        assertTrue(deck.contains("data-asset=\"logo.png\""))
    }

    @Test
    fun `deck escapes html in the beat intent`() {
        val deck = ViralDeckBuilder.build(
            storyboard(
                beats = listOf(
                    StoryboardBeat("hook", "a <b> & c", 3.0),
                    StoryboardBeat("cta", "d", 17.0),
                ),
            ),
        )
        assertTrue(deck.contains("a &lt;b&gt; &amp; c"))
        assertFalse(deck.contains("a <b> & c"))
    }

    @Test
    fun `deck injects the brand tokens when a visual identity is carried`() {
        val deck = ViralDeckBuilder.build(
            storyboard(
                visualIdentity = VisualIdentity(
                    brand = "cheroliv.com",
                    format = "9:16 (1080x1920)",
                    tokens = linkedMapOf("--accent-color" to "#0d6efd", "--footer-bg" to "#7952b3"),
                    typography = "Inter",
                ),
            ),
        )
        assertTrue(deck.contains("--accent-color: #0d6efd"))
        assertTrue(deck.contains("--footer-bg: #7952b3"))
        assertTrue(deck.contains("cheroliv.com"))
        assertTrue(deck.contains("--capsule-typography: Inter"))
    }

    @Test
    fun `deck is deterministic`() {
        val source = storyboard()
        assertEquals(ViralDeckBuilder.build(source), ViralDeckBuilder.build(source))
    }
}
