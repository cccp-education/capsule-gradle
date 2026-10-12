package capsule.kernel

/**
 * Engine-neutral rendering kind of a video segment (CAP-ENGINE-DECOUPLING US-1).
 *
 * The kernel abstracts *what* a segment is, never *how* it is rendered: the
 * concrete renderer (Reveal.js capture, HyperFrames, Manim, …) is an adapter
 * detail. This enum is the neutral vocabulary that replaces the former
 * renderer-leaking `capsule.feed.SlideType`.
 *
 * - [VISUAL]    : a visual scene rendered from markup (static or scripted).
 * - [ANIMATION] : a programmatic animation scene (scene name carried by
 *                 [VideoSegment.scene]).
 */
enum class VideoKind { VISUAL, ANIMATION }
