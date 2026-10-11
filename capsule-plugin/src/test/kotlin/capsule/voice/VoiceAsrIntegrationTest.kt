package capsule.voice

import capsule.ProcessRunner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir

/**
 * Real-ASR dogfooding for the voice-quality pipeline (CAP-QC-VOICE US-4).
 *
 * Synthesises a phrase with a real TTS (espeak) and transcribes it with a real
 * ASR (whisper/faster-whisper), then scores the fidelity. Both binaries are
 * external: the test is **skip-guarded** ([assumeTrue]) and never a prerequisite
 * of `check` — the fast/full CI jobs carry neither, so it is skipped there and
 * runs only on a host where the tools are installed.
 *
 * This is the counterpart of `capsule.voice`'s pure tests: they prove the
 * decision, this proves the wiring against real audio.
 */
class VoiceAsrIntegrationTest {

    @field:TempDir
    lateinit var workDir: File

    private fun onPath(binary: String): Boolean =
        System.getenv("PATH")?.split(File.pathSeparator).orEmpty().any { entry ->
            File(entry, binary).let { it.isFile && it.canExecute() }
        }

    private fun ttsBinary(): String? = listOf("espeak-ng", "espeak", "piper").firstOrNull { onPath(it) }

    private fun asrBinary(): String? = listOf("whisper", "faster-whisper").firstOrNull { onPath(it) }

    @Test
    fun `a real synthesised take is transcribed and scores a low word error rate`() {
        val tts = ttsBinary()
        assumeTrue(tts != null) { "no TTS binary on PATH — real ASR dogfooding skipped" }
        val asr = asrBinary()
        assumeTrue(asr != null) { "no ASR binary on PATH — real ASR dogfooding skipped" }

        val phrase = "bonjour le monde"
        val wav = File(workDir, "slide-01.wav")
        val result = ProcessRunner.run(listOf(tts!!, "-v", "fr", "-s", "150", "-w", wav.absolutePath, phrase))
        assumeTrue(result.isSuccess && wav.isFile && wav.length() > 0) { "TTS synthesis failed ($tts)" }

        val transcript = CliSpeechRecognizer(asr!!).transcribe(wav, "fr")
        val score = FidelityScorer.score(phrase, transcript)

        assertTrue(
            score.wordErrorRate <= 0.5,
            "expected a low word error rate for a clean synthetic take, got ${score.wordErrorRate} (transcript='$transcript')",
        )
    }
}
