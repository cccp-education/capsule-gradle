package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class VttGeneratorTest {

    private val generator = VttGenerator()

    @Test
    fun `format returns VTT`() {
        assertEquals(SubtitleFormat.VTT, generator.format())
    }

    @Test
    fun `generate empty list returns WEBVTT header only`() {
        val vtt = generator.generate(emptyList())
        assertEquals("WEBVTT", vtt.trim())
    }

    @Test
    fun `generate starts with WEBVTT header`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 5.0, text = "Hello")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.trimStart().startsWith("WEBVTT"))
    }

    @Test
    fun `generate single entry produces valid VTT cue`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 5.0, text = "Bonjour")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("00:00:00.000 --> 00:00:05.000"))
        assertTrue(vtt.contains("Bonjour"))
    }

    @Test
    fun `generate multiple entries produces sequential VTT cues`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 3.5, text = "Slide 1"),
            SubtitleEntry(index = 2, startTime = 3.5, endTime = 7.2, text = "Slide 2"),
            SubtitleEntry(index = 3, startTime = 7.2, endTime = 12.0, text = "Slide 3")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("00:00:00.000 --> 00:00:03.500"))
        assertTrue(vtt.contains("00:00:03.500 --> 00:00:07.200"))
        assertTrue(vtt.contains("00:00:07.200 --> 00:00:12.000"))
    }

    @Test
    fun `generate formats milliseconds with dot separator`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 1.123, endTime = 4.567, text = "Test")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("00:00:01.123 --> 00:00:04.567"))
    }

    @Test
    fun `generate handles hours in timestamps`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 3661.5, endTime = 7200.0, text = "Long")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("01:01:01.500 --> 02:00:00.000"))
    }

    @Test
    fun `generate multiline text is preserved`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 5.0, text = "Line 1\nLine 2\nLine 3")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("Line 1"))
        assertTrue(vtt.contains("Line 2"))
        assertTrue(vtt.contains("Line 3"))
    }

    @Test
    fun `generate cues are separated by blank line`() {
        val entries = listOf(
            SubtitleEntry(index = 1, startTime = 0.0, endTime = 2.0, text = "A"),
            SubtitleEntry(index = 2, startTime = 2.0, endTime = 4.0, text = "B")
        )
        val vtt = generator.generate(entries)
        assertTrue(vtt.contains("\n\n"))
    }

    @Test
    fun `generate uses index as cue identifier`() {
        val entries = listOf(
            SubtitleEntry(index = 5, startTime = 0.0, endTime = 2.0, text = "Fifth")
        )
        val vtt = generator.generate(entries)
        val lines = vtt.lines()
        assertTrue(lines.any { it.trim() == "5" })
    }
}
