package capsule.feed

import capsule.adapter.revealjs.RevealJsScriptContract
import video.kernel.segment.VideoKind
import video.kernel.segment.VideoScript
import video.kernel.segment.VideoSegment
import java.io.File

/**
 * Renderer vocabulary of a segment — **legacy** type (CAP-ENGINE-DECOUPLING US-1).
 *
 * Superseded by the engine-neutral [VideoKind] for the video production model.
 * It is kept because it remains the serialization vocabulary of the viral
 * storyboard ([capsule.viral.StoryboardBeat]) and of the `-script.txt` contract
 * markers (`[manim:Scene]`). Use [toVideoKind] to move into the neutral kernel.
 *
 * - [HTML]  : segment rendered from markup.
 * - [MANIM] : segment rendered as a Manim animation.
 */
enum class SlideType { HTML, MANIM }

/** Maps the legacy [SlideType] to the neutral [VideoKind]. */
fun SlideType.toVideoKind(): VideoKind = when (this) {
    SlideType.HTML -> VideoKind.VISUAL
    SlideType.MANIM -> VideoKind.ANIMATION
}

/** Maps the neutral [VideoKind] back to the legacy [SlideType]. */
fun VideoKind.toSlideType(): SlideType = when (this) {
    VideoKind.VISUAL -> SlideType.HTML
    VideoKind.ANIMATION -> SlideType.MANIM
}

/**
 * Backward-compatible alias — a `SlideSegment` **is** a [VideoSegment]
 * (CAP-ENGINE-DECOUPLING US-1). Prefer the neutral [VideoSegment] in new code.
 */
typealias SlideSegment = VideoSegment

/**
 * Backward-compatible alias — a `CapsuleScript` **is** a [VideoScript]
 * (CAP-ENGINE-DECOUPLING US-1). Prefer the neutral [VideoScript] in new code.
 */
typealias CapsuleScript = VideoScript

/**
 * Legacy rendering-type accessor — the neutral field is [VideoSegment.kind].
 */
@Deprecated("Neutralized vocabulary: use VideoSegment.kind", ReplaceWith("kind.toSlideType()"))
val VideoSegment.type: SlideType get() = kind.toSlideType()

/**
 * Legacy deck-name accessor — the neutral field is [VideoScript.name].
 */
@Deprecated("Neutralized vocabulary: use VideoScript.name", ReplaceWith("name"))
val VideoScript.deckName: String get() = name

/**
 * Serializes a [VideoScript] into the plain-text contract consumed by
 * [CapsuleScriptReader] (round-trip idempotent).
 *
 * Contract format:
 * ```
 * === CAPSULE SCRIPT : <name> ===
 *
 * --- SLIDE <n> : <title> ---
 * <narration>
 *
 * --- SLIDE <n+1> : <title> ---
 * <narration>
 * ```
 */
object CapsuleScriptWriter {

    /**
     * Serializes [script] into the `-script.txt` contract.
     *
     * **Legacy compat surface** (CAP-ENGINE-DECOUPLING US-3): delegates to
     * [RevealJsScriptContract] so existing call-sites keep working.
     */
    fun write(script: VideoScript): String = RevealJsScriptContract.write(script)
}

/**
 * Parses a `*-script.txt` file (the plain-text contract produced by [CapsuleScriptWriter])
 * back into a [VideoScript] production model.
 *
 * **Legacy compat surface** (CAP-ENGINE-DECOUPLING US-3): delegates to
 * [RevealJsScriptContract] so existing call-sites keep working.
 */
object CapsuleScriptReader {

    fun read(file: File): VideoScript = RevealJsScriptContract.read(file)
}
