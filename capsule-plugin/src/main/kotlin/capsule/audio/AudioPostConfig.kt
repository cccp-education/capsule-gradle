package capsule.audio

/**
 * Configuration section for audio post-production (CAP-AUDIO US-1).
 *
 * Applies BGM mix, loudness normalization (EBU R128), and sidechain
 * ducking to the final capsule WebM, *after* subtitle burn-in and
 * *before* MP4 format conversion. This is a pure config data class —
 * the `AudioPostProcessor` (US-2) consumes it.
 *
 * All fields default to disabled/empty to preserve backward
 * compatibility — existing configs without an `audioPost` section
 * keep the no-audio-post behavior.
 *
 * @param bgmEnabled      `true` to mix background music under the voice
 *        track (default `false` — opt-in).
 * @param bgmFile         absolute or relative path to the BGM audio
 *        file (default `""` — no BGM file). When [bgmEnabled] is true
 *        but this is blank, the post-processor degrades gracefully
 *        (keeps the original video, logs a warning).
 * @param bgmLevel        BGM level in dB (default `-18.0` — typical
 *        music bed level under a voice track).
 * @param loudnessTarget  target loudness in LUFS for EBU R128
 *        normalization (default `-16.0` — standard for streaming
 *        web content).
 * @param duckingEnabled  `true` to enable sidechain compression that
 *        lowers the BGM when the voice speaks (default `false` —
 *        opt-in, requires [bgmEnabled] to have an effect).
 */
data class AudioPostConfig(
    val bgmEnabled: Boolean = false,
    val bgmFile: String = "",
    val bgmLevel: Double = -18.0,
    val loudnessTarget: Double = -16.0,
    val duckingEnabled: Boolean = false
)