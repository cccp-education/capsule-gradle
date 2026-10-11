package capsule.viral

import capsule.HtmlEscape

/**
 * Pure builder of the viral **thumbnail / cover** (CAP-VIRAL-E2E US-2).
 *
 * The [ViralThumbnailPlanner] computes *what* the cover must carry (the hook
 * overlay text, the platform, the portrait dimensions); this object renders it as
 * a self-contained portrait HTML document that the screenshot socle
 * ([capsule.ScreenshotPlanner] / `ScreenshotCaptureImpl`) captures to the PNG
 * handed to the distribution layer as `CampaignBundle.thumbnailFile`.
 *
 * Distinct from [ViralDeckBuilder]: the deck is the multi-beat video source, the
 * cover is the single **hook** frame — bigger type, no per-beat chrome.
 *
 * Pure — no I/O, no browser, no FFmpeg. Deterministic.
 */
object ViralThumbnailBuilder {

    /** Suffix of the per-variant cover file (`<variant.id>-thumb`). */
    const val THUMBNAIL_SUFFIX: String = "-thumb"

    /** Container of the per-variant cover. */
    const val THUMBNAIL_EXTENSION: String = "png"

    /** Names the per-variant cover file, e.g. `fr-tiktok-30-thumb.png`. */
    fun fileNameFor(variantId: String): String = "$variantId$THUMBNAIL_SUFFIX.$THUMBNAIL_EXTENSION"

    /** Cover typeface fallback (no visual identity is carried by the plan). */
    private const val DEFAULT_TYPOGRAPHY = "Sora, Inter, system-ui, sans-serif"

    /** Cover background fallback (platform-agnostic dark slate). */
    private const val DEFAULT_BACKGROUND = "#0f1b33"

    /** Text colour for legibility on the default dark background. */
    private const val DEFAULT_FOREGROUND = "#fcf9f0"

    /**
     * Builds the capturable portrait cover for [plan].
     *
     * @param plan the thumbnail plan ([ViralThumbnailPlanner]); carries the hook
     *             overlay text, the language, the platform and the dimensions.
     * @return a deterministic, self-contained HTML document string.
     */
    fun build(plan: ThumbnailPlan): String {
        val width = plan.width
        val height = plan.height
        return buildString {
            appendLine("<!DOCTYPE html>")
            append("<html lang=\"").append(HtmlEscape.escape(plan.language)).appendLine("\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=").append(width)
                .append(", height=").append(height).appendLine("\">")
            append("<title>").append(HtmlEscape.escape(plan.overlayText)).appendLine("</title>")
            appendLine("<style>")
            appendLine("  html, body { margin: 0; padding: 0; }")
            appendLine(
                "  section.cover { box-sizing: border-box; width: ${width}px; height: ${height}px; " +
                    "display: flex; flex-direction: column; align-items: center; justify-content: center; " +
                    "gap: 40px; padding: 120px; background: $DEFAULT_BACKGROUND; " +
                    "color: $DEFAULT_FOREGROUND; font-family: $DEFAULT_TYPOGRAPHY; text-align: center; }",
            )
            appendLine("  section.cover .platform { font-size: 36px; letter-spacing: 6px; text-transform: uppercase; opacity: 0.7; }")
            appendLine("  section.cover .hook { font-size: 120px; line-height: 1.1; margin: 0; font-weight: 700; }")
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<section class=\"cover\">")
            append("  <div class=\"platform\">").append(HtmlEscape.escape(plan.platform.name)).appendLine("</div>")
            append("  <p class=\"hook\">").append(HtmlEscape.escape(plan.overlayText)).appendLine("</p>")
            appendLine("</section>")
            appendLine("</body>")
            appendLine("</html>")
        }.trimEnd() + "\n"
    }
}
