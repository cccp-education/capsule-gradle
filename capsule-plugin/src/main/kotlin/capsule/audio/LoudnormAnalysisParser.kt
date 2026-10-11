package capsule.audio

/**
 * Parses the first-pass `loudnorm` JSON block out of ffmpeg's merged output
 * (PRT-09).
 *
 * ffmpeg prints that block on stderr, surrounded by progress lines; the
 * reference pipeline extracts it with a regex before `json.loads`. Here the
 * five numbers are read directly, so the parser stays dependency-free and pure
 * (no Jackson, no I/O).
 *
 * Note: the values are printed as JSON strings (`"input_i" : "-23.03"`), so the
 * quotes are optional in the match.
 */
object LoudnormAnalysisParser {

    /**
     * Extracts the loudness measurement from [output], or `null` when the block
     * is absent or any field is missing/unparseable (degraded — the caller must
     * not apply a two-pass normalisation without measurements).
     */
    fun parse(output: String): LoudnormMeasurement? {
        if (!output.contains("\"input_i\"")) return null
        val inputI = number(output, "input_i") ?: return null
        val inputTp = number(output, "input_tp") ?: return null
        val inputLra = number(output, "input_lra") ?: return null
        val inputThresh = number(output, "input_thresh") ?: return null
        val targetOffset = number(output, "target_offset") ?: return null
        return LoudnormMeasurement(inputI, inputTp, inputLra, inputThresh, targetOffset)
    }

    private fun number(output: String, key: String): Double? {
        val match = Regex(""""$key"\s*:\s*"?(-?[0-9]+(?:\.[0-9]+)?)"?""").find(output) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
