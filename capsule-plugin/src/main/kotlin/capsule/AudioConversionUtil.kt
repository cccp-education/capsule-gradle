package capsule

import java.io.File

object AudioConversionUtil {

    fun wavToMp3(wavFile: File, mp3File: File, ffmpegPath: String = "ffmpeg") {
        mp3File.parentFile.mkdirs()
        try {
            val proc = ProcessBuilder(
                ffmpegPath, "-y",
                "-i", wavFile.absolutePath,
                "-codec:a", "libmp3lame",
                "-qscale:a", "2",
                mp3File.absolutePath
            ).redirectErrorStream(true).start()

            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                wavFile.copyTo(mp3File, overwrite = true)
            }
        } catch (e: Exception) {
            wavFile.copyTo(mp3File, overwrite = true)
        }
    }

    fun isAvailable(ffmpegPath: String = "ffmpeg"): Boolean {
        return try {
            val proc = ProcessBuilder(ffmpegPath, "-version")
                .redirectErrorStream(true)
                .start()
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
}
