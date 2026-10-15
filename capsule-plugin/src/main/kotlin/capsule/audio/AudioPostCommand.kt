package capsule.audio

import java.io.File

/**
 * Pure builder for the FFmpeg complex filter graph and argv of
 * [AudioPostProcessorImpl] (CAP-AUDIO US-2).
 *
 * Pure object — no I/O, no Gradle dependencies, fully testable.
 *
 * [buildFilterGraph] assembles the `filter_complex` chain:
 * - loudness normalization (`loudnorm=I=<target>:TP=-1.5:LRA=11`) is
 *   always applied (even when BGM/ducking are disabled — a capsule
 *   without loudness normalization sounds amateur).
 * - BGM mix (`volume + amix`) is applied only when
 *   [AudioPostConfig.bgmEnabled] and `hasBgmInput` is true.
 * - sidechain ducking (`sidechaincompress`) is applied only when
 *   [AudioPostConfig.duckingEnabled] **and** BGM is enabled — ducking
 *   without BGM is meaningless and is ignored.
 *
 * [buildArgv] assembles the full FFmpeg command line. When BGM is
 * enabled, the BGM file is a second `-i` input and `filter_complex` is
 * used. When BGM is disabled, only `-af` is used (single input).
 */
object AudioPostCommand {

    /**
     * Builds the FFmpeg filter graph string for [config].
     *
     * @param config      the audio post-production config
     * @param hasBgmInput `true` if the BGM file is available as a second
     *        ffmpeg input (when `false` and `bgmEnabled`, the BGM mix is
     *        skipped — degraded mode is the caller's responsibility)
     * @return the filter graph string (for `-filter_complex` or `-af`)
     */
    fun buildFilterGraph(config: AudioPostConfig, hasBgmInput: Boolean): String {
        // S-203 (dogfooding réel) — two real-ffmpeg fixes:
        //  1. `loudnorm` outputs 192 kHz; without a resample, a following `amix`
        //     (BGM mix) truncates the stream. Re-sample back to 48 kHz.
        //  2. In the ducking branch, `[voice]` must be `asplit` before being
        //     consumed by BOTH `sidechaincompress` and `amix` (a filter output
        //     pad is single-use — reusing the label fails with
        //     "Invalid stream specifier: voice").
        val loudness = "loudnorm=I=${config.loudnessTarget}:TP=-1.5:LRA=11,aresample=48000"

        // Ducking without BGM is meaningless — ignore it.
        val effectiveBgm = config.bgmEnabled && hasBgmInput
        if (!effectiveBgm) {
            return loudness
        }

        // BGM mix path: voice → loudnorm, BGM → volume, amix both.
        val bgmVolume = "volume=${config.bgmLevel}dB"
        val amix = "amix=inputs=2:duration=first:dropout_transition=0"

        if (!config.duckingEnabled) {
            // loudness on voice, volume on BGM, amix — filter_complex chain.
            return "[0:a]${loudness}[voice];" +
                "[1:a]${bgmVolume}[bgm];" +
                "[voice][bgm]${amix}[aout]"
        }

        // Ducking: sidechaincompress on BGM driven by voice; voice split for mix.
        val ducking = "sidechaincompress=threshold=0.1:ratio=4:attack=5:release=300"
        return "[0:a]${loudness}[voice];" +
            "[1:a]${bgmVolume}[bgm];" +
            "[voice]asplit=2[vsc][vmix];" +
            "[vsc][bgm]${ducking}[ducked];" +
            "[vmix][ducked]${amix}[aout]"
    }

    /**
     * Builds the full FFmpeg argv for audio post-production.
     *
     * @param videoFile  the input WebM video
     * @param bgmFile    the BGM audio file, or `null` when BGM is disabled
     * @param outputFile the output video file
     * @param config     the audio post-production config
     * @param ffmpegPath the ffmpeg executable path
     * @return the argv list (first element is the ffmpeg path)
     */
    fun buildArgv(
        videoFile: File,
        bgmFile: File?,
        outputFile: File,
        config: AudioPostConfig,
        ffmpegPath: String
    ): List<String> {
        val argv = mutableListOf(ffmpegPath, "-y", "-i", videoFile.absolutePath)

        val hasBgm = config.bgmEnabled && bgmFile != null
        if (hasBgm) {
            argv.add("-i")
            argv.add(bgmFile.absolutePath)
            argv.add("-filter_complex")
            argv.add(buildFilterGraph(config, hasBgmInput = true))
            argv.add("-map")
            argv.add("0:v")
            argv.add("-map")
            argv.add("[aout]")
        } else {
            argv.add("-af")
            argv.add(buildFilterGraph(config, hasBgmInput = false))
        }

        argv.add("-c:v")
        argv.add("copy")
        argv.add("-c:a")
        argv.add("libvorbis")
        argv.add("-shortest")
        argv.add(outputFile.absolutePath)

        return argv
    }

    // ─── PRT-09 — two-pass loudness (exact) ────────────────────────────────

    /**
     * First-pass `loudnorm` argv that only MEASURES (PRT-09): it decodes the
     * input, writes the JSON metrics to the (merged) output and produces no file
     * (`-f null -`). The five numbers are re-injected on the second pass via
     * [buildTwoPassLoudnorm].
     *
     * Ported from `prepa_electricite_complet/assemble.py`, which measures once
     * then normalises with the measured values plus a make-up gain and a limiter.
     */
    fun loudnormMeasureArgs(videoFile: File, config: AudioPostConfig, ffmpegPath: String): List<String> =
        listOf(
            ffmpegPath, "-hide_banner", "-i", videoFile.absolutePath,
            "-af", "loudnorm=I=${config.loudnessTarget}:TP=$TWO_PASS_TP:LRA=11:print_format=json",
            "-f", "null", "-",
        )

    /**
     * Second-pass filter chain built from the [measured] values (PRT-09):
     * loudnorm in `linear` mode using the measured metrics, then a fixed make-up
     * gain, then a limiter placed BEFORE the encoder (the peak rises again after
     * loudnorm), then a 48 kHz resample.
     *
     * @param gainDb make-up gain applied after loudnorm; loudnorm `linear`
     *        caps below the target, so a fixed gain finishes the rise.
     * @param limit  the limiter ceiling (linear, 0..1); `0.63` ≈ -4 dBFS.
     */
    fun buildTwoPassLoudnorm(
        config: AudioPostConfig,
        measured: LoudnormMeasurement,
        gainDb: Double = DEFAULT_MAKEUP_GAIN_DB,
        limit: Double = DEFAULT_LIMITER_LIMIT,
    ): String =
        "loudnorm=I=${config.loudnessTarget}:TP=$TWO_PASS_TP:LRA=11:" +
            "measured_I=${fmt(measured.inputI)}:measured_TP=${fmt(measured.inputTp)}:" +
            "measured_LRA=${fmt(measured.inputLra)}:measured_thresh=${fmt(measured.inputThresh)}:" +
            "offset=${fmt(measured.targetOffset)}:linear=true," +
            "volume=${fmt(gainDb)}dB,alimiter=limit=${fmt(limit)}:level=disabled,aresample=48000"

    /**
     * Full argv for the two-pass path (PRT-09).
     *
     * Only the **single-input** (no BGM) case is supported here: with BGM the mix
     * must be measured first, which the existing single-pass complex graph
     * ([buildArgv]) already covers. The caller must therefore only route to this
     * path when BGM is disabled.
     */
    fun buildTwoPassArgv(
        videoFile: File,
        outputFile: File,
        config: AudioPostConfig,
        measured: LoudnormMeasurement,
        ffmpegPath: String,
    ): List<String> = listOf(
        ffmpegPath, "-y", "-i", videoFile.absolutePath,
        "-af", buildTwoPassLoudnorm(config, measured),
        "-c:v", "copy", "-c:a", "libvorbis", "-shortest",
        outputFile.absolutePath,
    )

    private fun fmt(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value)

    /** True-peak target used by the two-pass chain (reference value). */
    const val TWO_PASS_TP: String = "-2"

    /** Make-up gain applied after loudnorm (reference value, dB). */
    const val DEFAULT_MAKEUP_GAIN_DB: Double = 1.7

    /** Limiter ceiling before the encoder (reference value, linear). */
    const val DEFAULT_LIMITER_LIMIT: Double = 0.63
}