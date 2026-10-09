package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [ViralConfig] and [ViralPlatform] — CAP-VIRAL US-3.
 *
 * The config is a plain data class with backward-compat defaults (disabled
 * — existing configs without a `viral` section keep the pedagogical-only
 * behavior). The short-form duration is coerced into [15, 60] s by
 * [ViralConfig.effectiveDurationSeconds].
 */
class ViralConfigTest {

    @Test
    fun `ViralConfig defaults are backward-compat disabled`() {
        val config = ViralConfig()
        assertFalse(config.enabled, "enabled should default to false (viral is opt-in)")
        assertEquals(ViralPlatform.TIKTOK, config.platform, "platform should default to TIKTOK")
        assertEquals(30, config.targetDurationSeconds, "target duration should default to 30 s")
        assertTrue(config.storyboardRequired, "storyboard gate should default to required")
        assertEquals("", config.hook, "hook should default to empty")
        assertEquals("", config.cta, "cta should default to empty")
        assertEquals("", config.storyboardFile, "storyboardFile should default to empty")
    }

    @Test
    fun `effectiveDurationSeconds clamps below the short-form floor`() {
        assertEquals(15, ViralConfig(targetDurationSeconds = 5).effectiveDurationSeconds)
        assertEquals(15, ViralConfig(targetDurationSeconds = 14).effectiveDurationSeconds)
    }

    @Test
    fun `effectiveDurationSeconds clamps above the short-form ceiling`() {
        assertEquals(60, ViralConfig(targetDurationSeconds = 120).effectiveDurationSeconds)
        assertEquals(60, ViralConfig(targetDurationSeconds = 61).effectiveDurationSeconds)
    }

    @Test
    fun `effectiveDurationSeconds keeps values inside the range`() {
        assertEquals(20, ViralConfig(targetDurationSeconds = 20).effectiveDurationSeconds)
        assertEquals(45, ViralConfig(targetDurationSeconds = 45).effectiveDurationSeconds)
        assertEquals(60, ViralConfig(targetDurationSeconds = 60).effectiveDurationSeconds)
    }

    @Test
    fun `ViralConfig can be fully enabled with explicit fields`() {
        val config = ViralConfig(
            enabled = true,
            platform = ViralPlatform.YOUTUBE_SHORT,
            targetDurationSeconds = 45,
            hook = "Et si votre formation ne durait que 30 secondes ?",
            cta = "Découvrez FPA sur talaria.school",
            storyboardRequired = true,
            storyboardFile = "storyboard.adoc"
        )
        assertTrue(config.enabled)
        assertEquals(ViralPlatform.YOUTUBE_SHORT, config.platform)
        assertEquals(45, config.effectiveDurationSeconds)
        assertEquals("Et si votre formation ne durait que 30 secondes ?", config.hook)
        assertEquals("Découvrez FPA sur talaria.school", config.cta)
        assertEquals("storyboard.adoc", config.storyboardFile)
    }

    @Test
    fun `ViralConfig is a data class with equals by value`() {
        val a = ViralConfig(enabled = true, platform = ViralPlatform.REELS, targetDurationSeconds = 40)
        val b = ViralConfig(enabled = true, platform = ViralPlatform.REELS, targetDurationSeconds = 40)
        assertEquals(a, b, "data class equals should be by value")
    }

    @Test
    fun `ViralConfig copy preserves unmodified fields`() {
        val base = ViralConfig(enabled = true, platform = ViralPlatform.REELS, targetDurationSeconds = 40)
        val copy = base.copy(enabled = false)
        assertFalse(copy.enabled)
        assertEquals(ViralPlatform.REELS, copy.platform, "platform preserved on copy")
        assertEquals(40, copy.targetDurationSeconds, "duration preserved on copy")
    }
}

/**
 * TDD unit tests for [ViralPlatform.fromString] — CAP-VIRAL US-3.
 */
class ViralPlatformTest {

    @Test
    fun `fromString parses known platforms case-insensitively`() {
        assertEquals(ViralPlatform.YOUTUBE_SHORT, ViralPlatform.fromString("youtube_short"))
        assertEquals(ViralPlatform.YOUTUBE_SHORT, ViralPlatform.fromString("YOUTUBE_SHORT"))
        assertEquals(ViralPlatform.TIKTOK, ViralPlatform.fromString("tiktok"))
        assertEquals(ViralPlatform.REELS, ViralPlatform.fromString("Reels"))
    }

    @Test
    fun `fromString falls back to TIKTOK for null blank or unknown`() {
        assertEquals(ViralPlatform.TIKTOK, ViralPlatform.fromString(null))
        assertEquals(ViralPlatform.TIKTOK, ViralPlatform.fromString(""))
        assertEquals(ViralPlatform.TIKTOK, ViralPlatform.fromString("   "))
        assertEquals(ViralPlatform.TIKTOK, ViralPlatform.fromString("myspace"))
    }
}
