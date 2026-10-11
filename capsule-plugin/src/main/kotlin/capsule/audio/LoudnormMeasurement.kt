package capsule.audio

/**
 * First-pass EBU R128 loudness measurements (PRT-09).
 *
 * `ffmpeg -af loudnorm=...:print_format=json` prints these five numbers on the
 * first pass; feeding them back on the second pass makes the normalisation
 * exact instead of approximate-on-a-single-pass. Ported from
 * `prepa_electricite_complet/assemble.py`.
 *
 * Pure — no I/O.
 */
data class LoudnormMeasurement(
    val inputI: Double,
    val inputTp: Double,
    val inputLra: Double,
    val inputThresh: Double,
    val targetOffset: Double,
)
