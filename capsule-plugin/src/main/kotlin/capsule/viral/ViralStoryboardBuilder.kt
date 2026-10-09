package capsule.viral

/**
 * Pure builder of the storyboard AsciiDoc document (CAP-VIRAL US-13).
 *
 * Renders a [ViralStoryboard] into a human-reviewable AsciiDoc document — the
 * artifact the pilot validates *before* any rendering (editorial gate). The
 * structure is deterministic and stable so the document can be diffed between
 * revisions.
 *
 * Pattern: [capsule.transcript.TranscriptBuilder] (pure object, deterministic
 * AsciiDoc, no I/O, no LLM).
 */
object ViralStoryboardBuilder {

    /**
     * Builds the storyboard AsciiDoc document.
     *
     * @param storyboard the storyboard to render.
     * @return a deterministic AsciiDoc document string.
     */
    fun build(storyboard: ViralStoryboard): String = buildString {
        append("= Storyboard — ")
        appendLine(storyboard.message)
        append(":platform: ")
        appendLine(storyboard.platform.name)
        append(":language: ")
        appendLine(storyboard.language)
        append(":target-duration: ")
        appendLine(storyboard.targetDurationSeconds.toString())
        appendLine()
        append("== Angle")
        appendLine()
        appendLine(storyboard.angle)
        appendLine()
        append("== Beats")
        appendLine()
        storyboard.beats.forEachIndexed { index, beat ->
            append("=== Beat ")
            append((index + 1).toString())
            append(" — ")
            appendLine(beat.role)
            appendLine()
            append("Intent: ")
            appendLine(beat.intent)
            appendLine()
            append("Duration: ")
            append(beat.durationSeconds.toString())
            appendLine("s")
            append("Render: ")
            appendLine(beat.type.name)
            if (beat.isManim) {
                append("Manim scene: ")
                appendLine(beat.manimScene ?: "")
            }
            if (beat.asset.isNotBlank()) {
                append("Asset: ")
                appendLine(beat.asset)
            }
            appendLine()
        }
    }.trimEnd() + "\n"
}
