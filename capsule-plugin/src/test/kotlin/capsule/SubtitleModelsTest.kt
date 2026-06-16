package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SubtitleModelsTest {

    @Test
    fun `SubtitleFormat enum has SRT and VTT values`() {
        assertEquals(2, SubtitleFormat.entries.size)
        assertEquals("srt", SubtitleFormat.SRT.name.lowercase())
        assertEquals("vtt", SubtitleFormat.VTT.name.lowercase())
    }

    @Test
    fun `SubtitleFormat fromString resolves srt`() {
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromString("srt"))
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromString("SRT"))
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromString("Srt"))
    }

    @Test
    fun `SubtitleFormat fromString resolves vtt`() {
        assertEquals(SubtitleFormat.VTT, SubtitleFormat.fromString("vtt"))
        assertEquals(SubtitleFormat.VTT, SubtitleFormat.fromString("VTT"))
        assertEquals(SubtitleFormat.VTT, SubtitleFormat.fromString("Vtt"))
    }

    @Test
    fun `SubtitleFormat fromString defaults to SRT for unknown`() {
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromString("unknown"))
        assertEquals(SubtitleFormat.SRT, SubtitleFormat.fromString(""))
    }

    @Test
    fun `SubtitleFormat fileExtension returns correct extensions`() {
        assertEquals(".srt", SubtitleFormat.SRT.fileExtension)
        assertEquals(".vtt", SubtitleFormat.VTT.fileExtension)
    }

    @Test
    fun `SubtitleEntry holds index startTime endTime and text`() {
        val entry = SubtitleEntry(
            index = 1,
            startTime = 0.0,
            endTime = 5.0,
            text = "Bonjour le monde"
        )
        assertEquals(1, entry.index)
        assertEquals(0.0, entry.startTime)
        assertEquals(5.0, entry.endTime)
        assertEquals("Bonjour le monde", entry.text)
    }

    @Test
    fun `SubtitleEntry startTime must be less than endTime`() {
        assertThrows(IllegalArgumentException::class.java) {
            SubtitleEntry(index = 1, startTime = 5.0, endTime = 3.0, text = "invalid")
        }
    }

    @Test
    fun `SubtitleEntry startTime can equal endTime for zero-duration entries`() {
        val entry = SubtitleEntry(index = 1, startTime = 0.0, endTime = 0.0, text = "")
        assertEquals(0.0, entry.startTime)
        assertEquals(0.0, entry.endTime)
    }

    @Test
    fun `SubtitleEntry text can be empty`() {
        val entry = SubtitleEntry(index = 1, startTime = 0.0, endTime = 1.0, text = "")
        assertEquals("", entry.text)
    }

    @Test
    fun `SubtitleGenerator interface defines generate and format`() {
        val generator = object : SubtitleGenerator {
            override fun generate(entries: List<SubtitleEntry>): String = "mock"
            override fun format(): SubtitleFormat = SubtitleFormat.SRT
        }
        assertEquals("mock", generator.generate(emptyList()))
        assertEquals(SubtitleFormat.SRT, generator.format())
    }
}
