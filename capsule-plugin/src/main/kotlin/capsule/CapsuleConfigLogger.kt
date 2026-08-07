package capsule

/**
 * CR-8 — Structured config logging.
 *
 * Decomposes the monolithic 13-placeholder log line into 4 section lines:
 * TTS, Capture, Subtitles, Manim. Tokens searched by existing Cucumber steps
 * (`engine=`, `quality=`, `subtitle=`, `subtitleFormat=`, `burnIn=`, `scriptsDir=`)
 * are preserved verbatim so BDD assertions stay green.
 */
object CapsuleConfigLogger {

    fun formatConfigLog(config: CapsuleConfig): List<String> = listOf(
        formatTtsLine(config.tts),
        formatCaptureLine(config.capture),
        formatSubtitleLine(config.capture),
        formatManimLine(config.manim)
    )

    private fun formatTtsLine(tts: TtsConfig): String =
        "Capsule TTS: engine=${tts.engine}, voice=${tts.voice}, language=${tts.language}"

    private fun formatCaptureLine(capture: CaptureConfig): String =
        "Capsule Capture: vw=${capture.viewportWidth}x${capture.viewportHeight}, parallel=${capture.parallelCaptureEnabled}"

    private fun formatSubtitleLine(capture: CaptureConfig): String =
        "Capsule Subtitles: subtitle=${capture.subtitleEnabled}, subtitleFormat=${capture.subtitleFormat}, burnIn=${capture.subtitleBurnIn}"

    private fun formatManimLine(manim: ManimConfig): String =
        "Capsule Manim: path=${manim.executablePath}, quality=${manim.quality}, scriptsDir=${manim.scriptsDir}, outputDir=${manim.outputDir}"
}