package capsule.viral

import capsule.feed.SlideType

/**
 * One beat of a viral storyboard (CAP-VIRAL US-13).
 *
 * A beat is a time-boxed unit of the short-form narrative: a [role], a textual
 * [intent], its [durationSeconds], the segment [type] that renders it
 * ([SlideType.HTML] or [SlideType.MANIM]) and the [manimScene]/[asset] it needs.
 *
 * @property role           beat role (`hook`, `development`, `cta`, …).
 * @property intent         what the beat says (the beat's message).
 * @property durationSeconds beat duration (seconds), must be > 0.
 * @property type           rendering strategy for the beat.
 * @property manimScene     Manim scene name when [type] is [SlideType.MANIM].
 * @property asset          expected asset (image/overlay), blank when none.
 */
data class StoryboardBeat(
    val role: String,
    val intent: String,
    val durationSeconds: Double,
    val type: SlideType = SlideType.HTML,
    val manimScene: String? = null,
    val asset: String = "",
) {
    init {
        require(role.isNotBlank()) { "StoryboardBeat.role must not be blank" }
        require(intent.isNotBlank()) { "StoryboardBeat.intent must not be blank" }
        require(durationSeconds > 0.0) { "StoryboardBeat.durationSeconds must be > 0, got $durationSeconds" }
        if (type == SlideType.MANIM) {
            require(!manimScene.isNullOrBlank()) { "StoryboardBeat.manimScene is required when type is MANIM" }
        }
    }

    /** `true` when the beat is rendered by a Manim animation. */
    val isManim: Boolean get() = type == SlideType.MANIM
}

/**
 * Immutable storyboard of a viral campaign (CAP-VIRAL US-13).
 *
 * A campaign is *first a document that carries a message* — a point of view to
 * expose — whose function is to **propose a validable storyboard, transformable
 * into a deployable video**. This model is that document: the [message], its
 * [angle], the ordered [beats], the [platform], the [language] and the total
 * [targetDurationSeconds]. It is rendered to AsciiDoc by [ViralStoryboardBuilder]
 * and gated by [ViralStoryboardValidator] (no video is rendered before a
 * validated storyboard — the editorial gate, mirror of rule A9).
 *
 * Invariants (fail-fast):
 * - [message], [angle], [language] must not be blank.
 * - [beats] must not be empty.
 * - [targetDurationSeconds] must be within the short-form range [15, 60].
 *
 * @property message               the message / point of view the campaign exposes.
 * @property angle                 the narrative angle.
 * @property beats                 ordered storyboard beats.
 * @property platform              target platform preset.
 * @property language              target ISO language code.
 * @property targetDurationSeconds target short-form duration, in [15, 60].
 */
data class ViralStoryboard(
    val message: String,
    val angle: String,
    val beats: List<StoryboardBeat>,
    val platform: ViralPlatform,
    val language: String,
    val targetDurationSeconds: Int,
) {
    init {
        require(message.isNotBlank()) { "ViralStoryboard.message must not be blank" }
        require(angle.isNotBlank()) { "ViralStoryboard.angle must not be blank" }
        require(language.isNotBlank()) { "ViralStoryboard.language must not be blank" }
        require(beats.isNotEmpty()) { "ViralStoryboard.beats must not be empty" }
        require(targetDurationSeconds in ViralConfig.MIN_DURATION_SECONDS..ViralConfig.MAX_DURATION_SECONDS) {
            "ViralStoryboard.targetDurationSeconds must be in [${ViralConfig.MIN_DURATION_SECONDS}, ${ViralConfig.MAX_DURATION_SECONDS}], got $targetDurationSeconds"
        }
    }

    /** Sum of the beat durations (seconds). */
    val totalBeatSeconds: Double get() = beats.sumOf { it.durationSeconds }

    /** The `hook` beat when present. */
    val hookBeat: StoryboardBeat? get() = beats.firstOrNull { it.role.equals("hook", ignoreCase = true) }

    /** The `cta` beat when present. */
    val ctaBeat: StoryboardBeat? get() = beats.firstOrNull { it.role.equals("cta", ignoreCase = true) }
}
