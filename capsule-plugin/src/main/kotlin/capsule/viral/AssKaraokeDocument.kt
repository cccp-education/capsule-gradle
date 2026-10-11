package capsule.viral

/**
 * Pure builder of a complete **ASS** karaoke caption document (CAP-VIRAL-E2E US-3).
 *
 * [KaraokeSubtitleBuilder] produces the per-line `\k` dialogue text and the ASS
 * timestamps; this object wraps them into the full `.ass` file the distribution
 * layer consumes (and FFmpeg's `subtitles=` filter burns in): script info,
 * one `V4+` style tuned for the portrait short-form, and one `Dialogue` per
 * [KaraokeLine].
 *
 * The style is deliberately large and bottom-centred — readable at phone size,
 * the retention-critical caption style for TikTok/Shorts/Reels.
 *
 * Pure — no I/O. Deterministic.
 */
object AssKaraokeDocument {

    /** Render width declared to the ASS renderer (portrait). */
    const val RESOLUTION_WIDTH: Int = ViralViewport.PORTRAIT_WIDTH

    /** Render height declared to the ASS renderer (portrait). */
    const val RESOLUTION_HEIGHT: Int = ViralViewport.PORTRAIT_HEIGHT

    /** Name of the single style carried by the document. */
    const val STYLE_NAME: String = "Viral"

    /**
     * Builds the ASS karaoke document for [lines].
     *
     * @param lines the karaoke timeline ([KaraokeWordPlanner]); empty lines are skipped.
     * @return a deterministic, self-contained ASS v4.00+ document string.
     */
    fun build(lines: List<KaraokeLine>): String {
        val dialogues = lines.filter { it.words.isNotEmpty() }.map { line ->
            val text = KaraokeSubtitleBuilder.buildKaraokeText(line.words)
            "Dialogue: 0,${KaraokeSubtitleBuilder.formatAssTimestamp(line.startTime)}," +
                "${KaraokeSubtitleBuilder.formatAssTimestamp(line.endTime)},$STYLE_NAME,,0,0,0,,$text"
        }
        return buildString {
            appendLine("[Script Info]")
            appendLine("ScriptType: v4.00+")
            appendLine("PlayResX: $RESOLUTION_WIDTH")
            appendLine("PlayResY: $RESOLUTION_HEIGHT")
            appendLine("WrapStyle: 0")
            appendLine()
            appendLine("[V4+ Styles]")
            appendLine(
                "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, " +
                    "BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, " +
                    "BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding",
            )
            appendLine(
                "Style: $STYLE_NAME,Sora,96,&H00FFFFFF,&H0000FFFF,&H00000000,&H00000000," +
                    "-1,0,0,0,100,100,0,0,1,4,2,2,60,60,240,1",
            )
            appendLine()
            appendLine("[Events]")
            appendLine("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text")
            dialogues.forEach { appendLine(it) }
        }.trimEnd() + "\n"
    }
}
