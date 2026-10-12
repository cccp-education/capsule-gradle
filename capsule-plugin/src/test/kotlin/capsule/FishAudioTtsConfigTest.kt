package capsule

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * CAP-FISH-AUDIO US-3 — the non-secret Fish parameters live in the `tts`
 * section and follow the standard 4-source precedence
 * (ENV < gradle.properties < YAML < CLI).
 */
class FishAudioTtsConfigTest {

    @field:TempDir
    lateinit var dir: File

    private fun merge(yaml: CapsuleConfig, cli: Map<String, Any?> = emptyMap()): CapsuleConfig =
        CapsuleConfigMerger.merge(dir, yaml, cli, yamlLoaded = true)

    @Test
    fun `defaults mirror the prepa reference`() {
        val tts = TtsConfig()
        assertEquals("", tts.fishVoice)
        assertEquals("speech-1.6", tts.fishModel)
        assertEquals("wav", tts.fishFormat)
        assertEquals(0.85, tts.fishTemperature)
        assertEquals(0.88, tts.fishTopP)
        assertEquals("https://api.fish.audio/v1/tts", tts.fishEndpoint)
    }

    @Test
    fun `the yaml tts section carries the fish parameters`() {
        val yaml = CapsuleConfig(tts = TtsConfig(fishVoice = "voice-xyz", fishModel = "speech-1.6"))
        val merged = merge(yaml)
        assertEquals("voice-xyz", merged.tts.fishVoice)
        assertEquals("speech-1.6", merged.tts.fishModel)
    }

    @Test
    fun `cli overrides the yaml for the fish parameters`() {
        val yaml = CapsuleConfig(tts = TtsConfig(fishVoice = "from-yaml"))
        val merged = merge(
            yaml,
            cli = mapOf("tts.fishVoice" to "from-cli", "tts.fishModel" to "speech-1.6-turbo"),
        )
        assertEquals("from-cli", merged.tts.fishVoice)
        assertEquals("speech-1.6-turbo", merged.tts.fishModel)
    }
}
