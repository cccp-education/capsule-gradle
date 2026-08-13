package capsule.transcript

import capsule.feed.SlideType

/**
 * Pure, deterministic builder of a transcript AsciiDoc article from a
 * [TranscriptPlan] (CAP-TRANSCRIPT US-2).
 *
 * Assembles a level-0 title (`= <deckName>`), a `:language:` attribute, an
 * intro boilerplate, one `== <title>` section per HTML segment with the
 * speaker note as body, and an outro boilerplate. MANIM segments are
 * skipped — animation-only slides have no place in a text article.
 *
 * Zero I/O, zero LLM — fully testable. Pattern: [capsule.feed.CapsuleScriptWriter]
 * (pure object `write(script): String`).
 */
object TranscriptBuilder {

    /** Intro boilerplate appended after the title + language attribute. */
    const val INTRO: String =
        "This article is the transcript of the training capsule."

    /** Outro boilerplate appended after the last section. */
    const val OUTRO: String =
        "End of the capsule transcript."

    /**
     * Build the transcript AsciiDoc article from [plan].
     *
     * @param plan the transcript plan (deck name, segments, language).
     * @return a deterministic AsciiDoc article string.
     */
    fun build(plan: TranscriptPlan): String = buildString {
        append("= ")
        appendLine(plan.deckName)
        append(":language: ")
        appendLine(plan.language)
        appendLine()
        appendLine(INTRO)
        appendLine()
        for (segment in plan.segments) {
            if (segment.type == SlideType.MANIM) continue
            append("== ")
            appendLine(segment.title)
            appendLine()
            appendLine(segment.speakerNote.trim())
            appendLine()
        }
        appendLine(OUTRO)
    }.trimEnd() + "\n"
}