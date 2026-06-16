package capsule

class SrtGenerator : SubtitleGenerator {

    override fun format(): SubtitleFormat = SubtitleFormat.SRT

    override fun generate(entries: List<SubtitleEntry>): String {
        if (entries.isEmpty()) return ""

        return entries.joinToString("\n\n") { entry ->
            buildString {
                appendLine(entry.index.toString())
                appendLine("${formatSrtTimestamp(entry.startTime)} --> ${formatSrtTimestamp(entry.endTime)}")
                append(entry.text)
            }
        }
    }

    companion object {
        fun formatSrtTimestamp(totalSeconds: Double): String {
            val hours = (totalSeconds / 3600).toInt()
            val minutes = ((totalSeconds % 3600) / 60).toInt()
            val seconds = (totalSeconds % 60).toInt()
            val millis = ((totalSeconds % 1) * 1000).toInt()
            return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
        }
    }
}
