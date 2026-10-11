package capsule.voice

import capsule.ProcessRunner
import java.io.File
import java.nio.file.Files

/**
 * CLI adapter of the [SpeechRecognizer] port (CAP-QC-VOICE US-2).
 *
 * Runs a `whisper` / `faster-whisper` executable through [ProcessRunner] (the
 * single process entry point — anti-deadlock, bounded wait) and reads back the
 * plain-text transcript it wrote. The binary is **never** a dependency: a
 * missing executable is detected by [isAvailable] and the task degrades, and a
 * run-time failure raises [SpeechRecognizerException] so the take is marked
 * degraded rather than read as clean.
 *
 * The argv is built by the pure [SpeechRecognizerCommand]; this class only owns
 * the process and the transcript file lookup.
 */
class CliSpeechRecognizer(
    private val executablePath: String = "whisper",
    private val model: String = "",
    private val workingDir: File? = null,
) : SpeechRecognizer {

    override fun transcribe(audio: File, language: String): String {
        val outputDir = Files.createTempDirectory("capsule-whisper").toFile()
        try {
            val args = SpeechRecognizerCommand.args(executablePath, audio, model, language, outputDir)
            val result = ProcessRunner.run(args, workingDir = workingDir)
            if (!result.isSuccess) {
                throw SpeechRecognizerException(
                    "ASR engine '$executablePath' exited with code ${result.exitCode}: ${result.tail()}"
                )
            }
            return readTranscript(outputDir, audio)
        } finally {
            outputDir.deleteRecursively()
        }
    }

    /** Reads `<audio>.txt`, else any `.txt` the CLI wrote, else an empty string. */
    private fun readTranscript(outputDir: File, audio: File): String {
        val expected = File(outputDir, audio.nameWithoutExtension + ".txt")
        if (expected.isFile) return expected.readText()
        return outputDir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
            ?.firstOrNull()
            ?.readText()
            .orEmpty()
    }

    companion object {
        /**
         * `true` when the recogniser binary responds to `--help`. A blank path
         * or a missing executable returns `false` (the task then degrades).
         */
        fun isAvailable(executablePath: String): Boolean {
            if (executablePath.isBlank()) return false
            return ProcessRunner.probe(executablePath, "--help")
        }
    }
}

/** Raised when the CLI ASR could not produce a transcript. */
class SpeechRecognizerException(message: String) : RuntimeException(message)
