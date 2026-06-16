package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SrtGeneratorTest {

    private val generator = SrtGenerator()

    @Test
    fun `format returns SRT`() {
        assertEquals(SubtitleFormat.SRT, generator.format())
    }

    @Test
    fun `generate empty list returns empty string`() {
        assertEquals("", generator.generate(emptyList()))
    }

    @Test
    fun `generate single entry produces valid SRT block`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 5.0, text = "Bonjour")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.contains("1"))
        assertTrue(srt.contains("00:00:00,000 --> 00:00:05,000"))
        assertTrue(srt.contains("Bonjour"))
    }

    @Test
    fun `generate multiple entries produces sequential SRT blocks`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 3.5, text = "Slide 1"),
            SubtitleEntry(index = 2, startTime = 3.5, endTime = 7.2, text = "Slide 2"),
            SubtitleEntry(index = 3, startTime = 7.2, endTime = 12.0, text = "Slide 3")
        )
        val srt = generator.generate(entries)
        val lines = srt.lines()
        assertTrue(lines.any { it.trim() == "1" })
        assertTrue(lines.any { it.trim() == "2" })
        assertTrue(lines.any { it.trim() == "3" })
        assertTrue(lines.any { it.contains("00:00:00,000 --> 00:00:03,500") })
        assertTrue(lines.any { it.contains("00:00:03,500 --> 00:00:07,200") })
        assertTrue(lines.any { it.contains("00:00:07,200 --> 00:00:12,000") })
    }

    @Test
    fun `generate formats milliseconds correctly`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 1.123, endTime = 4.567, text = "Test")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.contains("00:00:01,123 --> 00:00:04,567"))
    }

    @Test
    fun `generate handles hours in timestamps`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 3661.5, endTime = 7200.0, text = "Long")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.contains("01:01:01,500 --> 02:00:00,000"))
    }

    @Test
    fun `generate multiline text is preserved`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 5.0, text = "Line 1\nLine 2\nLine 3")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.contains("Line 1"))
        assertTrue(srt.contains("Line 2"))
        assertTrue(srt.contains("Line 3"))
    }

    @Test
    fun `generate blocks are separated by blank line`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 2.0, text = "A"),
            SubtitleEntry(index = 2, startTime = 2.0, endTime = 4.0, text = "B")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.contains("\n\n"))
    }

    @Test
    fun `generate index matches SubtitleEntry index not list position`() {
        val entries = listOf(
            SubtitleEntry(index = 5, startTime = 0.0, endTime = 2.0, text = "Fifth")
        )
        val srt = generator.generate(entries)
        assertTrue(srt.trimStart().startsWith("5"))
    }
}
