package capsule.viral

import capsule.feed.SlideType

/**
 * Pure parser of the storyboard AsciiDoc document (CAP-VIRAL wiring).
 *
 * The inverse of [ViralStoryboardBuilder]: it reads the document the pilot
 * validated back into a [ViralStoryboard] so the Gradle task can drive the
 * rendering from a document, not from a set of properties. The round-trip
 * `parse(build(sb)) == sb` holds for every valid storyboard.
 *
 * Degradation (economy of ink): a malformed or incomplete document returns
 * `null` — the caller treats it as "no validable storyboard" and the editorial
 * gate blocks rendering. The parser never throws on bad input.
 *
 * Pure — no Gradle, no I/O. Pattern: [ViralHookParser] (tolerant, returns null).
 */
object ViralStoryboardParser {

    private const val HEADER_PREFIX = "= Storyboard — "
    private const val ANGLE_MARKER = "== Angle"
    private const val BEATS_MARKER = "== Beats"
    private const val BEAT_PREFIX = "=== Beat "
    private const val INTENT_PREFIX = "Intent: "
    private const val DURATION_PREFIX = "Duration: "
    private const val RENDER_PREFIX = "Render: "
    private const val MANIM_PREFIX = "Manim scene: "
    private const val ASSET_PREFIX = "Asset: "

    /**
     * Parses [document] into a [ViralStoryboard].
     *
     * @param document the storyboard AsciiDoc document.
     * @return the [ViralStoryboard], or `null` when the document is not a
     *         well-formed validable storyboard.
     */
    fun parse(document: String): ViralStoryboard? {
        val lines = document.lines()
        val header = lines.firstOrNull { it.startsWith(HEADER_PREFIX) } ?: return null
        val message = header.removePrefix(HEADER_PREFIX).trim()
        if (message.isBlank()) return null

        val platform = lines.firstOrNull { it.startsWith(":platform:") }
            ?.removePrefix(":platform:")?.trim()
            ?.let { ViralPlatform.fromString(it) } ?: return null

        val language = lines.firstOrNull { it.startsWith(":language:") }
            ?.removePrefix(":language:")?.trim().orEmpty()
        if (language.isBlank()) return null

        val duration = lines.firstOrNull { it.startsWith(":target-duration:") }
            ?.removePrefix(":target-duration:")?.trim()?.toIntOrNull() ?: return null

        val angle = extractAngle(lines) ?: return null
        val beats = extractBeats(lines)
        if (beats.isEmpty()) return null

        return runCatching {
            ViralStoryboard(
                message = message,
                angle = angle,
                beats = beats,
                platform = platform,
                language = language,
                targetDurationSeconds = duration,
            )
        }.getOrNull()
    }

    /** Extracts the `== Angle` body (the paragraph that follows the marker). */
    private fun extractAngle(lines: List<String>): String? {
        val start = lines.indexOfFirst { it.trim() == ANGLE_MARKER }
        if (start < 0) return null
        val end = lines.indexOfFirst { it.trim() == BEATS_MARKER }.takeIf { it >= 0 } ?: lines.size
        val body = lines.subList(start + 1, end).joinToString("\n").trim()
        return body.ifBlank { null }
    }

    /**
     * Extracts the ordered beats. Each beat block starts at `=== Beat` and ends
     * at the next beat marker (or EOF). Blank lines within a block (e.g. after
     * `Render:`) are skipped when reconstructing a beat.
     */
    private fun extractBeats(lines: List<String>): List<StoryboardBeat> {
        val blocks = mutableListOf<MutableList<String>>()
        var current: MutableList<String>? = null
        for (line in lines) {
            if (line.startsWith(BEAT_PREFIX)) {
                current = mutableListOf(line)
                blocks += current
            } else if (current != null) {
                current += line
            }
        }
        return blocks.mapNotNull { parseBeat(it) }
    }

    private fun parseBeat(block: List<String>): StoryboardBeat? {
        val header = block.first()
        val role = header.removePrefix(BEAT_PREFIX)
            .substringAfter("— ", "")
            .trim()
        if (role.isBlank()) return null

        val intent = block.firstOrNull { it.startsWith(INTENT_PREFIX) }
            ?.removePrefix(INTENT_PREFIX)?.trim().orEmpty()
        if (intent.isBlank()) return null

        val duration = block.firstOrNull { it.startsWith(DURATION_PREFIX) }
            ?.removePrefix(DURATION_PREFIX)?.trim()?.removeSuffix("s")?.toDoubleOrNull() ?: return null

        val typeName = block.firstOrNull { it.startsWith(RENDER_PREFIX) }
            ?.removePrefix(RENDER_PREFIX)?.trim().orEmpty()
        val type = SlideType.entries.firstOrNull { it.name.equals(typeName, ignoreCase = true) }
            ?: SlideType.HTML

        val manimScene = block.firstOrNull { it.startsWith(MANIM_PREFIX) }
            ?.removePrefix(MANIM_PREFIX)?.trim()?.ifBlank { null }

        val asset = block.firstOrNull { it.startsWith(ASSET_PREFIX) }
            ?.removePrefix(ASSET_PREFIX)?.trim().orEmpty()

        return runCatching {
            StoryboardBeat(
                role = role,
                intent = intent,
                durationSeconds = duration,
                type = type,
                manimScene = manimScene,
                asset = asset,
            )
        }.getOrNull()
    }
}
