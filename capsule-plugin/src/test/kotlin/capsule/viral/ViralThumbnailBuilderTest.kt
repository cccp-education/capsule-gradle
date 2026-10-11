package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral cover builder (CAP-VIRAL-E2E US-2): the pure
 * portrait HTML the screenshot socle captures into the campaign thumbnail.
 */
class ViralThumbnailBuilderTest {

    private fun plan(
        overlay: String = "Et si 30 secondes suffisaient ?",
        language: String = "fr",
        platform: ViralPlatform = ViralPlatform.TIKTOK,
    ) = ThumbnailPlan(
        overlayText = overlay,
        language = language,
        platform = platform,
        width = ViralViewport.PORTRAIT_WIDTH,
        height = ViralViewport.PORTRAIT_HEIGHT,
    )

    @Test
    fun `cover carries the overlay text on the portrait viewport`() {
        val html = ViralThumbnailBuilder.build(
            ThumbnailPlan("Le hook", "fr", ViralPlatform.TIKTOK, 1080, 1920),
        )
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("width=1080, height=1920"), "Expected the portrait viewport, got: $html")
        assertTrue(html.contains("<p class=\"hook\">Le hook</p>"), "Expected the hook overlay, got: $html")
        assertTrue(html.contains("<section class=\"cover\">"), "Expected a single cover section")
        assertEquals(1, Regex("<section").findAll(html).count(), "The cover is a single section")
    }

    @Test
    fun `cover labels the platform`() {
        val html = ViralThumbnailBuilder.build(plan(platform = ViralPlatform.YOUTUBE_SHORT))
        assertTrue(html.contains("YOUTUBE_SHORT"), "Expected the platform label, got: $html")
    }

    @Test
    fun `cover escapes html in the overlay text`() {
        val html = ViralThumbnailBuilder.build(plan(overlay = "<script>alert('x')</script>"))
        assertTrue(html.contains("&lt;script&gt;"), "Expected escaped overlay, got: $html")
        assertTrue(!html.contains("<script>"), "Raw script must never appear")
    }

    @Test
    fun `cover declares its language`() {
        val html = ViralThumbnailBuilder.build(plan(language = "ar"))
        assertTrue(html.contains("<html lang=\"ar\">"), "Expected the document language, got: $html")
    }

    @Test
    fun `cover is deterministic`() {
        assertEquals(ViralThumbnailBuilder.build(plan()), ViralThumbnailBuilder.build(plan()))
    }

    @Test
    fun `file name follows the variant thumb convention`() {
        assertEquals("fr-tiktok-30-thumb.png", ViralThumbnailBuilder.fileNameFor("fr-tiktok-30"))
    }
}
