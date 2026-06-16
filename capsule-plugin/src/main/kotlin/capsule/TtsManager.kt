package capsule

import java.io.File

interface TtsEngine {
    fun synthesize(text: String, outputFile: File)
    fun isAvailable(): Boolean
    fun name(): String
    fun language(): Language? = null
}

class PiperTtsEngine(
    private val executablePath: String = "piper",
    private val model: String = "fr_FR-siwis-medium",
    private val language: Language? = null
) : TtsEngine {

    private val resolvedModel: String = language?.let { VoiceMapping.piperModel(it) } ?: model

    override fun isAvailable(): Boolean {
        return try {
            val proc = ProcessBuilder(executablePath, "--help")
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun name(): String = "piper"

    override fun language(): Language? = language ?: VoiceMapping.resolveLanguage(resolvedModel)

    override fun synthesize(text: String, outputFile: File) {
        if (!isAvailable()) {
            throw TtsException("Piper executable not found at: $executablePath")
        }

        val wavFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")

        val args = listOf(
            executablePath,
            "--model", resolvedModel,
            "--output_file", wavFile.absolutePath
        )

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()

        process.outputStream.bufferedWriter().use { writer ->
            writer.write(text)
            writer.flush()
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().readText()
            throw TtsException("Piper exited with code $exitCode: $errorOutput")
        }

        AudioConversionUtil.wavToMp3(wavFile, outputFile)
        wavFile.delete()
    }
}

class NoOpTtsEngine : TtsEngine {
    override fun isAvailable(): Boolean = true
    override fun name(): String = "noop"

    override fun synthesize(text: String, outputFile: File) {
        outputFile.parentFile.mkdirs()
        val placeholder = listOf(
            "# TTS PLACEHOLDER (noop engine)",
            "# Text: ${text.take(100)}..."
        ).joinToString("\n")
        outputFile.writeText(placeholder)
    }
}

class TtsException(message: String) : RuntimeException(message)

class EspeakTtsEngine(
    private val executablePath: String = "espeak",
    private val voice: String = "fr",
    private val speed: Int = 150,
    private val language: Language? = null
) : TtsEngine {

    private val resolvedVoice: String = language?.let { VoiceMapping.espeakVoice(it) } ?: voice

    override fun isAvailable(): Boolean {
        return try {
            val proc = ProcessBuilder(executablePath, "--help")
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun name(): String = "espeak"

    override fun language(): Language? = language ?: VoiceMapping.resolveLanguageFromEspeak(resolvedVoice)

    override fun synthesize(text: String, outputFile: File) {
        if (!isAvailable()) {
            throw TtsException("espeak executable not found at: $executablePath")
        }

        outputFile.parentFile.mkdirs()

        val wavFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")

        val proc = ProcessBuilder(
            executablePath,
            "-v", resolvedVoice,
            "-s", speed.toString(),
            "-w", wavFile.absolutePath,
            text
        ).redirectErrorStream(true).start()

        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            val stderr = proc.inputStream.bufferedReader().readText()
            throw TtsException("espeak exited with code $exitCode: $stderr")
        }

        AudioConversionUtil.wavToMp3(wavFile, outputFile)
        wavFile.delete()
    }
}
