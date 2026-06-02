package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD unit tests for ManimConfig enrichment.
 *
 * EPIC CAP-19: ManimEngine configurable.
 * Tests written BEFORE production code changes (baby-step TDD).
 */
class ManimConfigTest {

    // ─── Default values ─────────────────────────────────────────────

    @Test
    fun `ManimConfig default values are correct`() {
        val config = ManimConfig()
        assertEquals("manim", config.executablePath)
        assertEquals("l", config.quality)
        assertEquals("src/manim", config.scriptsDir)
        assertEquals("build/capsule/manim", config.outputDir)
    }

    // ─── VALID_QUALITIES companion ──────────────────────────────────

    @Test
    fun `ManimConfig VALID_QUALITIES contains all 5 presets`() {
        assertEquals(5, ManimConfig.VALID_QUALITIES.size)
        assertTrue(ManimConfig.VALID_QUALITIES.contains("l"))
        assertTrue(ManimConfig.VALID_QUALITIES.contains("m"))
        assertTrue(ManimConfig.VALID_QUALITIES.contains("h"))
        assertTrue(ManimConfig.VALID_QUALITIES.contains("p"))
        assertTrue(ManimConfig.VALID_QUALITIES.contains("k"))
    }

    // ─── validate() ────────────────────────────────────────────────

    @Test
    fun `ManimConfig validate returns empty for valid quality l`() {
        val errors = ManimConfig().validate()
        assertTrue(errors.isEmpty(), "Default ManimConfig should be valid, got: $errors")
    }

    @Test
    fun `ManimConfig validate returns empty for each valid quality`() {
        for (quality in ManimConfig.VALID_QUALITIES) {
            val config = ManimConfig(quality = quality)
            val errors = config.validate()
            assertTrue(errors.isEmpty(), "Quality '$quality' should be valid, got errors: $errors")
        }
    }

    @Test
    fun `ManimConfig validate allows noop executablePath`() {
        val config = ManimConfig(executablePath = "noop")
        val errors = config.validate()
        assertTrue(errors.isEmpty(), "noop executablePath should be valid, got: $errors")
    }

    @Test
    fun `ManimConfig validate returns error for invalid quality`() {
        val config = ManimConfig(quality = "x")
        val errors = config.validate()
        assertTrue(errors.isNotEmpty(), "Invalid quality 'x' should produce errors")
        assertTrue(errors.any { it.contains("quality") }, "Error should mention 'quality', got: $errors")
    }

    @Test
    fun `ManimConfig validate returns error for blank quality`() {
        val config = ManimConfig(quality = "")
        val errors = config.validate()
        assertTrue(errors.isNotEmpty(), "Blank quality should produce validation error")
    }

    @Test
    fun `ManimConfig validate returns error for blank executablePath when not noop`() {
        val config = ManimConfig(executablePath = "")
        val errors = config.validate()
        assertTrue(errors.isNotEmpty(), "Blank executablePath should produce validation error")
        assertTrue(errors.any { it.contains("executablePath") }, "Error should mention 'executablePath', got: $errors")
    }

    @Test
    fun `ManimConfig validate does not error on blank executablePath when noop`() {
        val config = ManimConfig(executablePath = "noop")
        val errors = config.validate()
        assertTrue(errors.isEmpty(), "noop executablePath with default quality should be valid, got: $errors")
    }

    @Test
    fun `ManimConfig validate returns error for blank scriptsDir`() {
        val config = ManimConfig(scriptsDir = "")
        val errors = config.validate()
        assertTrue(errors.isNotEmpty(), "Blank scriptsDir should produce validation error")
        assertTrue(errors.any { it.contains("scriptsDir") }, "Error should mention 'scriptsDir', got: $errors")
    }

    @Test
    fun `ManimConfig validate returns multiple errors when multiple fields are invalid`() {
        val config = ManimConfig(executablePath = "", quality = "invalid", scriptsDir = "")
        val errors = config.validate()
        assertTrue(errors.size >= 3, "Should have at least 3 validation errors, got ${errors.size}: $errors")
    }

    // ─── Data class copy ────────────────────────────────────────────

    @Test
    fun `ManimConfig can be created with custom values via data class copy`() {
        val config = ManimConfig().copy(
            executablePath = "/usr/local/bin/manim",
            quality = "h",
            scriptsDir = "custom/manim",
            outputDir = "custom/output"
        )
        assertEquals("/usr/local/bin/manim", config.executablePath)
        assertEquals("h", config.quality)
        assertEquals("custom/manim", config.scriptsDir)
        assertEquals("custom/output", config.outputDir)
    }

    // ─── parallelRender ──────────────────────────────────────────────

    @Test
    fun `ManimConfig default parallelRender is false`() {
        val config = ManimConfig()
        assertEquals(false, config.parallelRender, "Default parallelRender should be false")
    }

    @Test
    fun `ManimConfig can set parallelRender to true`() {
        val config = ManimConfig(parallelRender = true)
        assertEquals(true, config.parallelRender, "parallelRender should be true when explicitly set")
    }

    @Test
    fun `ManimConfig validate allows parallelRender true`() {
        val config = ManimConfig(parallelRender = true)
        val errors = config.validate()
        assertTrue(errors.isEmpty(), "parallelRender=true should be valid, got: $errors")
    }

    // ─── Quality preset documentation ───────────────────────────────

    @Test
    fun `ManimConfig quality presets match Manim convention`() {
        // l = 480p60, m = 720p60, h = 1080p60, p = 1440p60, k = 2160p60
        // This test documents the convention — Manim uses -ql, -qm, -qh, -qp, -qk
        assertEquals(setOf("l", "m", "h", "p", "k"), ManimConfig.VALID_QUALITIES)
    }
}