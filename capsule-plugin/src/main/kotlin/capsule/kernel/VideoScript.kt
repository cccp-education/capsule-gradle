package capsule.kernel

/**
 * Aggregate root — the full engine-neutral video production script
 * (CAP-ENGINE-DECOUPLING US-1).
 *
 * Replaces the former `capsule.feed.CapsuleScript`: the `deck`-leaking field
 * name `deckName` becomes the neutral [name], and the aggregate is now defined
 * in the engine-agnostic kernel.
 *
 * Invariants (fail-fast):
 * - [name] must not be blank.
 *
 * @property name     production name (without extension), must be non-blank.
 * @property segments ordered segments (may be empty when nothing is narrated).
 */
data class VideoScript(
    val name: String,
    val segments: List<VideoSegment>,
) {
    init {
        require(name.isNotBlank()) { "VideoScript.name must not be blank" }
    }

    /** `true` when the script carries no segment. */
    val isEmpty: Boolean get() = segments.isEmpty()
}
