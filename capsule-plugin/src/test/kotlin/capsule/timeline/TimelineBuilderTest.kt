package capsule.timeline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * TDD unit tests for [TimelineBuilder] (PRT-07). Pure — no I/O.
 */
class TimelineBuilderTest {

    @Test
    fun `empty durations yield an empty timeline`() {
        val timeline = TimelineBuilder.build(emptyList())
        assertEquals(0L, timeline.totalFrames)
        assertEquals(0.0, timeline.totalSeconds)
    }

    @Test
    fun `durations snap to contiguous integer frames`() {
        val timeline = TimelineBuilder.build(listOf(5.0, 3.0), TimelineConfig(fps = 30))
        assertEquals(240L, timeline.totalFrames)
        assertEquals(8.0, timeline.totalSeconds, 1e-9)
        assertEquals(0L, timeline.entries[0].startFrame)
        assertEquals(150L, timeline.entries[0].frameCount)
        assertEquals(150L, timeline.entries[1].startFrame)
        assertEquals(90L, timeline.entries[1].frameCount)
    }

    @Test
    fun `durations round to the nearest frame`() {
        // 1.01 s at 30 fps = 30.3 frames -> 30
        val timeline = TimelineBuilder.build(listOf(1.01), TimelineConfig(fps = 30))
        assertEquals(30L, timeline.entries.single().frameCount)
    }

    @Test
    fun `lead and tail pad the timeline`() {
        val timeline = TimelineBuilder.build(
            listOf(1.0),
            TimelineConfig(fps = 30, leadSeconds = 0.5, tailSeconds = 0.5),
        )
        // lead 15 + audio 30 + tail 15
        assertEquals(60L, timeline.totalFrames)
        assertEquals(15L, timeline.entries.single().startFrame)
        assertEquals(45L, timeline.entries.single().frameCount)
    }

    @Test
    fun `avance makes the visual lead the voice on later slides`() {
        val timeline = TimelineBuilder.build(
            listOf(1.0, 1.0),
            TimelineConfig(fps = 30, avanceSeconds = 0.1),
        )
        assertEquals(0L, timeline.entries[0].voiceStartFrame)
        assertEquals(33L, timeline.entries[1].voiceStartFrame) // start 30 + avance 3
    }

    @Test
    fun `a zero duration still occupies one frame`() {
        val timeline = TimelineBuilder.build(listOf(0.0), TimelineConfig(fps = 30))
        assertEquals(1L, timeline.entries.single().frameCount)
    }

    @Test
    fun `framesFor and secondsFor are inverse`() {
        assertEquals(150L, TimelineBuilder.framesFor(5.0, 30))
        assertEquals(5.0, TimelineBuilder.secondsFor(150, 30), 1e-9)
        assertEquals(0L, TimelineBuilder.framesFor(-1.0, 30))
    }

    @Test
    fun `a non positive fps is rejected`() {
        assertFailsWith<IllegalArgumentException> { TimelineConfig(fps = 0) }
    }
}
