package capsule

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for [CaptureStrategy] enum (CAP-CR3-3 US-1).
 *
 * `CaptureStrategy` is a type-safe enum with two values:
 * `PLAYWRIGHT` (default, backward compat) and `SCREENSHOT` (alternative).
 * The companion `fromString` is case-insensitive and falls back to
 * `PLAYWRIGHT` for unknown/blank values.
 */
class CaptureStrategyTest {

    @Test
    fun `fromString returns PLAYWRIGHT for null`() {
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString(null))
    }

    @Test
    fun `fromString returns PLAYWRIGHT for blank`() {
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString(""))
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("   "))
    }

    @Test
    fun `fromString returns PLAYWRIGHT for known value`() {
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("playwright"))
    }

    @Test
    fun `fromString returns SCREENSHOT for known value`() {
        assertEquals(CaptureStrategy.SCREENSHOT, CaptureStrategy.fromString("screenshot"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("Playwright"))
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("PLAYWRIGHT"))
        assertEquals(CaptureStrategy.SCREENSHOT, CaptureStrategy.fromString("Screenshot"))
        assertEquals(CaptureStrategy.SCREENSHOT, CaptureStrategy.fromString("SCREENSHOT"))
    }

    @Test
    fun `fromString falls back to PLAYWRIGHT for unknown value`() {
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("manim"))
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.fromString("foobar"))
    }

    @Test
    fun `enum has exactly two values`() {
        assertEquals(2, CaptureStrategy.entries.size)
        assertEquals(CaptureStrategy.PLAYWRIGHT, CaptureStrategy.entries[0])
        assertEquals(CaptureStrategy.SCREENSHOT, CaptureStrategy.entries[1])
    }
}