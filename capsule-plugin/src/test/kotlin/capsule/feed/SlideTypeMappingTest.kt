package capsule.feed

import capsule.kernel.segment.VideoKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * TDD — legacy [SlideType] ↔ neutral [VideoKind] mapping
 * (CAP-ENGINE-DECOUPLING US-1).
 */
class SlideTypeMappingTest {

    @Test
    fun `maps legacy HTML to neutral VISUAL`() {
        assertThat(SlideType.HTML.toVideoKind()).isEqualTo(VideoKind.VISUAL)
    }

    @Test
    fun `maps legacy MANIM to neutral ANIMATION`() {
        assertThat(SlideType.MANIM.toVideoKind()).isEqualTo(VideoKind.ANIMATION)
    }

    @Test
    fun `round-trips through both directions`() {
        SlideType.entries.forEach { legacy ->
            assertThat(legacy.toVideoKind().toSlideType()).isEqualTo(legacy)
        }
        VideoKind.entries.forEach { neutral ->
            assertThat(neutral.toSlideType().toVideoKind()).isEqualTo(neutral)
        }
    }

    @Test
    fun `the legacy typealias bridge exposes neutral kind through the old accessor`() {
        val segment: SlideSegment = SlideSegment(
            index = 1,
            title = "Diagram",
            narration = "Body.",
            kind = VideoKind.ANIMATION,
            scene = "Scene1",
        )

        @Suppress("DEPRECATION")
        assertThat(segment.type).isEqualTo(SlideType.MANIM)
    }
}
