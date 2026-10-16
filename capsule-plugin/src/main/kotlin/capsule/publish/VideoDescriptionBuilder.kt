package capsule.publish

import video.kernel.publish.VideoPublicationLimits

/**
 * Builds a YouTube-ready video description (PRT-10).
 *
 * YouTube rejects angle brackets in a description; the builder strips any
 * forbidden character and reports how many it removed. Ported from
 * `prepa_electricite_complet/description.py`, whose final check asserts the
 * description contains no `<` / `>`.
 *
 * HF-YOUTUBE-PUBLISH US-3 — the forbidden-character set is now **shared** with
 * the neutral kernel ([VideoPublicationLimits.FORBIDDEN_CHARS]) so capsule and
 * hyperframes reject the exact same characters (doctrine "factor at 2").
 *
 * Pure — no I/O.
 */
object VideoDescriptionBuilder {

    /** Characters YouTube rejects in a description (neutral kernel source of truth). */
    val FORBIDDEN_CHARS: Set<Char> = VideoPublicationLimits.FORBIDDEN_CHARS

    /**
     * @param text                   the description, free of [FORBIDDEN_CHARS].
     * @param strippedForbiddenChars how many forbidden characters were removed.
     */
    data class Result(val text: String, val strippedForbiddenChars: Int)

    /**
     * Assembles a description from an optional [header], a [title], chapter
     * [chapterLines] and [tags] (each prefixed with `#` when needed), then strips
     * every [FORBIDDEN_CHARS].
     */
    fun build(
        title: String,
        chapterLines: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        header: String = "",
    ): Result {
        val sb = StringBuilder()
        if (header.isNotBlank()) sb.append(header).append("\n\n")
        if (title.isNotBlank()) sb.append(title).append("\n\n")
        if (chapterLines.isNotEmpty()) {
            sb.append("CHAPTERS\n")
            chapterLines.forEach { sb.append(it).append('\n') }
            sb.append('\n')
        }
        if (tags.isNotEmpty()) {
            sb.append(tags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" }).append('\n')
        }
        val raw = sb.toString().trimEnd() + "\n"

        val cleaned = StringBuilder(raw.length)
        var stripped = 0
        for (ch in raw) {
            if (ch in FORBIDDEN_CHARS) stripped++ else cleaned.append(ch)
        }
        return Result(cleaned.toString(), stripped)
    }
}
