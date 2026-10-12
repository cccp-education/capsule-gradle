package capsule.viral

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * One word with its ASR-measured time span (CAP-QC-VOICE US-5).
 *
 * @property text  the word as transcribed.
 * @property start start time in seconds.
 * @property end   end time in seconds (>= [start]).
 */
data class TimedWord(
    val text: String,
    val start: Double,
    val end: Double,
) {
    init {
        require(end >= start) { "TimedWord.end ($end) must be >= start ($start)" }
    }
}

/**
 * Parses word-level timings out of an ASR transcript JSON (CAP-QC-VOICE US-5).
 *
 * Reads the `segments[].words[]` shape produced by whisper / faster-whisper with
 * word timestamps enabled — each word carrying `word` (or `text`) plus `start`
 * and `end` seconds. Tolerant: a missing or malformed document yields an empty
 * list (the karaoke planner then falls back to proportional timing), never an
 * exception.
 *
 * Pure — no Gradle, no I/O.
 */
object WordTimingsJson {

    private val mapper = ObjectMapper()

    /** Normalises a word for matching (`"Bonjour,"` → `"bonjour"`). */
    fun normalize(text: String): String =
        text.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]+"), "")

    /** Extracts the [TimedWord]s from [json], or an empty list on any problem. */
    fun parse(json: String): List<TimedWord> {
        if (json.isBlank()) return emptyList()
        return try {
            val root = mapper.readTree(json)
            val words = mutableListOf<TimedWord>()
            val segments = root.get("segments") ?: return emptyList()
            for (segment in segments) {
                val segmentWords = segment.get("words") ?: continue
                for (node in segmentWords) {
                    val text = node.get("word")?.asText() ?: node.get("text")?.asText() ?: continue
                    val start = node.get("start")?.asDouble() ?: continue
                    val end = node.get("end")?.asDouble() ?: continue
                    val normalized = normalize(text)
                    if (normalized.isNotEmpty() && end >= start) {
                        words += TimedWord(normalized, start, end)
                    }
                }
            }
            words
        } catch (_: Exception) {
            emptyList()
        }
    }
}
