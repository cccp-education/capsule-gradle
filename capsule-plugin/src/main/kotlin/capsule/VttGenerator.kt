package capsule

class VttGenerator : SubtitleGenerator {

    override fun format(): SubtitleFormat = SubtitleFormat.VTT

    override fun generate(entries: List<SubtitleEntry>): String {
        val cues = if (entries.isEmpty()) {
            ""
        } else {
            entries.joinToString("\n\n") { entry ->
                buildString {
                    appendLine(entry.index.toString())
                    appendLine("${formatVttTimestamp(entry.startTime)} --> ${formatVttTimestamp(entry.endTime)}")
                    append(entry.text)
                }
            }
        }

        return buildString {
            appendLine("WEBVTT")
            if (cues.isNotEmpty()) {
                appendLine()
                append(cues)
            }
        }
    }

    companion object {
        fun formatVttTimestamp(totalSeconds: Double): String {
            val hours = (totalSeconds / 3600).toInt()
            val minutes = ((totalSeconds % 3600) / 60).toInt()
            val seconds = (totalSeconds % 60).toInt()
            val millis = ((totalSeconds % 1) * 1000).toInt()
            return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
        }
    }
}
