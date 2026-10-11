package capsule.chapters

/**
 * Formats chapter entries as YouTube chapter text (PRT-10).
 *
 * YouTube recognises a description block of `<timestamp> <title>` lines, the
 * first chapter being at `0:00`. Ported from
 * `prepa_electricite_complet/subs.py` (which wrote `final/chapitres.txt`).
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
     * YouTube accepts.
     */
    fun formatTimestamp(startMs: Long): String {
        val totalSeconds = (startMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
