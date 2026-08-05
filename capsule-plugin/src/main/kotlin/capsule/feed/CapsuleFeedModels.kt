package capsule.feed

import java.io.File

/**
 * Type of a slide segment — drives rendering strategy (HTML capture vs Manim scene).
 *
 * - [HTML] : slide rendered as HTML (default, Reveal.js capture via Playwright).
 * - [MANIM] : slide rendered as a Manim animation (scene name carried by [SlideSegment.manimScene]).
 */
enum class SlideType { HTML, MANIM }

/**
 * One slide segment inside a capsule feed script.
 *
 * @param index 1-based slide position.
 * @param title slide title (AsciiDoc `== ` heading text), must be non-blank.
 * @param speakerNote narration body, must be non-blank.
 * @param type rendering strategy — [SlideType.HTML] (default) or [SlideType.MANIM].
 * @param manimScene Manim scene name when [type] is [SlideType.MANIM], otherwise null.
 */
data class SlideSegment(
    val index: Int,
    val title: String,
    val speakerNote: String,
    val type: SlideType = SlideType.HTML,
    val manimScene: String? = null,
) {
    init {
        require(title.isNotBlank()) { "SlideSegment.title must not be blank (index=$index)" }
        require(speakerNote.isNotBlank()) { "SlideSegment.speakerNote must not be blank (index=$index)" }
    }
}

/**
 * Aggregate root — the full capsule feed script for one deck.
 *
 * Produced by [AsciidocSpeakerNoteParser] from an AsciiDoc deck and serialized
 * by [CapsuleScriptWriter] into the plain-text contract consumed by
 * capsule-gradle video tasks ([CapsuleScriptReader]).
 *
 * @param deckName deck name (without extension), must be non-blank.
 * @param segments ordered slide segments (may be empty when no slide carries
 * a `[NOTE.speaker]` block).
 */
data class CapsuleScript(
    val deckName: String,
    val segments: List<SlideSegment>,
) {
    init {
        require(deckName.isNotBlank()) { "CapsuleScript.deckName must not be blank" }
    }

    val isEmpty: Boolean get() = segments.isEmpty()
}

/**
 * Serializes a [CapsuleScript] into the plain-text contract consumed by
 * [CapsuleScriptReader] (round-trip idempotent).
 *
 * Contract format:
 * ```
 * === CAPSULE SCRIPT : <deckName> ===
 *
 * --- SLIDE <n> : <title> ---
 * <speakerNote>
 *
 * --- SLIDE <n+1> : <title> ---
 * <speakerNote>
 * ```
 */
object CapsuleScriptWriter {

    fun write(script: CapsuleScript): String {
        require(script.segments.isNotEmpty()) { "CapsuleScript.segments must not be empty" }
        val sb = StringBuilder()
        sb.append("=== CAPSULE SCRIPT : ${script.deckName} ===")
        sb.append("\n")
        script.segments.forEach { seg ->
            sb.append("\n")
            val decoratedTitle = decorateTitle(seg)
            sb.append("--- SLIDE ${seg.index} : $decoratedTitle ---\n")
            sb.append(seg.speakerNote.trim())
            sb.append("\n")
        }
        return sb.toString().trimEnd() + "\n"
    }

    private fun decorateTitle(seg: SlideSegment): String {
        val base = seg.title.trim()
        return when (seg.type) {
            SlideType.MANIM -> {
                val scene = seg.manimScene
                if (scene.isNullOrBlank()) base else "$base [manim:$scene]"
            }
            SlideType.HTML -> base
        }
    }
}

/**
 * Parses a `*-script.txt` file (the plain-text contract produced by [CapsuleScriptWriter])
 * back into a [CapsuleScript] feed model.
 *
 * Round-trip idempotent with [CapsuleScriptWriter.write] : `read(write(script))` preserves
 * `deckName`, `segments` ordering, `index`, `title` (markers stripped), `speakerNote`,
 * `type` and `manimScene`.
 *
 * Contract parsed:
 * ```
 * === CAPSULE SCRIPT : <deckName> ===
 *
 * --- SLIDE <n> : <title> [markers] ---
 * <speakerNote>
 * ```
 * Markers supported in the title: `[manim:SceneName]` (→ [SlideType.MANIM] + `manimScene`),
 * `[html]` (→ [SlideType.HTML]). When no marker is present, defaults to [SlideType.HTML].
 */
object CapsuleScriptReader {

    private val manimMarkerRegex = Regex("""\[manim:(\w+)]""")
    private val htmlMarker = "[html]"

    fun read(file: File): CapsuleScript {
        val lines = file.readLines()
        val header = lines.firstOrNull()?.trim().orEmpty()
        val deckName = if (header.startsWith("=== CAPSULE SCRIPT : ") && header.endsWith(" ===")) {
            header.removePrefix("=== CAPSULE SCRIPT : ")
                .removeSuffix(" ===")
                .trim()
                .ifBlank { null }
        } else null
        val resolvedDeckName: String = deckName ?: file.nameWithoutExtension

        val segments = mutableListOf<SlideSegment>()
        var currentIndex = -1
        var currentTitle = ""
        var currentType = SlideType.HTML
        var currentManimScene: String? = null
        val noteLines = mutableListOf<String>()

        val startIdx = if (header.startsWith("=== CAPSULE SCRIPT : ")) 1 else 0
        for (i in startIdx until lines.size) {
            val line = lines[i]
            when {
                line.startsWith("--- SLIDE ") && line.contains(":") -> {
                    flushSegment(segments, currentIndex, currentTitle, currentType, currentManimScene, noteLines)
                    val parts = line.removeSurrounding("--- SLIDE ", " ---")
                    val colonIdx = parts.indexOf(":")
                    currentIndex = parts.substring(0, colonIdx).trim()
                        .toIntOrNull() ?: (segments.size + 1)
                    val rawTitle = parts.substring(colonIdx + 1).trim()
                    val manimMatch = manimMarkerRegex.find(rawTitle)
                    currentType = if (manimMatch != null) SlideType.MANIM else SlideType.HTML
                    currentManimScene = manimMatch?.groupValues?.get(1)
                    currentTitle = rawTitle
                        .replace(manimMarkerRegex, "")
                        .replace(htmlMarker, "")
                        .trim()
                }
                line.isNotBlank() && currentIndex >= 0 -> noteLines.add(line)
            }
        }

        flushSegment(segments, currentIndex, currentTitle, currentType, currentManimScene, noteLines)

        return CapsuleScript(deckName = resolvedDeckName, segments = segments)
    }

    private fun flushSegment(
        sink: MutableList<SlideSegment>,
        index: Int,
        title: String,
        type: SlideType,
        manimScene: String?,
        noteLines: MutableList<String>,
    ) {
        if (index < 0 || title.isBlank() || noteLines.isEmpty()) return
        val note = noteLines.joinToString("\n").trim()
        if (note.isBlank()) return
        sink.add(
            SlideSegment(
                index = index,
                title = title,
                speakerNote = note,
                type = type,
                manimScene = manimScene,
            )
        )
        noteLines.clear()
    }
}