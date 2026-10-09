package capsule.viral

import capsule.CaptureStrategy

/**
 * Pure rule resolving the capture strategy for a viral campaign (CAP-VIRAL US-2).
 *
 * Pilot arbitration (S-138): a viral campaign is assembled with the
 * **deterministic FFmpeg socle** — one PNG per slide, encoded to the exact
 * duration, then `concat` demuxer (`-c copy`, no re-encoding). The real-time
 * Playwright recording is **excluded** from the viral path: its timing is
 * non-deterministic and hostile to a reproducible batch.
 *
 * `REMOTION` is preserved when explicitly selected: it is the *content
 * enrichment* tier (motion, transitions, Manim), composed **over** the FFmpeg
 * socle — not a rival of it. Only plain `PLAYWRIGHT` recording is replaced.
 *
 * Pure — no Gradle, no I/O.
 */
object ViralCaptureStrategy {

    /**
     * Resolves the effective capture strategy.
     *
     * @param configured  the strategy from the configuration (4 sources).
     * @param viralEnabled whether the `viral` section is enabled.
     * @return [CaptureStrategy.SCREENSHOT] when viral is enabled and the
     *         configured strategy is the excluded [CaptureStrategy.PLAYWRIGHT];
     *         otherwise the configured strategy unchanged (backward compat).
     */
    fun resolve(configured: CaptureStrategy, viralEnabled: Boolean): CaptureStrategy =
        if (viralEnabled && configured == CaptureStrategy.PLAYWRIGHT) {
            CaptureStrategy.SCREENSHOT
        } else {
            configured
        }
}
