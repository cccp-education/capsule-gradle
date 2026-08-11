package capsule

import capsule.audio.AudioPostConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [AudioPostConfig] — CAP-AUDIO US-1.
 *
 * The config is a plain data class with 5 fields and backward-compat
 * defaults (all disabled — existing configs without an `audioPost`
 * section keep the no-audio-post behavior).
 */
class AudioPostConfigTest {

    @Test
    fun `AudioPostConfig defaults are backward-compat disabled`() {
        val config = AudioPostConfig()
        assertFalse(config.bgmEnabled, "bgmEnabled should default to false (no BGM by default)")
        assertEquals("", config.bgmFile, "bgmFile should default to empty string")
        assertEquals(-18.0, config.bgmLevel, 0.001, "bgmLevel should default to -18.0 dB")
        assertEquals(-16.0, config.loudnessTarget, 0.001, "loudnessTarget should default to -16.0 LUFS EBU R128")
        assertFalse(config.duckingEnabled, "duckingEnabled should default to false")
    }

    @Test
    fun `AudioPostConfig can enable BGM with explicit values`() {
        val config = AudioPostConfig(
            bgmEnabled = true,
            bgmFile = "/music/loop.mp3",
            bgmLevel = -20.0,
            loudnessTarget = -14.0,
            duckingEnabled = true
        )
        assertTrue(config.bgmEnabled)
        assertEquals("/music/loop.mp3", config.bgmFile)
        assertEquals(-20.0, config.bgmLevel, 0.001)
        assertEquals(-14.0, config.loudnessTarget, 0.001)
        assertTrue(config.duckingEnabled)
    }

    @Test
    fun `AudioPostConfig is a data class with equals by value`() {
        val a = AudioPostConfig(bgmEnabled = true, bgmFile = "x.mp3", bgmLevel = -20.0, loudnessTarget = -14.0, duckingEnabled = true)
        val b = AudioPostConfig(bgmEnabled = true, bgmFile = "x.mp3", bgmLevel = -20.0, loudnessTarget = -14.0, duckingEnabled = true)
        assertEquals(a, b, "data class equals should be by value")
    }

    @Test
    fun `AudioPostConfig copy preserves unmodified fields`() {
        val base = AudioPostConfig(bgmEnabled = true, bgmFile = "x.mp3", bgmLevel = -20.0, loudnessTarget = -14.0, duckingEnabled = true)
        val copy = base.copy(duckingEnabled = false)
        assertTrue(copy.bgmEnabled)
        assertEquals("x.mp3", copy.bgmFile)
        assertEquals(-20.0, copy.bgmLevel, 0.001)
        assertEquals(-14.0, copy.loudnessTarget, 0.001)
        assertFalse(copy.duckingEnabled)
    }
}