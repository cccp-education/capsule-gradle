package capsule.a11y

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for [AudioDescriptionPlanner] (CAP-AUDIODESC US-1).
 *
 * Pure — no TTS, no FFmpeg, no I/O. Pins the determinism of the placement and
 * the two honesty rules of the EPIC: never overlap the narration, and **declare**
 * the limit when the silences are too short.
 */
class AudioDescriptionPlannerTest {
    @Test
    fun `places a cue in the leading silence`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("A container appears.", 2.0)),
                narration = listOf(NarrationWindow(10.0, 20.0)),
            )

        assertThat(script.segments).hasSize(1)
        assertThat(script.segments[0].text).isEqualTo("A container appears.")
        assertThat(script.segments[0].startSeconds).isEqualTo(0.0)
        assertThat(script.segments[0].endSeconds).isEqualTo(2.0)
        assertThat(script.hasUnplaced).isFalse()
    }

    @Test
    fun `places a cue between two narration windows`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("The diagram is shown.", 5.0)),
                narration = listOf(NarrationWindow(0.0, 10.0), NarrationWindow(30.0, 40.0)),
            )

        assertThat(script.segments.single().startSeconds).isEqualTo(10.0)
        assertThat(script.segments.single().endSeconds).isEqualTo(15.0)
    }

    @Test
    fun `never overlaps the narration`() {
        val narration = listOf(NarrationWindow(5.0, 10.0), NarrationWindow(20.0, 25.0))

        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("First description.", 4.0), AudioDescriptionCue("Second description.", 3.0)),
                narration = narration,
            )

        assertThat(script.segments).hasSize(2)
        for (segment in script.segments) {
            for (window in narration) {
                val disjoint = segment.endSeconds <= window.startSeconds || segment.startSeconds >= window.endSeconds
                assertThat(disjoint).describedAs("segment %s vs window %s", segment, window).isTrue()
            }
        }
    }

    @Test
    fun `keeps the cue order with a first-fit placement`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("A.", 2.0), AudioDescriptionCue("B.", 3.0)),
                narration = listOf(NarrationWindow(0.0, 5.0), NarrationWindow(10.0, 15.0)),
            )

        assertThat(script.segments.map { it.text }).containsExactly("A.", "B.")
        assertThat(script.segments.map { it.startSeconds }).containsExactly(5.0, 7.0)
    }

    @Test
    fun `declares unplaced cues when the silences are too short`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("Too long for the clip.", 2.0)),
                narration = listOf(NarrationWindow(0.0, 10.0)),
            )

        assertThat(script.isEmpty).isTrue()
        assertThat(script.hasUnplaced).isTrue()
        assertThat(script.unplaced.single().text).isEqualTo("Too long for the clip.")
    }

    @Test
    fun `uses the trailing silence when the total duration is provided`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("Closing shot.", 5.0)),
                narration = listOf(NarrationWindow(0.0, 10.0)),
                totalDurationSeconds = 20.0,
            )

        assertThat(script.segments.single().startSeconds).isEqualTo(10.0)
        assertThat(script.segments.single().endSeconds).isEqualTo(15.0)
    }

    @Test
    fun `ignores silences shorter than the minimum gap`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("Too tight.", 1.0)),
                narration = listOf(NarrationWindow(0.0, 10.0), NarrationWindow(10.2, 20.0)),
                minGapSeconds = 0.5,
            )

        assertThat(script.isEmpty).isTrue()
        assertThat(script.hasUnplaced).isTrue()
    }

    @Test
    fun `merges overlapping narration windows`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("After the overlap.", 5.0)),
                narration = listOf(NarrationWindow(10.0, 20.0), NarrationWindow(15.0, 25.0)),
            )

        assertThat(script.segments.single().startSeconds).isEqualTo(0.0)
        assertThat(script.segments.single().endSeconds).isEqualTo(5.0)
    }

    @Test
    fun `sums the spoken duration of the placed segments`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = listOf(AudioDescriptionCue("A.", 2.0), AudioDescriptionCue("B.", 3.0)),
                narration = listOf(NarrationWindow(0.0, 5.0), NarrationWindow(10.0, 15.0)),
            )

        assertThat(script.totalSpokenSeconds).isEqualTo(5.0)
    }

    @Test
    fun `returns an empty script without cues`() {
        val script =
            AudioDescriptionPlanner.plan(
                cues = emptyList(),
                narration = listOf(NarrationWindow(0.0, 10.0)),
            )

        assertThat(script.isEmpty).isTrue()
        assertThat(script.hasUnplaced).isFalse()
        assertThat(script.totalSpokenSeconds).isEqualTo(0.0)
    }

    @Test
    fun `rejects an invalid narration window`() {
        assertThrows<IllegalArgumentException> { NarrationWindow(5.0, 5.0) }
        assertThrows<IllegalArgumentException> { NarrationWindow(-1.0, 2.0) }
    }

    @Test
    fun `rejects a blank cue`() {
        assertThrows<IllegalArgumentException> { AudioDescriptionCue("   ", 1.0) }
    }

    @Test
    fun `rejects a non-positive cue duration`() {
        assertThrows<IllegalArgumentException> { AudioDescriptionCue("A description.", 0.0) }
    }

    @Test
    fun `rejects a negative minimum gap`() {
        assertThrows<IllegalArgumentException> {
            AudioDescriptionPlanner.plan(cues = emptyList(), narration = emptyList(), minGapSeconds = -1.0)
        }
    }
}
