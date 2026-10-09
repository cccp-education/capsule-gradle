package capsule.viral

import capsule.CaptureStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guard for the viral capture-strategy rule (CAP-VIRAL US-2 — FFmpeg socle).
 *
 * The pilot arbitrated that a viral campaign uses the **deterministic FFmpeg
 * socle** (one PNG per slide → exact duration → concat demuxer), **never** the
 * real-time Playwright recording (non-deterministic timing, batch-hostile).
 * `Remotion` stays available as the *content-enrichment* tier — it is not
 * antagonist to FFmpeg, it composes over it — but plain `PLAYWRIGHT` recording
 * is excluded from the viral path.
 *
 * This pure rule encodes that decision so the wiring cannot silently drift back
 * to Playwright when the viral section is enabled.
 */
class ViralCaptureStrategyTest {

    @Test
    fun `viral enabled replaces playwright recording with the ffmpeg screenshot socle`() {
        assertEquals(
            CaptureStrategy.SCREENSHOT,
            ViralCaptureStrategy.resolve(CaptureStrategy.PLAYWRIGHT, viralEnabled = true),
        )
    }

    @Test
    fun `viral enabled keeps the ffmpeg screenshot socle`() {
        assertEquals(
            CaptureStrategy.SCREENSHOT,
            ViralCaptureStrategy.resolve(CaptureStrategy.SCREENSHOT, viralEnabled = true),
        )
    }

    @Test
    fun `viral enabled keeps the remotion enrichment tier`() {
        assertEquals(
            CaptureStrategy.REMOTION,
            ViralCaptureStrategy.resolve(CaptureStrategy.REMOTION, viralEnabled = true),
        )
    }

    @Test
    fun `viral disabled preserves the configured strategy unchanged`() {
        for (strategy in CaptureStrategy.entries) {
            assertEquals(strategy, ViralCaptureStrategy.resolve(strategy, viralEnabled = false))
        }
    }
}
