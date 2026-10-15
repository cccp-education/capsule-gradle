package capsule.adapter.kernel

import capsule.SubtitleBurnInService
import capsule.TtsEngine
import capsule.audio.AudioPostConfig
import capsule.audio.AudioPostProcessor
import contracts.i18n.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Capsule adapters over the shared N1 video ports (VK-4 US-4b→d).
 *
 * The adapters translate capsule's historical interfaces to the engine-neutral
 * contract: `false` on degradation (never throw), and a faithful mapping of the
 * neutral audio-post settings onto capsule's config.
 */
class CapsuleKernelPortsTest {

    @TempDir
    lateinit var tmp: Path

    @Test
    fun `text-to-speech returns true when the engine produces a non-empty file`() {
        val output = tmp.resolve("narration.mp3").toFile()
        val engine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                outputFile.writeText("audio:$text")
            }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "fake"
            override fun language(): SupportedLanguage? = null
        }

        assertThat(CapsuleTextToSpeech(engine).synthesize("hello", output)).isTrue()
        assertThat(output.readText()).isEqualTo("audio:hello")
    }

    @Test
    fun `text-to-speech degrades to false when the engine throws`() {
        val engine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) = error("no piper")
            override fun isAvailable(): Boolean = false
            override fun name(): String = "fake"
        }

        assertThat(CapsuleTextToSpeech(engine).synthesize("hello", tmp.resolve("x.mp3").toFile())).isFalse()
    }

    @Test
    fun `text-to-speech degrades to false when the engine produces nothing`() {
        val engine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) { /* no-op */ }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "fake"
        }

        assertThat(CapsuleTextToSpeech(engine).synthesize("hello", tmp.resolve("x.mp3").toFile())).isFalse()
    }

    @Test
    fun `audio-post maps the neutral settings onto the capsule config`() {
        var captured: AudioPostConfig? = null
        val processor = object : AudioPostProcessor {
            override fun process(videoFile: File, outputFile: File, config: AudioPostConfig): Boolean {
                captured = config
                return true
            }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "fake"
        }
        val settings = video.kernel.AudioPostSettings(
            bgmEnabled = true,
            bgmFile = "music.mp3",
            bgmLevelDb = -12.0,
            loudnessTargetLufs = -14.0,
            duckingEnabled = true,
            twoPassLoudness = true,
        )

        val result = CapsuleAudioPostProduction(processor).process(
            tmp.resolve("v.webm").toFile(),
            tmp.resolve("o.webm").toFile(),
            settings,
        )

        assertThat(result).isTrue()
        assertThat(captured).isEqualTo(
            AudioPostConfig(
                bgmEnabled = true,
                bgmFile = "music.mp3",
                bgmLevel = -12.0,
                loudnessTarget = -14.0,
                duckingEnabled = true,
                twoPassLoudnessEnabled = true,
            )
        )
    }

    @Test
    fun `subtitle-burn-in returns true when the service produces the output`() {
        val output = tmp.resolve("subbed.webm").toFile()
        val service = object : SubtitleBurnInService {
            override fun burnIn(videoFile: File, subtitleFile: File, outputFile: File): File {
                outputFile.writeText("subbed")
                return outputFile
            }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "fake"
        }

        assertThat(CapsuleSubtitleBurnIn(service).burnIn(tmp.resolve("v.webm").toFile(), tmp.resolve("s.vtt").toFile(), output)).isTrue()
    }

    @Test
    fun `subtitle-burn-in degrades to false when the service throws`() {
        val service = object : SubtitleBurnInService {
            override fun burnIn(videoFile: File, subtitleFile: File, outputFile: File): File = error("no ffmpeg")
            override fun isAvailable(): Boolean = false
            override fun name(): String = "fake"
        }

        assertThat(CapsuleSubtitleBurnIn(service).burnIn(tmp.resolve("v.webm").toFile(), tmp.resolve("s.vtt").toFile(), tmp.resolve("o.webm").toFile())).isFalse()
    }
}
