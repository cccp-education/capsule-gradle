package capsule.a11y

import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * One synthesized description cue to mix into the video
 * (CAP-AUDIODESC US-3).
 *
 * @property file the synthesized audio file (TTS output).
 * @property startSeconds absolute start on the timeline (from the plan).
 */
data class AudioDescriptionInput(
    val file: File,
    val startSeconds: Double,
) {
    init {
        require(startSeconds.isFinite() && startSeconds >= 0.0) {
            "startSeconds must be a finite non-negative value, got $startSeconds"
        }
    }
}

/**
 * Mixing settings for the audio description (CAP-AUDIODESC US-3).
 *
 * @property levelDb description level under the narration (default `-6.0`).
 * @property duckingEnabled lower the description when the narration speaks
 *   (safety net — the planner already places cues in silences). Default `true`.
 * @property duckThreshold sidechain threshold (linear, default `0.1`).
 * @property duckRatio sidechain ratio (default `4`).
 * @property duckAttackMs sidechain attack in milliseconds (default `5`).
 * @property duckReleaseMs sidechain release in milliseconds (default `300`).
 */
data class AudioDescriptionMixSettings(
    val levelDb: Double = -6.0,
    val duckingEnabled: Boolean = true,
    val duckThreshold: Double = 0.1,
    val duckRatio: Int = 4,
    val duckAttackMs: Int = 5,
    val duckReleaseMs: Int = 300,
) {
    init {
        require(levelDb.isFinite()) { "levelDb must be finite, got $levelDb" }
        require(duckThreshold.isFinite() && duckThreshold > 0.0) {
            "duckThreshold must be a positive finite value, got $duckThreshold"
        }
        require(duckRatio >= 1) { "duckRatio must be >= 1, got $duckRatio" }
        require(duckAttackMs >= 0) { "duckAttackMs must be >= 0, got $duckAttackMs" }
        require(duckReleaseMs >= 0) { "duckReleaseMs must be >= 0, got $duckReleaseMs" }
    }
}

/**
 * Pure builder for the FFmpeg filter graph and argv of the audio-description
 * mix (CAP-AUDIODESC US-3).
 *
 * The narration track is already in the video; the description cues are
 * separate inputs, delayed to their planned start (`adelay`), mixed together,
 * then ducked under the narration (`sidechaincompress`). The final mix is
 * **split** so the same graph feeds both the video and the separate audio track
 * (a filter output pad is single-use).
 *
 * Pure — no I/O, no process.
 */
object AudioDescriptionMixCommand {
    /**
     * Builds the `filter_complex` graph.
     *
     * Input `0` is the video (narration audio); inputs `1..n` are the synthesized
     * description cues.
     */
    fun buildFilterGraph(
        inputs: List<AudioDescriptionInput>,
        settings: AudioDescriptionMixSettings,
    ): String {
        require(inputs.isNotEmpty()) { "at least one description input is required" }

        val descriptions =
            inputs
                .mapIndexed { index, input ->
                    val delayMs = (input.startSeconds * 1000.0).roundToInt()
                    "[${index + 1}:a]volume=${fmt(settings.levelDb)}dB,adelay=$delayMs|$delayMs[d$index]"
                }.toMutableList()

        val descLabel =
            if (inputs.size == 1) {
                descriptions[0] = descriptions[0].removeSuffix("[d0]") + "[desc]"
                "[desc]"
            } else {
                val labels = inputs.indices.joinToString("") { "[d$it]" }
                descriptions.add("$labels" + "amix=inputs=${inputs.size}:duration=longest:dropout_transition=0[desc]")
                "[desc]"
            }

        val tails =
            if (settings.duckingEnabled) {
                val ducking =
                    "sidechaincompress=threshold=${fmt(settings.duckThreshold)}:ratio=${settings.duckRatio}" +
                        ":attack=${settings.duckAttackMs}:release=${settings.duckReleaseMs}"
                listOf(
                    "[0:a]asplit=2[voice][key]",
                    "[desc][key]$ducking[ducked]",
                    "[voice][ducked]amix=inputs=2:duration=first:dropout_transition=0,asplit=2[aout][asep]",
                )
            } else {
                listOf(
                    "[0:a]$descLabel" + "amix=inputs=2:duration=first:dropout_transition=0,asplit=2[aout][asep]",
                )
            }

        return (descriptions + tails).joinToString(";")
    }

    /**
     * Builds the full FFmpeg argv: one invocation writes the mixed video and the
     * separate audio track (`[asep]`).
     *
     * @param audioCodec codec of the mixed video's audio (container-driven:
     *   Vorbis for WebM, AAC for MP4).
     */
    fun buildArgv(
        videoFile: File,
        inputs: List<AudioDescriptionInput>,
        outputVideo: File,
        separateAudio: File,
        settings: AudioDescriptionMixSettings,
        audioCodec: String,
        ffmpegPath: String,
    ): List<String> {
        require(inputs.isNotEmpty()) { "at least one description input is required" }
        val argv = mutableListOf(ffmpegPath, "-y", "-i", videoFile.absolutePath)
        inputs.forEach { input ->
            argv.add("-i")
            argv.add(input.file.absolutePath)
        }
        argv.add("-filter_complex")
        argv.add(buildFilterGraph(inputs, settings))
        argv.addAll(listOf("-map", "0:v", "-map", "[aout]", "-c:v", "copy", "-c:a", audioCodec, "-shortest"))
        argv.add(outputVideo.absolutePath)
        argv.addAll(listOf("-map", "[asep]", "-vn", "-c:a", "aac"))
        argv.add(separateAudio.absolutePath)
        return argv
    }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.2f", value)
}
