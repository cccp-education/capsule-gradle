package capsule.audio

import java.io.File

/**
 * Applies audio post-production to a final capsule WebM (CAP-AUDIO US-2).
 *
 * Post-production pipeline (post subtitle burn-in, pre format conversion):
 * 1. Loudness normalization — FFmpeg `loudnorm` (EBU R128, single-pass,
 *    target [AudioPostConfig.loudnessTarget] LUFS, true peak -1.5 dB,
 *    LRA 11 — standard EBU R128 for streaming web content).
 * 2. BGM mix — `amix=inputs=2:duration=first:dropout_transition=0` when
 *    [AudioPostConfig.bgmEnabled] and the BGM file exists.
 * 3. Sidechain ducking — `sidechaincompress` (threshold -20 dB, ratio 4:1,
 *    attack 5 ms, release 300 ms) when [AudioPostConfig.duckingEnabled]
 *    (lowers the BGM when the voice speaks). Requires BGM to be enabled.
 *
 * Design mirrors [capsule.SubtitleBurnInService] and
 * [capsule.VideoFormatConverter]:
 * - [AudioPostProcessorImpl] uses ffmpeg complex filter.
 * - [NoOpAudioPostProcessor] is the test/no-ffmpeg fallback (returns `false`).
 * - Factory: [capsule.CapsuleManager.resolveAudioPostProcessor]
 *   (5ème `resolve*`, pattern `resolveFormatConverter` — ffmpegPath="noop"
 *   → NoOp, unavailable + non-strict → NoOp, unavailable + strict → throw
 *   via [capsule.StrictModeGuard], available → Impl).
 *
 * Economy of ink (AGENT.adoc): [process] returns `false` when the BGM file
 * is blank/missing (degraded — keeps the original video) rather than
 * throwing. The caller decides whether to apply the result.
 */
interface AudioPostProcessor {

    /**
     * Applies audio post-production to [videoFile] and writes the result
     * to [outputFile].
     *
     * @return `true` if the post-processed video was produced successfully,
     *         `false` otherwise (NoOp fallback, ffmpeg failure, missing
     *         BGM file when enabled). On `false`, the caller keeps the
     *         original video (degraded mode, backward compatibility).
     * @throws AudioPostException on unrecoverable ffmpeg errors
     *         (pattern [capsule.BurnInException])
     */
    fun process(videoFile: File, outputFile: File, config: AudioPostConfig): Boolean

    /** Returns true if the service is available (e.g. ffmpeg found). */
    fun isAvailable(): Boolean

    /** Returns the service name for logging. */
    fun name(): String
}