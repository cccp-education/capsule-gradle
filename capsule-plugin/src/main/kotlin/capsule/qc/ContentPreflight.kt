package capsule.qc

import capsule.feed.SlideSegment

/** Severity of a pre-flight finding. */
enum class Severity { ERROR, WARNING }

/**
 * One pre-flight finding on a slide segment (PRT-05).
 *
 * @param severity  [Severity.ERROR] fails the pre-flight; [Severity.WARNING] is
 *        advisory (fails only in strict mode).
 * @param slideIndex the 1-based slide index the finding concerns.
 * @param code       a stable machine-readable code (`long-note`, `duplicate-note`,
 *        `forbidden-form`).
 * @param message    a human-readable explanation.
 */
data class PreflightFinding(
    val severity: Severity,
    val slideIndex: Int,
    val code: String,
    val message: String,
)

/**
 * A form forbidden in a narrated note — a `pattern` and why it must not appear.
 * Ported from `prepa_electricite_complet/qc_script.py` (`INTERDIT`), where a
 * curated list of phrasings is rejected before any metered TTS call.
 */
data class ForbiddenForm(val pattern: Regex, val reason: String)

/**
 * Rules for [ContentPreflight].
 *
 * @param maxNoteChars a note longer than this earns a warning.
 * @param forbidden    forms that must not appear in a narrated note.
 */
data class PreflightRules(
    val maxNoteChars: Int = DEFAULT_MAX_NOTE_CHARS,
    val forbidden: List<ForbiddenForm> = DEFAULT_FORBIDDEN,
) {
    companion object {
        const val DEFAULT_MAX_NOTE_CHARS: Int = 500

        /** Generic default: markdown emphasis left in the text that is spoken. */
        val DEFAULT_FORBIDDEN: List<ForbiddenForm> = listOf(
            ForbiddenForm(Regex("""\*\*"""), "markdown emphasis in the narrated text"),
        )
    }
}

/**
 * Structural pre-flight of capsule scripts, run BEFORE any metered TTS synthesis
 * (PRT-05).
 *
 * Ported from `prepa_electricite_complet/qc_script.py`, which refuses an
 * incoherent scene (and a curated list of mispronounced phrasings) before any
 * paid TTS call. Capsule's [SlideSegment] already guarantees a non-blank title
 * and note, so the checks here are: an over-long note, a note duplicated across
 * slides, and any forbidden form.
 *
 * Pure — no I/O, no Gradle.
 */
object ContentPreflight {

    /**
     * Checks [slides] against [rules] and returns every finding, in slide order.
     * An empty list means the scripts are clean.
     */
    fun check(slides: List<SlideSegment>, rules: PreflightRules = PreflightRules()): List<PreflightFinding> {
        val findings = mutableListOf<PreflightFinding>()
        val firstSeen = LinkedHashMap<String, Int>()

        for (slide in slides) {
            val note = slide.speakerNote

            if (note.length > rules.maxNoteChars) {
                findings.add(
                    PreflightFinding(
                        Severity.WARNING,
                        slide.index,
                        "long-note",
                        "speaker note is ${note.length} chars (> ${rules.maxNoteChars})",
                    )
                )
            }

            val normalized = note.trim().lowercase()
            val first = firstSeen[normalized]
            if (first == null) {
                firstSeen[normalized] = slide.index
            } else {
                findings.add(
                    PreflightFinding(
                        Severity.WARNING,
                        slide.index,
                        "duplicate-note",
                        "speaker note duplicates slide $first",
                    )
                )
            }

            for (form in rules.forbidden) {
                val match = form.pattern.find(note)
                if (match != null) {
                    findings.add(
                        PreflightFinding(
                            Severity.ERROR,
                            slide.index,
                            "forbidden-form",
                            "${form.reason} (matched '${match.value.take(40)}')",
                        )
                    )
                }
            }
        }

        return findings
    }
}
