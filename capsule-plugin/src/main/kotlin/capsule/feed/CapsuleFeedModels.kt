package capsule.feed

import capsule.kernel.VideoKind
import capsule.kernel.VideoScript
import capsule.kernel.VideoSegment
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

    fun write(script: VideoScript): String {
        require(script.segments.isNotEmpty()) { "VideoScript.segments must not be empty" }
        val sb = StringBuilder()
        sb.append("=== CAPSULE SCRIPT : ${script.name} ===")
        sb.append("\n")
        script.segments.forEach { seg ->
            sb.append("\n")
            val decoratedTitle = decorateTitle(seg)
            sb.append("--- SLIDE ${seg.index} : $decoratedTitle ---\n")
            sb.append(seg.narration.trim())
            sb.append("\n")
        }
        return sb.toString().trimEnd() + "\n"
    }

    private fun decorateTitle(seg: VideoSegment): String {
        val base = seg.title.trim()
        return when (seg.kind) {
            VideoKind.ANIMATION -> {
                val scene = seg.scene
                if (scene.isNullOrBlank()) base else "$base [manim:$scene]"
            }
            VideoKind.VISUAL -> base
        }
    }
}

/**
 * Parses a `*-script.txt` file (the plain-text contract produced by [CapsuleScriptWriter])
 * back into a [VideoScript] production model.
 *
 * Round-trip idempotent with [CapsuleScriptWriter.write] : `read(write(script))` preserves
 * `name`, `segments` ordering, `index`, `title` (markers stripped), `narration`,
 * `kind` and `scene`.
 *
 * Contract parsed:
 * ```
 * === CAPSULE SCRIPT : <name> ===
 *
 * --- SLIDE <n> : <title> [markers] ---
 * <narration>
 * ```
 * Markers supported in the title: `[manim:SceneName]` (→ [VideoKind.ANIMATION] + `scene`),
 * `[html]` (→ [VideoKind.VISUAL]). When no marker is present, defaults to [VideoKind.VISUAL].
 */
object CapsuleScriptReader {

    private val manimMarkerRegex = Regex("""\[manim:(\w+)]""")
    private val htmlMarker = "[html]"

    fun read(file: File): VideoScript {
        val lines = file.readLines()
        val header = lines.firstOrNull()?.trim().orEmpty()
        val name = if (header.startsWith("=== CAPSULE SCRIPT : ") && header.endsWith(" ===")) {
            header.removePrefix("=== CAPSULE SCRIPT : ")
                .removeSuffix(" ===")
                .trim()
                .ifBlank { null }
        } else null
        val resolvedName: String = name ?: file.nameWithoutExtension

        val segments = mutableListOf<VideoSegment>()
        var currentIndex = -1
        var currentTitle = ""
        var currentKind = VideoKind.VISUAL
        var currentScene: String? = null
        val noteLines = mutableListOf<String>()

        val startIdx = if (header.startsWith("=== CAPSULE SCRIPT : ")) 1 else 0
        for (i in startIdx until lines.size) {
            val line = lines[i]
            when {
                line.startsWith("--- SLIDE ") && line.contains(":") -> {
                    flushSegment(segments, currentIndex, currentTitle, currentKind, currentScene, noteLines)
                    val parts = line.removeSurrounding("--- SLIDE ", " ---")
                    val colonIdx = parts.indexOf(":")
                    currentIndex = parts.substring(0, colonIdx).trim()
                        .toIntOrNull() ?: (segments.size + 1)
                    val rawTitle = parts.substring(colonIdx + 1).trim()
                    val manimMatch = manimMarkerRegex.find(rawTitle)
                    currentKind = if (manimMatch != null) VideoKind.ANIMATION else VideoKind.VISUAL
                    currentScene = manimMatch?.groupValues?.get(1)
                    currentTitle = rawTitle
                        .replace(manimMarkerRegex, "")
                        .replace(htmlMarker, "")
                        .trim()
                }
                line.isNotBlank() && currentIndex >= 0 -> noteLines.add(line)
            }
        }

        flushSegment(segments, currentIndex, currentTitle, currentKind, currentScene, noteLines)

        return VideoScript(name = resolvedName, segments = segments)
    }

    private fun flushSegment(
        sink: MutableList<VideoSegment>,
        index: Int,
        title: String,
        kind: VideoKind,
        scene: String?,
        noteLines: MutableList<String>,
    ) {
        if (index < 0 || title.isBlank() || noteLines.isEmpty()) return
        val note = noteLines.joinToString("\n").trim()
        if (note.isBlank()) return
        sink.add(
            VideoSegment(
                index = index,
                title = title,
                narration = note,
                kind = kind,
                scene = scene,
            )
        )
        noteLines.clear()
    }
}
