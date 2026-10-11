package capsule.voice

import java.io.File

/**
 * Pure command builder for the CLI ASR adapter (CAP-QC-VOICE US-2).
 *
 * Ported shape from `prepa_electricite_complet/verif_voix.py`: the recogniser is
 * invoked per file, told the language, and asked for a plain-text transcript
 * written into a directory the plugin owns. Blank [model] and [language] are
 * omitted rather than passed empty, so a model-less or language-auto run stays
 * valid.
 *
 * Pattern: [capsule.SubtitleBurnInCommand] / `capsule.audio.AudioPostCommand`.
 * Pure — no I/O.
 */
object SpeechRecognizerCommand {

    /**
     * Builds the argv that transcribes [audio] into [outputDir].
     *
     * @param executablePath the recogniser binary (`whisper`, `faster-whisper`…).
     * @param audio          the audio file to transcribe.
     * @param model          the model name, blank to use the CLI default.
     * @param language       the language hint, blank to let the CLI auto-detect.
     * @param outputDir      directory the CLI writes `<audio>.txt` into.
     */
    fun args(
        executablePath: String,
        audio: File,
        model: String,
        language: String,
        outputDir: File,
    ): List<String> {
        val args = mutableListOf(executablePath, audio.absolutePath)
        if (model.isNotBlank()) {
            args += "--model"
            args += model
        }
        if (language.isNotBlank()) {
            args += "--language"
            args += language
        }
        args += "--output_format"
        args += "txt"
        args += "--output_dir"
        args += outputDir.absolutePath
        return args
    }
}
