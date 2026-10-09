package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral thumbnail (CAP-VIRAL US-11) and karaoke
 * subtitles (CAP-VIRAL US-12).
 */
class ViralAssetsTest {

    private fun storyboard() = ViralStoryboard(
        message = "Devenir formateur",
        angle = "Le métier en 30 s",
        beats = listOf(
            StoryboardBeat("hook", "Et si 30 secondes suffisaient ?", 3.0),
            StoryboardBeat("development", "Les 4 activités types", 10.0),
            StoryboardBeat("development", "Le rôle du formateur", 10.0),
            StoryboardBeat("cta", "Découvrez FPA", 7.0),
        ),
        platform = ViralPlatform.YOUTUBE_SHORT,
        language = "fr",
        targetDurationSeconds = 30,
    )

    // ─── US-11 thumbnail ──────────────────────────────────────────────

    @Test
    fun `thumbnail plan uses the hook intent and the portrait viewport`() {
        val plan = ViralThumbnailPlanner.plan(storyboard())
        assertEquals("Et si 30 secondes suffisaient ?", plan.overlayText)
        assertTrue(plan.isPortrait)
        assertEquals(1080, plan.width)
        assertEquals(1920, plan.height)
    }

    @Test
    fun `thumbnail plan falls back to the message when no hook beat`() {
        val sb = storyboard().copy(
            beats = listOf(
                StoryboardBeat("development", "Dev", 25.0),
                StoryboardBeat("cta", "CTA", 5.0),
            ),
        )
        assertEquals("Devenir formateur", ViralThumbnailPlanner.plan(sb).overlayText)
    }

    @Test
    fun `thumbnail plan rejects blank overlay`() {
        assertTrue(
            runCatching { ThumbnailPlan("", "fr", ViralPlatform.TIKTOK, 1080, 1920) }.isFailure,
        )
    }

    // ─── US-12 karaoke subtitles ──────────────────────────────────────

    @Test
    fun `karaoke text emits one k override per word`() {
        val words = listOf(
            KaraokeWord("Et", 0.0, 0.2),
            KaraokeWord("si", 0.2, 0.4),
            KaraokeWord("30", 0.4, 0.8),
        )
        val text = KaraokeSubtitleBuilder.buildKaraokeText(words)
        assertEquals("{\\k20}Et {\\k20}si {\\k40}30", text)
    }

    @Test
    fun `karaoke text is blank for no words`() {
        assertEquals("", KaraokeSubtitleBuilder.buildKaraokeText(emptyList()))
    }

    @Test
    fun `ass timestamp formats hours minutes seconds centiseconds`() {
        assertEquals("0:00:00.00", KaraokeSubtitleBuilder.formatAssTimestamp(0.0))
        assertEquals("0:00:01.50", KaraokeSubtitleBuilder.formatAssTimestamp(1.5))
        assertEquals("0:01:05.25", KaraokeSubtitleBuilder.formatAssTimestamp(65.25))
        assertEquals("1:00:00.00", KaraokeSubtitleBuilder.formatAssTimestamp(3600.0))
    }

    @Test
    fun `KaraokeWord rejects blank text and negative times`() {
        assertTrue(runCatching { KaraokeWord(" ", 0.0, 1.0) }.isFailure)
        assertTrue(runCatching { KaraokeWord("x", -1.0, 1.0) }.isFailure)
        assertTrue(runCatching { KaraokeWord("x", 2.0, 1.0) }.isFailure)
    }

    @Test
    fun `karaoke duration rounds up to at least one centisecond`() {
        val text = KaraokeSubtitleBuilder.buildKaraokeText(listOf(KaraokeWord("x", 0.0, 0.001)))
        assertTrue(text.contains("{\\k1}"), "sub-centisecond words should still advance by 1 centisecond")
        assertFalse(text.contains("{\\k0}"))
    }
}
