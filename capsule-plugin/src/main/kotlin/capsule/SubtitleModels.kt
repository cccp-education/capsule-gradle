package capsule

enum class SubtitleFormat {
    SRT, VTT;

    val fileExtension: String
        get() = when (this) {
            SRT -> ".srt"
            VTT -> ".vtt"
        }

    companion object {
        fun fromString(value: String): SubtitleFormat {
            return when (value.lowercase()) {
                "vtt" -> VTT
                else -> SRT
            }
        }
    }
}

data class SubtitleEntry(
    val index: Int,
    val startTime: Double,
    val endTime: Double,
    val text: String
) {
    init {
        require(startTime <= endTime) {
            "startTime ($startTime) must be <= endTime ($endTime)"
        }
    }
}

interface SubtitleGenerator {
    fun generate(entries: List<SubtitleEntry>): String
    fun format(): SubtitleFormat
}
