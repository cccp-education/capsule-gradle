package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * CAP-FISH-AUDIO US-1 — [FishAudioKey] is a secret holder: its value must never
 * leak through a `toString()`/log/serialisation. The redaction is the whole point
 * of the type, so it is pinned here.
 */
class FishAudioKeyTest {

    @Test
    fun `toString redacts the key`() {
        val rendered = FishAudioKey("sk-fish-secret").toString()
        assertFalse(rendered.contains("sk-fish-secret"), "raw key must not leak: $rendered")
        assertEquals("FishAudioKey(key=***)", rendered)
    }

    @Test
    fun `blank key is rejected`() {
        assertFailsWith<IllegalArgumentException> { FishAudioKey("   ") }
    }

    @Test
    fun `equality is on the key value`() {
        assertEquals(FishAudioKey("a"), FishAudioKey("a"))
    }

    @Test
    fun `a data-class rendering of CapsuleConfig does not leak the key`() {
        val config = CapsuleConfig(fishAudio = listOf(FishAudioKey("sk-fish-secret")))
        assertFalse(config.toString().contains("sk-fish-secret"), "config.toString must be redacted")
    }

    @Test
    fun `the structured config log does not leak the key`() {
        val config = CapsuleConfig(fishAudio = listOf(FishAudioKey("sk-fish-secret")))
        val log = CapsuleConfigLogger.formatConfigLog(config).joinToString("\n")
        assertFalse(log.contains("sk-fish-secret"), "config log must not leak the key")
    }
}
