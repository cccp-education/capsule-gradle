package capsule.adapter.kernel

import capsule.SubtitleBurnInService
import capsule.TtsEngine
import capsule.audio.AudioPostConfig
import capsule.audio.AudioPostProcessor
import java.io.File
import video.kernel.AudioPostProduction
import video.kernel.AudioPostSettings
import video.kernel.SubtitleBurnIn
import video.kernel.TextToSpeech

/**
 * Capsule-side implementations of the shared N1 video **ports** (EPIC VK-4
 * US-4b→d), so capsule exposes its TTS / audio-post / subtitle-burn-in
 * capabilities through the engine-neutral contract consumed by a generic
 * orchestrator (parity with hyperframes, the second engine).
 *
 * Adapters, not rewrites: capsule's historical interfaces ([TtsEngine],
 * [AudioPostProcessor], [SubtitleBurnInService]) keep their signatures and
 * behaviour; each adapter only **translates** to the kernel contract —
 * degrading to `false` on failure (the kernel port never throws), mapping the
 * neutral [AudioPostSettings] to the capsule config.
 */
class CapsuleTextToSpeech(private val engine: TtsEngine) : TextToSpeech {

    override fun synthesize(text: String, output: File): Boolean =
        runCatching {
            engine.synthesize(text, output)
            output.exists() && output.length() > 0L
        }.getOrDefault(false)
}

class CapsuleAudioPostProduction(private val processor: AudioPostProcessor) : AudioPostProduction {

    override fun process(video: File, output: File, settings: AudioPostSettings): Boolean =
        processor.process(video, output, settings.toCapsuleConfig())
}

class CapsuleSubtitleBurnIn(private val service: SubtitleBurnInService) : SubtitleBurnIn {

    override fun burnIn(video: File, subtitles: File, output: File): Boolean =
        runCatching {
            service.burnIn(video, subtitles, output)
            output.exists()
        }.getOrDefault(false)
}

/**
 * Maps the neutral kernel settings onto capsule's historical config. Field
 * names differ (`bgmLevelDb` vs `bgmLevel`, …); the semantics are identical.
 */
internal fun AudioPostSettings.toCapsuleConfig(): AudioPostConfig = AudioPostConfig(
    bgmEnabled = bgmEnabled,
    bgmFile = bgmFile,
    bgmLevel = bgmLevelDb,
    loudnessTarget = loudnessTargetLufs,
    duckingEnabled = duckingEnabled,
    twoPassLoudnessEnabled = twoPassLoudness,
)
