package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SubtitleTimingCalculatorTest {

    @Test
    fun `calculate empty slides returns empty list`() {
        val result = SubtitleTimingCalculator.calculate(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `calculate single slide starts at zero`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "Intro", speakerNote = "Hello")
        )
        val durations = listOf(5.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(1, result.size)
        assertEquals(0, result[0].index)
        assertEquals(0.0, result[0].startTime)
        assertEquals(5.0, result[0].endTime)
        assertEquals("Hello", result[0].text)
    }

    @Test
    fun `calculate two slides accumulate start times`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "Slide 0", speakerNote = "First"),
            SlideSegment(index = 1, title = "Slide 1", speakerNote = "Second")
        )
        val durations = listOf(3.0, 4.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(2, result.size)
        assertEquals(0.0, result[0].startTime)
        assertEquals(3.0, result[0].endTime)
        assertEquals(3.0, result[1].startTime)
        assertEquals(7.0, result[1].endTime)
    }

    @Test
    fun `calculate three slides with varying durations`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "A", speakerNote = "a"),
            SlideSegment(index = 1, title = "B", speakerNote = "b"),
            SlideSegment(index = 2, title = "C", speakerNote = "c")
        )
        val durations = listOf(2.5, 6.0, 3.5)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(3, result.size)
        assertEquals(0.0, result[0].startTime)
        assertEquals(2.5, result[0].endTime)
        assertEquals(2.5, result[1].startTime)
        assertEquals(8.5, result[1].endTime)
        assertEquals(8.5, result[2].startTime)
        assertEquals(12.0, result[2].endTime)
    }

    @Test
    fun `calculate uses slide index from SlideSegment`() {
        val slides = listOf(
            SlideSegment(index = 5, title = "Fifth", speakerNote = "five")
        )
        val durations = listOf(10.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(5, result[0].index)
    }

    @Test
    fun `calculate uses speakerNote as subtitle text`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "Title", speakerNote = "Actual speaker note text")
        )
        val durations = listOf(3.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals("Actual speaker note text", result[0].text)
    }

    @Test
    fun `calculate handles zero duration slides`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "A", speakerNote = "a"),
            SlideSegment(index = 1, title = "B", speakerNote = "b")
        )
        val durations = listOf(0.0, 5.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(0.0, result[0].startTime)
        assertEquals(0.0, result[0].endTime)
        assertEquals(0.0, result[1].startTime)
        assertEquals(5.0, result[1].endTime)
    }

    @Test
    fun `calculate mismatched sizes uses available durations`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "A", speakerNote = "a"),
            SlideSegment(index = 1, title = "B", speakerNote = "b"),
            SlideSegment(index = 2, title = "C", speakerNote = "c")
        )
        val durations = listOf(2.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(3, result.size)
        assertEquals(0.0, result[0].startTime)
        assertEquals(2.0, result[0].endTime)
        assertEquals(2.0, result[1].startTime)
        assertEquals(2.0, result[1].endTime)
        assertEquals(2.0, result[2].startTime)
        assertEquals(2.0, result[2].endTime)
    }

    @Test
    fun `calculate more durations than slides ignores extra`() {
        val slides = listOf(
            SlideSegment(index = 0, title = "A", speakerNote = "a")
        )
        val durations = listOf(2.0, 3.0, 4.0)
        val result = SubtitleTimingCalculator.calculate(slides, durations)
        assertEquals(1, result.size)
        assertEquals(0.0, result[0].startTime)
        assertEquals(2.0, result[0].endTime)
    }
}
