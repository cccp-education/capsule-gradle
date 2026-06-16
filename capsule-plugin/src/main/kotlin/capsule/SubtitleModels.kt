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

data class SubtitleBurnInStyle(
    val fontSize: Int = 24,
    val fontColor: String = "&H00FFFFFF",
    val outlineColor: String = "&H00000000",
    val position: String = "bottom"
) {
    init {
        require(fontSize in 8..72) { "fontSize must be between 8 and 72, got $fontSize" }
        require(fontColor.matches(Regex("&H[0-9A-Fa-f]{8}"))) { "fontColor must be ASS hex format &HXXXXXXXX, got '$fontColor'" }
        require(outlineColor.matches(Regex("&H[0-9A-Fa-f]{8}"))) { "outlineColor must be ASS hex format &HXXXXXXXX, got '$outlineColor'" }
        require(position in VALID_POSITIONS) { "position must be one of ${VALID_POSITIONS.joinToString(", ")}, got '$position'" }
    }

    fun toForceStyle(): String {
        val alignment = when (position) {
            "bottom" -> 2
            "top" -> 8
            "middle" -> 5
            else -> 2
        }
        return "FontSize=$fontSize,PrimaryColour=$fontColor,OutlineColour=$outlineColor,Outline=1,Shadow=1,Alignment=$alignment"
    }

    companion object {
        val VALID_POSITIONS = setOf("bottom", "top", "middle")
    }
}
