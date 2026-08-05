package capsule.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SlideSegmentExtendedModelTest {

    @Test
    fun `SlideSegment defaults type to HTML and manimScene to null`() {
        val segment = SlideSegment(index = 1, title = "Intro", speakerNote = "Note.")

        assertThat(segment.type).isEqualTo(SlideType.HTML)
        assertThat(segment.manimScene).isNull()
    }

    @Test
    fun `SlideSegment can specify type MANIM with manimScene`() {
        val segment = SlideSegment(
            index = 2,
            title = "Animation",
            speakerNote = "Watch this.",
            type = SlideType.MANIM,
            manimScene = "MoveSquare",
        )

        assertThat(segment.type).isEqualTo(SlideType.MANIM)
        assertThat(segment.manimScene).isEqualTo("MoveSquare")
    }

    @Test
    fun `SlideSegment can specify type HTML explicitly`() {
        val segment = SlideSegment(
            index = 1,
            title = "Intro",
            speakerNote = "Note.",
            type = SlideType.HTML,
        )

        assertThat(segment.type).isEqualTo(SlideType.HTML)
    }

    @Test
    fun `SlideSegment MANIM without manimScene is allowed`() {
        val segment = SlideSegment(
            index = 1,
            title = "Mystery",
            speakerNote = "Note.",
            type = SlideType.MANIM,
        )

        assertThat(segment.type).isEqualTo(SlideType.MANIM)
        assertThat(segment.manimScene).isNull()
    }

    @Test
    fun `SlideSegment preserves invariants when type is specified`() {
        assertThrows<IllegalArgumentException> {
            SlideSegment(index = 1, title = "  ", speakerNote = "Note.", type = SlideType.MANIM)
        }
        assertThrows<IllegalArgumentException> {
            SlideSegment(index = 1, title = "Intro", speakerNote = "  ", type = SlideType.MANIM)
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
            deckName = "mixed-deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome.", type = SlideType.HTML),
                SlideSegment(
                    index = 2,
                    title = "Anim",
                    speakerNote = "Watch.",
                    type = SlideType.MANIM,
                    manimScene = "Scene1",
                ),
                SlideSegment(index = 3, title = "End", speakerNote = "Bye.", type = SlideType.HTML),
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
            deckName = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Note.")),
        )

        assertThat(script.isEmpty).isFalse()
    }

    @Test
    fun `CapsuleScript isEmpty returns true when segments empty`() {
        val script = CapsuleScript(deckName = "deck", segments = emptyList())

        assertThat(script.isEmpty).isTrue()
    }

    @Test
    fun `CapsuleScript field is named segments not slides`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Note.")),
        )

        assertThat(script.segments).isNotNull
    }
}