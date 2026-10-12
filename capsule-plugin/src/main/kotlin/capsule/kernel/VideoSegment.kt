package capsule.kernel

/**
 * One engine-neutral video segment (CAP-ENGINE-DECOUPLING US-1).
 *
 * A segment is a unit of narration paired with a rendering decision expressed
 * in neutral terms — [kind] (visual vs animation) and an optional animation
 * [scene] name. The kernel never mentions `slide`, `deck`, `reveal` or a
 * concrete renderer; those concepts live in the adapters.
 *
 * Invariants (fail-fast):
 * - [title] must not be blank.
 * - [narration] must not be blank.
 *
 * @property index     1-based position of the segment in the production.
 * @property title     segment title, must be non-blank.
 * @property narration narration body, must be non-blank.
 * @property kind      rendering kind — [VideoKind.VISUAL] (default) or
 *                     [VideoKind.ANIMATION].
 * @property scene     animation scene name when [kind] is
 *                     [VideoKind.ANIMATION], otherwise null.
 */
data class VideoSegment(
    val index: Int,
    val title: String,
    val narration: String,
    val kind: VideoKind = VideoKind.VISUAL,
    val scene: String? = null,
) {
    init {
        require(title.isNotBlank()) { "VideoSegment.title must not be blank (index=$index)" }
        require(narration.isNotBlank()) { "VideoSegment.narration must not be blank (index=$index)" }
    }

    /** Legacy narration accessor — the neutral field is [narration]. */
    @Deprecated("Neutralized vocabulary: use narration", ReplaceWith("narration"))
    val speakerNote: String get() = narration

    /** Legacy animation-scene accessor — the neutral field is [scene]. */
    @Deprecated("Neutralized vocabulary: use scene", ReplaceWith("scene"))
    val manimScene: String? get() = scene
}
