package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD Baby Step — ManimSlideReplacer unit tests.
 *
 * ManimSlideReplacer replaces a slide's HTML <section> element
 * with a <section> containing a <video> tag pointing to the Manim MP4.
 *
 * Design:
 * - Interface ManimSlideReplacer with replaceSlideAt(deckHtml, slideIndex, videoPath) + NoOp + Impl
 * - ManimSlideReplacerImpl parses the deck HTML and swaps the <section> at slideIndex
 *   with a video-embed section pointing to the muxed MP4
 * - NoOpManimSlideReplacer returns the deck HTML unchanged (for testing)
 */
class ManimSlideReplacerTest {

    private val sampleDeck = """
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Intro</h2><p>Welcome</p></section>
    <section data-capsule-slide="2"><h2>Anim</h2><p>Animation here</p></section>
    <section data-capsule-slide="3"><h2>End</h2><p>Goodbye</p></section>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
</body></html>
    """.trimIndent()

    // ─── NoOpManimSlideReplacer ──────────────────────────────────

    @Test
    fun `noop replacer returns deck unchanged`() {
        val replacer = NoOpManimSlideReplacer()
        val result = replacer.replaceSlideAt(sampleDeck, 1, "video/Scene1-muxed.mp4")
        assertEquals(sampleDeck, result, "NoOp replacer should return deck unchanged")
    }

    @Test
    fun `noop replacer is available and named noop-replacer`() {
        val replacer = NoOpManimSlideReplacer()
        assertTrue(replacer.isAvailable())
        assertEquals("noop-replacer", replacer.name())
    }

    // ─── ManimSlideReplacerImpl — basic replacement ──────────────

    @Test
    fun `replacer replaces slide at index with video embed`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 1, "video/Scene1-muxed.mp4")

        // The slide at index 1 should now contain a <video> tag
        assertTrue(result.contains("<video"), "Replaced slide should contain <video> tag")
        assertTrue(result.contains("Scene1-muxed.mp4"), "Video src should point to the muxed MP4")
        // Other slides should remain unchanged
        assertTrue(result.contains("Intro"), "Slide 0 should still contain 'Intro'")
        assertTrue(result.contains("Goodbye"), "Slide 2 should still contain 'Goodbye'")
        // The original "Anim" content is replaced by the video embed
        assertTrue(!result.contains("Animation here"), "Slide 1 content should be replaced")
    }

    @Test
    fun `replacer replaces first slide`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 0, "video/IntroAnim-muxed.mp4")

        assertTrue(result.contains("<video"), "Slide 0 should contain <video> tag")
        assertTrue(result.contains("IntroAnim-muxed.mp4"), "Should reference IntroAnim video")
        assertTrue(result.contains("Anim"), "Slide 1 should still contain 'Anim'")
        assertTrue(result.contains("Goodbye"), "Slide 2 should still contain 'Goodbye'")
    }

    @Test
    fun `replacer replaces last slide`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 2, "video/EndAnim-muxed.mp4")

        assertTrue(result.contains("<video"), "Slide 2 should contain <video> tag")
        assertTrue(result.contains("EndAnim-muxed.mp4"), "Should reference EndAnim video")
        assertTrue(result.contains("Intro"), "Slide 0 should still contain 'Intro'")
        assertTrue(result.contains("Anim"), "Slide 1 should still contain 'Anim'")
    }

    @Test
    fun `replacer preserves video attributes autoplay loop muted`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 1, "video/Scene1-muxed.mp4")

        assertTrue(result.contains("autoplay"), "Video should have autoplay attribute")
        assertTrue(result.contains("loop"), "Video should have loop attribute")
        assertTrue(result.contains("muted"), "Video should have muted attribute (for autoplay)")
    }

    @Test
    fun `replacer preserves surrounding HTML structure`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 1, "video/Scene1-muxed.mp4")

        assertTrue(result.contains("class=\"reveal\""), "Should preserve reveal container")
        assertTrue(result.contains("class=\"slides\""), "Should preserve slides container")
        assertTrue(result.contains("reveal.js"), "Should preserve reveal.js script")
        assertTrue(result.contains("</html>"), "Should preserve HTML structure")
    }

    // ─── Edge cases ──────────────────────────────────────────────

    @Test
    fun `replacer returns original deck when index out of bounds`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 99, "video/missing.mp4")
        assertEquals(sampleDeck, result, "Should return unchanged deck for out-of-bounds index")
    }

    @Test
    fun `replacer returns original deck when index is negative`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, -1, "video/missing.mp4")
        assertEquals(sampleDeck, result, "Should return unchanged deck for negative index")
    }

    @Test
    fun `replacer handles deck with only one slide`() {
        val singleSlideDeck = """
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Only Slide</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent()

        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(singleSlideDeck, 0, "video/only.mp4")

        assertTrue(result.contains("<video"), "Should replace the single slide with video")
        assertTrue(result.contains("only.mp4"), "Should reference the video file")
    }

    @Test
    fun `replacer handles nested sections preserving vertical stack`() {
        val nestedDeck = """
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1">
      <section><h2>Sub A</h2></section>
      <section><h2>Sub B</h2></section>
    </section>
    <section data-capsule-slide="2"><h2>Other</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent()

        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(nestedDeck, 0, "video/nested.mp4")

        // The nested section (with Sub A and Sub B) should be replaced by the video embed
        assertTrue(result.contains("<video"), "Should contain video tag for nested section slide")
        assertTrue(result.contains("nested.mp4"), "Should reference nested video")
        // Slide 2 should still be there
        assertTrue(result.contains("Other"), "Slide 2 should still be present")
    }

    @Test
    fun `replacer replaces multiple slides sequentially`() {
        val replacer = ManimSlideReplacerImpl()

        // First replacement
        val result1 = replacer.replaceSlideAt(sampleDeck, 0, "video/Scene1-muxed.mp4")
        assertTrue(result1.contains("Scene1-muxed.mp4"))
        assertTrue(result1.contains("Anim"))
        assertTrue(result1.contains("Goodbye"))

        // Second replacement on the result
        val result2 = replacer.replaceSlideAt(result1, 1, "video/Scene2-muxed.mp4")
        assertTrue(result2.contains("Scene1-muxed.mp4"), "First replacement should persist")
        assertTrue(result2.contains("Scene2-muxed.mp4"), "Second replacement should be added")
        assertTrue(result2.contains("Goodbye"), "Unaffected slide should remain")
    }

    @Test
    fun `replacer video section preserves data-capsule-slide attribute`() {
        val replacer = ManimSlideReplacerImpl()
        val result = replacer.replaceSlideAt(sampleDeck, 1, "video/Scene1-muxed.mp4")

        // The replacement <section> should preserve the data-capsule-slide attribute
        assertTrue(result.contains("data-capsule-slide"), "Video section should have data-capsule-slide")
    }

    // ─── Factory ─────────────────────────────────────────────────

    @Test
    fun `resolveManimSlideReplacer returns impl`() {
        val replacer = CapsuleManager.resolveManimSlideReplacer()
        assertNotNull(replacer)
        assertTrue(replacer.name().isNotBlank())
    }
}