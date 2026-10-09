package capsule.viral

import capsule.HtmlEscape

/**
 * Pure builder of the viral **deck** (CAP-SHORT US-1a).
 *
 * The storyboard ([ViralStoryboard]) is a *document* the pilot validates; it is
 * not a capturable deck. This object bridges the two: it derives a
 * self-contained portrait **1080×1920** HTML deck — one `<section>` per beat —
 * that the deterministic FFmpeg screenshot socle ([capsule.ScreenshotPlanner] /
 * `ScreenshotCaptureImpl`) can capture. The real-time Playwright recording is
 * excluded from the viral path (`ViralCaptureStrategy` forces `SCREENSHOT`).
 *
 * Each section carries, as `data-*` attributes, everything the render needs to
 * stay in sync: `data-role`, `data-duration` (seconds), `data-manim` and
 * `data-asset`. When a [VisualIdentity] is carried (CAP-CONTEXT US-1) its design
 * tokens are injected as CSS custom properties, so the deck is *dressed* by the
 * brand without touching the message.
 *
 * Pure — no I/O, no browser, no FFmpeg. Deterministic.
 */
object ViralDeckBuilder {

    /** Deck width (px) — 9:16 portrait (mirror of [ViralViewport.PORTRAIT_WIDTH]). */
    const val WIDTH: Int = ViralViewport.PORTRAIT_WIDTH

    /** Deck height (px) — 9:16 portrait. */
    const val HEIGHT: Int = ViralViewport.PORTRAIT_HEIGHT

    /** Typeface fallback when no visual identity is carried. */
    private const val DEFAULT_TYPOGRAPHY = "Sora, Inter, system-ui, sans-serif"

    /** Background fallback when no visual identity is carried. */
    private const val DEFAULT_BACKGROUND = "#0f1b33"

    /** Text colour for legibility on the default dark background. */
    private const val DEFAULT_FOREGROUND = "#fcf9f0"

    /**
     * Builds the capturable portrait deck for [storyboard].
     *
     * @param storyboard the validated storyboard to render as a deck.
     * @return a deterministic, self-contained HTML document string.
     */
    fun build(storyboard: ViralStoryboard): String {
        val identity = storyboard.visualIdentity
        val typography = identity?.typography?.takeIf { it.isNotBlank() } ?: DEFAULT_TYPOGRAPHY
        val background = identity?.background?.takeIf { it.isNotBlank() } ?: DEFAULT_BACKGROUND

        return buildString {
            appendLine("<!DOCTYPE html>")
            append("<html lang=\"").append(HtmlEscape.escape(storyboard.language)).appendLine("\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=$WIDTH, height=$HEIGHT\">")
            identity?.let { append("<meta name=\"brand\" content=\"").append(HtmlEscape.escape(it.brand)).appendLine("\">") }
            append("<title>").append(HtmlEscape.escape(storyboard.message)).appendLine("</title>")
            appendLine("<style>")
            appendLine("  :root {")
            appendLine("    --capsule-width: ${WIDTH}px;")
            appendLine("    --capsule-height: ${HEIGHT}px;")
            appendLine("    --capsule-typography: $typography;")
            appendLine("    --capsule-background: $background;")
            identity?.tokens?.forEach { (token, value) ->
                appendLine("    $token: $value;")
            }
            appendLine("  }")
            appendLine("  html, body { margin: 0; padding: 0; }")
            appendLine(
                "  section { box-sizing: border-box; width: var(--capsule-width); height: var(--capsule-height); " +
                    "display: flex; flex-direction: column; justify-content: center; gap: 48px; padding: 96px; " +
                    "background: var(--capsule-background); color: $DEFAULT_FOREGROUND; font-family: var(--capsule-typography); }",
            )
            appendLine("  section .role { font-size: 40px; letter-spacing: 4px; text-transform: uppercase; opacity: 0.7; }")
            appendLine("  section .intent { font-size: 96px; line-height: 1.15; margin: 0; font-weight: 600; }")
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            storyboard.beats.forEach { beat ->
                append("<section class=\"beat ").append(HtmlEscape.escape(beat.role.lowercase())).append("\"")
                append(" data-role=\"").append(HtmlEscape.escape(beat.role)).append("\"")
                append(" data-duration=\"").append(beat.durationSeconds.toString()).append("\"")
                if (beat.isManim) {
                    append(" data-manim=\"").append(HtmlEscape.escape(beat.manimScene ?: "")).append("\"")
                }
                if (beat.asset.isNotBlank()) {
                    append(" data-asset=\"").append(HtmlEscape.escape(beat.asset)).append("\"")
                }
                appendLine(">")
                append("  <div class=\"role\">").append(HtmlEscape.escape(beat.role)).appendLine("</div>")
                append("  <p class=\"intent\">").append(HtmlEscape.escape(beat.intent)).appendLine("</p>")
                appendLine("</section>")
            }
            appendLine("</body>")
            appendLine("</html>")
        }.trimEnd() + "\n"
    }
}
