package capsule.voice

import java.io.File

/**
 * ASR port (CAP-QC-VOICE US-2).
 *
 * Transcribes one audio file into text. The implementation is deliberately
 * outside the JVM (a `whisper` / `faster-whisper` CLI) so the plugin never
 * embeds an acoustic model — the port keeps the domain testable with a fake,
 * and a missing binary degrades instead of failing `check`.
 *
 * Ported from the role of `prepa_electricite_complet/verif_voix.py` (`faster-whisper`
 * large-v3 transcription step).
 */
fun interface SpeechRecognizer {

    /**
     * @param audio    the audio file to transcribe.
     * @param language the language hint (ISO-639-1).
     * @return the transcription (empty when nothing was recognised).
     * @throws Exception when the underlying recogniser cannot run — the caller
     *         treats this as a degraded take, never as clean.
     */
    fun transcribe(audio: File, language: String): String
}
