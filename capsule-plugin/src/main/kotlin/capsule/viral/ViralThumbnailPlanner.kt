package capsule.viral

/**
 * Pure planner of the viral thumbnail / cover (CAP-VIRAL US-11).
 *
 * A YouTube Shorts / TikTok campaign needs a cover asset. The planner derives,
 * deterministically, the source slide and overlay spec of the thumbnail from the
 * storyboard's `hook` beat: the thumbnail carries the hook text over the title
 * slide.
 *
 * Output dimensions follow the platform convention: 1280×720 for YouTube, but the
 * vertical short-form cover uses the 9:16 portrait (1080×1920) so the same crop
 * rule as the video applies (US-2). This resolver only computes the *plan*; the
 * rendering (PNG via the existing `chapters/CardRenderer`) is wired by the task.
 *
 * Pure — no Gradle, no I/O.
 */
object ViralThumbnailPlanner {

    /** YouTube thumbnail landscape width (px). */
    const val LANDSCAPE_WIDTH: Int = 1280

    /** YouTube thumbnail landscape height (px). */
    const val LANDSCAPE_HEIGHT: Int = 720

    /**
     * Plans the thumbnail for [storyboard].
     *
     * @param storyboard the validated storyboard.
     * @return the thumbnail plan (overlay text + dimensions + source beat role).
     */
    fun plan(storyboard: ViralStoryboard): ThumbnailPlan {
        val overlay = storyboard.hookBeat?.intent?.trim().orEmpty().ifBlank { storyboard.message }
        val (width, height) = ViralViewport.resolve(
            viralEnabled = true,
            defaultWidth = LANDSCAPE_WIDTH,
            defaultHeight = LANDSCAPE_HEIGHT,
        )
        return ThumbnailPlan(
            overlayText = overlay,
            language = storyboard.language,
            platform = storyboard.platform,
            width = width,
            height = height,
        )
    }
}

/**
 * Immutable plan of a viral thumbnail / cover (CAP-VIRAL US-11).
 *
 * @property overlayText text burned on the cover (the hook).
 * @property language    target ISO language code.
 * @property platform    target platform preset.
 * @property width       cover width (px).
 * @property height      cover height (px).
 */
data class ThumbnailPlan(
    val overlayText: String,
    val language: String,
    val platform: ViralPlatform,
    val width: Int,
    val height: Int,
) {
    init {
        require(overlayText.isNotBlank()) { "ThumbnailPlan.overlayText must not be blank" }
        require(language.isNotBlank()) { "ThumbnailPlan.language must not be blank" }
        require(width > 0 && height > 0) { "ThumbnailPlan dimensions must be positive" }
    }

    /** `true` when the plan is the vertical 9:16 short-form cover. */
    val isPortrait: Boolean get() = ViralViewport.isPortrait(width, height)
}
