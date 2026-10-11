package capsule.voice

import capsule.ProcessRunner
import java.io.File

/**
 * I/O half of the SNR estimate (CAP-QC-VOICE US-2).
 *
 * Decodes a compressed take (`slide-NN.mp3`) to raw 16-bit mono PCM at a fixed
 * sample rate with ffmpeg, then hands the bytes to [RmsEnvelope]. Mirrors the
 * split of `capsule.validation.VideoIntegrityProbe` (I/O) /
 * `VideoIntegrityChecker` (pure decision).
 *
 * Degrades to `null` on any failure (missing file, ffmpeg absent, empty
 * output): the SNR check is then skipped, never read as clean.
 */
object AudioEnvelopeExtractor {

    /** 16 kHz mono is the usual ASR/envelope working rate and keeps frames tiny. */
    const val SAMPLE_RATE: Int = 16_000

    /** 20 ms frames — short enough to isolate a breath, long enough to be stable. */
    const val FRAME_MILLIS: Int = 20

    /**
     * Extracts the per-frame RMS envelope of [audioFile].
     *
     * @return the envelope, or `null` when it could not be produced.
     */
    fun extract(
        audioFile: File,
        ffmpegPath: String = "ffmpeg",
        sampleRate: Int = SAMPLE_RATE,
        frameMillis: Int = FRAME_MILLIS,
    ): DoubleArray? {
        if (!audioFile.isFile || audioFile.length() == 0L) return null
        val pcmFile = File.createTempFile("capsule-envelope", ".pcm")
        return try {
            val result = ProcessRunner.run(decodeArgs(audioFile, pcmFile, sampleRate, ffmpegPath))
            if (!result.isSuccess || !pcmFile.isFile || pcmFile.length() == 0L) {
                null
            } else {
                val frameSamples = sampleRate * frameMillis / 1000
                RmsEnvelope.framesFromPcm(pcmFile.readBytes(), bytesPerSample = 2, frameSamples = frameSamples)
            }
        } catch (_: Exception) {
            null
        } finally {
            pcmFile.delete()
        }
    }

    /**
     * The ffmpeg argv that decodes [audioFile] to raw s16le mono PCM in
     * [pcmFile]. Exposed for tests — the decode command is part of the contract.
     */
    internal fun decodeArgs(
        audioFile: File,
        pcmFile: File,
        sampleRate: Int,
        ffmpegPath: String,
    ): List<String> = listOf(
        ffmpegPath,
        "-v", "error",
        "-i", audioFile.absolutePath,
        "-ac", "1",
        "-ar", sampleRate.toString(),
        "-f", "s16le",
        pcmFile.absolutePath,
    )
}
