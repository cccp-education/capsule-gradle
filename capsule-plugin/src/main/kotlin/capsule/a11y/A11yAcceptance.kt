package capsule.a11y

/**
 * The accessibility facts of a produced capsule (CAP-AUDIODESC US-4).
 *
 * @property subtitlesPresent the video carries subtitles (VTT/SRT or burn-in).
 * @property transcriptPresent a written transcript article was produced.
 * @property audioDescriptionRequested the build asked for an audio description.
 * @property audioDescriptionPresent the description was actually mixed.
 */
data class A11yInputs(
    val subtitlesPresent: Boolean,
    val transcriptPresent: Boolean,
    val audioDescriptionRequested: Boolean,
    val audioDescriptionPresent: Boolean,
)

/**
 * Verdict of the accessibility acceptance gate (CAP-AUDIODESC US-4).
 *
 * [limitations] is **always declared** — a deliverable never claims more than it
 * proves (the EPIC rule: no accessibility promise without measurement).
 */
sealed interface A11yVerdict {
    /** The declared limitations, always explicit. */
    val limitations: List<String>

    /** Every requested channel is present. */
    data class Accepted(
        override val limitations: List<String>,
    ) : A11yVerdict

    /** At least one expected channel is missing; [reasons] are actionable. */
    data class Degraded(
        val reasons: List<String>,
        override val limitations: List<String>,
    ) : A11yVerdict
}

/**
 * Pure acceptance gate (CAP-AUDIODESC US-4).
 *
 * Floor: subtitles and a transcript are always expected; the audio description
 * is expected only when it was requested. Missing channels **degrade** the
 * verdict (they never throw) and every verdict declares the same explicit
 * limitation: extended audio description is out of scope in v1.
 */
object A11yAcceptance {
    /** Explicit v1 limitation, declared on every verdict. */
    const val EXTENDED_AUDIO_DESCRIPTION_LIMIT: String = "extended audio description is not supported in v1"

    fun evaluate(inputs: A11yInputs): A11yVerdict {
        val reasons =
            buildList {
                if (!inputs.subtitlesPresent) add("subtitles are missing")
                if (!inputs.transcriptPresent) add("the transcription is missing")
                if (inputs.audioDescriptionRequested && !inputs.audioDescriptionPresent) {
                    add("the audio description is missing")
                }
            }
        val limitations = listOf(EXTENDED_AUDIO_DESCRIPTION_LIMIT)
        return if (reasons.isEmpty()) {
            A11yVerdict.Accepted(limitations)
        } else {
            A11yVerdict.Degraded(reasons = reasons, limitations = limitations)
        }
    }
}
