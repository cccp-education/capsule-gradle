package capsule

import java.io.File

interface ManimEngine {
    fun render(sceneName: String, scriptPath: File, outputDir: File): File
    fun isAvailable(): Boolean
    fun name(): String
    fun probeDuration(videoFile: File): Double
}

class ManimEngineImpl(
    private val config: ManimConfig = ManimConfig()
) : ManimEngine {

    override fun isAvailable(): Boolean {
        return try {
            val proc = ProcessBuilder(config.executablePath, "--version")
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun name(): String = "manim"

    override fun render(sceneName: String, scriptPath: File, outputDir: File): File {
        if (!isAvailable()) {
            throw ManimException("Manim executable not found at: ${config.executablePath}")
        }

        val qualityFlag = "-q${config.quality}"
        val args = listOf(
            config.executablePath,
            qualityFlag,
            scriptPath.absolutePath,
            sceneName
        )

        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val errorOutput = process.inputStream.bufferedReader().readText()
            throw ManimException("Manim exited with code $exitCode: $errorOutput")
        }

        // Manim outputs under media/ subdirectory relative to script parent
        val mediaDir = scriptPath.parentFile.resolve("media")
            .resolve("videos")
            .resolve(scriptPath.nameWithoutExtension)
            .resolve("${config.quality}p60")
        val mp4File = mediaDir.resolve("$sceneName.mp4")
        if (!mp4File.exists()) {
            throw ManimException("Expected output not found: ${mp4File.absolutePath}")
        }
        return mp4File
    }

    override fun probeDuration(videoFile: File): Double = MediaProbeUtil.probeDuration(videoFile)
}

class NoOpManimEngine : ManimEngine {
    override fun isAvailable(): Boolean = true
    override fun name(): String = "noop"

    override fun render(sceneName: String, scriptPath: File, outputDir: File): File {
        outputDir.mkdirs()
        val placeholderFile = outputDir.resolve("${sceneName}.mp4")
        val placeholder = listOf(
            "# MANIM PLACEHOLDER (noop engine)",
            "# Scene: $sceneName",
            "# Script: ${scriptPath.name}"
        ).joinToString("\n")
        placeholderFile.writeText(placeholder)
        return placeholderFile
    }

    override fun probeDuration(videoFile: File): Double = 0.0
}

class ManimException(message: String) : RuntimeException(message)
