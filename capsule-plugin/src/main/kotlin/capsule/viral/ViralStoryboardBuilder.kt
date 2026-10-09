package capsule.viral

/**
 * Pure builder of the storyboard AsciiDoc document (CAP-VIRAL US-13, extended
 * by CAP-CONTEXT US-2).
 *
 * Renders a [ViralStoryboard] into a human-reviewable AsciiDoc document — the
 * artifact the pilot validates *before* any rendering (editorial gate). The
 * structure is deterministic and stable so the document can be diffed between
 * revisions.
 *
 * CAP-CONTEXT adds the three context axes of `CAPSULE_VIDEO.adoc` — QQOQCP,
 * visual identity (design system) and narrative thread — plus the per-beat
 * entering transition. They are rendered only when carried (economy of ink):
 * a context-less storyboard produces the exact CAP-VIRAL document.
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
        storyboard.qqoqcp?.let { appendQqoqcp(it) }
        storyboard.visualIdentity?.let { appendVisualIdentity(it) }
        storyboard.narrativeThread?.let { appendNarrativeThread(it) }
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
            if (beat.enteringTransition.isNotBlank()) {
                append("Transition: ")
                appendLine(beat.enteringTransition)
            }
            appendLine()
        }
    }.trimEnd() + "\n"

    /** Renders the `== QQOQCP` section (six labelled answers). */
    private fun StringBuilder.appendQqoqcp(q: Qqoqcp) {
        appendLine("== QQOQCP")
        appendLine()
        appendLine("Qui (public): ${q.audience}")
        appendLine("Quoi (sujet): ${q.subject}")
        appendLine("Où (contexte): ${q.context}")
        appendLine("Quand (moment): ${q.moment}")
        appendLine("Comment (modalité): ${q.modality}")
        appendLine("Pourquoi (but): ${q.purpose}")
        appendLine()
    }

    /** Renders the `== Identité visuelle (design system)` section. */
    private fun StringBuilder.appendVisualIdentity(v: VisualIdentity) {
        appendLine("== Identité visuelle (design system)")
        appendLine()
        appendLine("Brand: ${v.brand}")
        appendLine("Format: ${v.format}")
        if (v.typography.isNotBlank()) appendLine("Typography: ${v.typography}")
        if (v.logo.isNotBlank()) appendLine("Logo: ${v.logo}")
        if (v.background.isNotBlank()) appendLine("Background: ${v.background}")
        v.tokens.forEach { (token, value) -> appendLine("Token: $token = $value") }
        appendLine()
    }

    /** Renders the `== Fil conducteur narratif` section. */
    private fun StringBuilder.appendNarrativeThread(n: NarrativeThread) {
        appendLine("== Fil conducteur narratif")
        appendLine()
        appendLine("Thesis: ${n.thesis}")
        appendLine("Recurring motif: ${n.recurringMotif}")
        appendLine("Arc: ${n.arc}")
        if (n.register.isNotBlank()) appendLine("Register: ${n.register}")
        if (n.voice.isNotBlank()) appendLine("Voice: ${n.voice}")
        appendLine()
    }
}
