package capsule.adapter.revealjs

import capsule.kernel.VideoKind
import capsule.kernel.VideoScript
import capsule.kernel.VideoSegment
import java.io.File

/**
 * Reveal.js `-script.txt` contract — the serialization contract exchanged
 * between the slider deck and the capsule video pipeline, extracted from
 * `capsule.feed.CapsuleScriptWriter` / `CapsuleScriptReader`
 * (CAP-ENGINE-DECOUPLING US-3).
 *
 * Contract format:
 * ```
 * === CAPSULE SCRIPT : <name> ===
 *
 * --- SLIDE <n> : <title> [markers] ---
 * <narration>
 * ```
 * Markers supported in the title: `[manim:SceneName]` (→ [VideoKind.ANIMATION] +
 * `scene`), `[html]` (→ [VideoKind.VISUAL]). No marker defaults to
 * [VideoKind.VISUAL]. Round-trip idempotent.
 *
 * Pure string transforms plus file I/O for [read]; no Gradle, no Playwright.
 */
object RevealJsScriptContract {

    private val manimMarkerRegex = Regex("""\[manim:(\w+)]""")
    private const val HTML_MARKER = "[html]"

    /** Serializes [script] into the plain-text `-script.txt` contract. */
    fun write(script: VideoScript): String {
        require(script.segments.isNotEmpty()) { "VideoScript.segments must not be empty" }
        val sb = StringBuilder()
        sb.append("=== CAPSULE SCRIPT : ${script.name} ===")
        sb.append("\n")
        script.segments.forEach { seg ->
            sb.append("\n")
            sb.append("--- SLIDE ${seg.index} : ${decorateTitle(seg)} ---\n")
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

    /** Parses a `-script.txt` file back into a [VideoScript]. */
    fun read(file: File): VideoScript {
        val lines = file.readLines()
        val header = lines.firstOrNull()?.trim().orEmpty()
        val declaredName = if (header.startsWith("=== CAPSULE SCRIPT : ") && header.endsWith(" ===")) {
            header.removePrefix("=== CAPSULE SCRIPT : ")
                .removeSuffix(" ===")
                .trim()
                .ifBlank { null }
        } else null
        val resolvedName: String = declaredName ?: file.nameWithoutExtension

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
                        .replace(HTML_MARKER, "")
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
