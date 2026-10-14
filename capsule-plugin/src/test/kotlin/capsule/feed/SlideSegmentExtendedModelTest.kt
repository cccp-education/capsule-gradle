package capsule.feed

import video.kernel.segment.VideoKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SlideSegmentExtendedModelTest {

    @Test
    fun `SlideSegment defaults type to HTML and manimScene to null`() {
        val segment = SlideSegment(index = 1, title = "Intro", narration = "Note.")

        assertThat(segment.type).isEqualTo(SlideType.HTML)
        assertThat(segment.manimScene).isNull()
    }

    @Test
    fun `SlideSegment can specify type MANIM with manimScene`() {
        val segment = SlideSegment(
            index = 2,
            title = "Animation",
            narration = "Watch this.",
            kind = VideoKind.ANIMATION,
            scene = "MoveSquare",
        )

        assertThat(segment.type).isEqualTo(SlideType.MANIM)
        assertThat(segment.manimScene).isEqualTo("MoveSquare")
    }

    @Test
    fun `SlideSegment can specify type HTML explicitly`() {
        val segment = SlideSegment(
            index = 1,
            title = "Intro",
            narration = "Note.",
            kind = VideoKind.VISUAL,
        )

        assertThat(segment.type).isEqualTo(SlideType.HTML)
    }

    @Test
    fun `SlideSegment MANIM without manimScene is allowed`() {
        val segment = SlideSegment(
            index = 1,
            title = "Mystery",
            narration = "Note.",
            kind = VideoKind.ANIMATION,
        )

        assertThat(segment.type).isEqualTo(SlideType.MANIM)
        assertThat(segment.manimScene).isNull()
    }

    @Test
    fun `SlideSegment preserves invariants when type is specified`() {
        assertThrows<IllegalArgumentException> {
            SlideSegment(index = 1, title = "  ", narration = "Note.", kind = VideoKind.ANIMATION)
        }
        assertThrows<IllegalArgumentException> {
            SlideSegment(index = 1, title = "Intro", narration = "  ", kind = VideoKind.ANIMATION)
        }
    }

    @Test
    fun `SlideType enum has exactly HTML and MANIM values`() {
        assertThat(SlideType.entries).hasSize(2)
        assertThat(SlideType.valueOf("HTML")).isEqualTo(SlideType.HTML)
        assertThat(SlideType.valueOf("MANIM")).isEqualTo(SlideType.MANIM)
    }

    @Test
    fun `CapsuleScript segments can mix HTML and MANIM slides`() {
        val script = CapsuleScript(
            name = "mixed-deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", narration = "Welcome.", kind = VideoKind.VISUAL),
                SlideSegment(
                    index = 2,
                    title = "Anim",
                    narration = "Watch.",
                    kind = VideoKind.ANIMATION,
                    scene = "Scene1",
                ),
                SlideSegment(index = 3, title = "End", narration = "Bye.", kind = VideoKind.VISUAL),
            ),
        )

        assertThat(script.segments).hasSize(3)
        assertThat(script.segments[0].type).isEqualTo(SlideType.HTML)
        assertThat(script.segments[1].type).isEqualTo(SlideType.MANIM)
        assertThat(script.segments[1].manimScene).isEqualTo("Scene1")
        assertThat(script.segments[2].type).isEqualTo(SlideType.HTML)
    }

    @Test
    fun `CapsuleScript isEmpty returns false when segments non-empty`() {
        val script = CapsuleScript(
            name = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", narration = "Note.")),
        )

        assertThat(script.isEmpty).isFalse()
    }

    @Test
    fun `CapsuleScript isEmpty returns true when segments empty`() {
        val script = CapsuleScript(name = "deck", segments = emptyList())

        assertThat(script.isEmpty).isTrue()
    }

    @Test
    fun `CapsuleScript field is named segments not slides`() {
        val script = CapsuleScript(
            name = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", narration = "Note.")),
        )

        assertThat(script.segments).isNotNull
    }
}