package capsule.a11y

import java.io.File

/**
 * Parser of an explicit audio-description script (CAP-AUDIODESC US-2).
 *
 * The authored description is a plain text (or AsciiDoc-adjacent) file that
 * lists one cue per line:
 *
 * ```
 * # a comment line is ignored
 * 2.5 | A container appears on the left.
 * 3.0 | The terminal shows the failing build.
 * ```
 *
 * The format is intentionally trivial: `<durationSeconds> | <text>`. Parsing is
 * **defensive** — a malformed line is skipped (silent degradation, as the EPIC
 * requires: an absent or partial description must never fail the build).
 *
 * The planner ([AudioDescriptionPlanner]) only needs the text and an estimated
 * duration; the real voice length is measured by the TTS step (US-3).
 */
object AudioDescriptionScriptParser {
    /** Field separator between the duration and the text. */
    const val SEPARATOR: Char = '|'

    /**
     * Parses [source] into cues, in document order.
     *
     * Blank lines and lines starting with `#` are ignored; a line without a
     * separator, a non-numeric duration, or a non-positive duration is skipped.
     */
    fun parse(source: String): List<AudioDescriptionCue> =
        source
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line -> cueOf(line) }
            .toList()

    /**
     * Reads [file] and parses it; a missing or unreadable file yields an **empty**
     * list (silent degradation — the video keeps its narration only).
     */
    fun load(file: File): List<AudioDescriptionCue> {
        if (!file.isFile) return emptyList()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        return parse(text)
    }

    private fun cueOf(line: String): AudioDescriptionCue? {
        val separator = line.indexOf(SEPARATOR)
        if (separator < 0) return null
        val duration = line.substring(0, separator).trim().toDoubleOrNull() ?: return null
        if (duration <= 0.0) return null
        val text = line.substring(separator + 1).trim()
        if (text.isEmpty()) return null
        return runCatching { AudioDescriptionCue(text = text, durationSeconds = duration) }.getOrNull()
    }
}
