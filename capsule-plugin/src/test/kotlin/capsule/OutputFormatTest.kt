package capsule

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for [OutputFormat] enum (CAP-MP4 US-1).
 *
 * `OutputFormat` is a type-safe enum with three values:
 * `WEBM` (default, backward compat), `MP4` (H.264 transcode), and `BOTH`
 * (keep WebM and produce MP4 alongside). The companion `fromString` is
 * case-insensitive and falls back to `WEBM` for unknown/blank values.
 */
class OutputFormatTest {

    @Test
    fun `fromString returns WEBM for null`() {
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString(null))
    }

    @Test
    fun `fromString returns WEBM for blank`() {
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString(""))
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("   "))
    }

    @Test
    fun `fromString returns WEBM for known value`() {
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("webm"))
    }

    @Test
    fun `fromString returns MP4 for known value`() {
        assertEquals(OutputFormat.MP4, OutputFormat.fromString("mp4"))
    }

    @Test
    fun `fromString returns BOTH for known value`() {
        assertEquals(OutputFormat.BOTH, OutputFormat.fromString("both"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("Webm"))
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("WEBM"))
        assertEquals(OutputFormat.MP4, OutputFormat.fromString("Mp4"))
        assertEquals(OutputFormat.MP4, OutputFormat.fromString("MP4"))
        assertEquals(OutputFormat.BOTH, OutputFormat.fromString("Both"))
        assertEquals(OutputFormat.BOTH, OutputFormat.fromString("BOTH"))
    }

    @Test
    fun `fromString falls back to WEBM for unknown value`() {
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("avi"))
        assertEquals(OutputFormat.WEBM, OutputFormat.fromString("foobar"))
    }

    @Test
    fun `enum has exactly three values`() {
        assertEquals(3, OutputFormat.entries.size)
        assertEquals(OutputFormat.WEBM, OutputFormat.entries[0])
        assertEquals(OutputFormat.MP4, OutputFormat.entries[1])
        assertEquals(OutputFormat.BOTH, OutputFormat.entries[2])
    }
}