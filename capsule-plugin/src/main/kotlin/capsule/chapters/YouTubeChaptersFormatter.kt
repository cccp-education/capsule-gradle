package capsule.chapters

import video.kernel.publish.VideoDescriptionComposer

/**
 * Formats chapter entries as YouTube chapter text (PRT-10).
 *
 * YouTube recognises a description block of `<timestamp> <title>` lines, the
 * first chapter being at `0:00`. Ported from
 * `prepa_electricite_complet/subs.py` (which wrote `final/chapitres.txt`).
 *
 * HF-YOUTUBE-PUBLISH US-3 — the timestamp formatting now **delegates** to the
 * shared neutral [VideoDescriptionComposer.chapterTimestamp] (single source of
 * truth across capsule and hyperframes; doctrine "factor at 2"), so both
 * engines render identical YouTube timelines.
 *
 * Pure — no I/O.
 */
object YouTubeChaptersFormatter {

    /**
     * Formats [entries] as newline-separated `<timestamp> <title>` lines, or an
     * empty string when [entries] is empty.
     */
    fun format(entries: List<ChapterEntry>): String {
        if (entries.isEmpty()) return ""
        return entries.joinToString("\n") { "${formatTimestamp(it.startMs)} ${it.title}" } + "\n"
    }

    /**
     * `M:SS`, or `H:MM:SS` once the timestamp reaches one hour — the two forms
     * YouTube accepts. Delegates to the neutral kernel.
     */
    fun formatTimestamp(startMs: Long): String =
        VideoDescriptionComposer.chapterTimestamp((startMs / 1000).coerceAtLeast(0).toInt())
}
