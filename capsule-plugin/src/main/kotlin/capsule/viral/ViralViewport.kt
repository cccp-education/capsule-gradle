package capsule.viral

/**
 * Pure resolver of the capture viewport for a viral campaign (CAP-VIRAL US-2).
 *
 * The short-form viral pipeline reuses the **deterministic FFmpeg socle**
 * (`ScreenshotPlanner` — one PNG per slide → exact duration → concat demuxer),
 * but captures at a **9:16 portrait** viewport instead of the pedagogical
 * 16:9 landscape. This object computes the viewport dimension to hand to the
 * capture engine; it is pure (no I/O, no Gradle) so the rule is unit-testable.
 *
 * The crop `deployCapsule` (16:9 → 9:16) remains the *fallback* for decks
 * already rendered landscape; native portrait is the nominal viral path.
 *
 * Platform presets all render 1080×1920 (the platform identity lives in the
 * metadata, not the encoding).
 */
object ViralViewport {

    /** Portrait width (px) — 9:16. */
    const val PORTRAIT_WIDTH: Int = 1080

    /** Portrait height (px) — 9:16. */
    const val PORTRAIT_HEIGHT: Int = 1920

    /**
     * Resolves the capture viewport.
     *
     * @param viralEnabled when `true`, the portrait 1080×1920 viewport is used.
     * @param defaultWidth  the landscape default (16:9) width.
     * @param defaultHeight the landscape default (16:9) height.
     * @return the (width, height) pair to hand to the capture engine.
     */
    fun resolve(
        viralEnabled: Boolean,
        defaultWidth: Int,
        defaultHeight: Int,
    ): Pair<Int, Int> =
        if (viralEnabled) PORTRAIT_WIDTH to PORTRAIT_HEIGHT else defaultWidth to defaultHeight

    /**
     * `true` when the (width, height) pair is the portrait 9:16 short-form
     * viewport.
     */
    fun isPortrait(width: Int, height: Int): Boolean =
        width == PORTRAIT_WIDTH && height == PORTRAIT_HEIGHT
}
