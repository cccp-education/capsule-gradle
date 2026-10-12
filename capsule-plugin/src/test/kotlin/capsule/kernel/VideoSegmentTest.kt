package capsule.kernel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TDD — engine-neutral [VideoSegment] model (CAP-ENGINE-DECOUPLING US-1).
 */
class VideoSegmentTest {

    @Test
    fun `defaults kind to VISUAL and scene to null`() {
        val segment = VideoSegment(index = 1, title = "Opening", narration = "Hello.")

        assertThat(segment.kind).isEqualTo(VideoKind.VISUAL)
        assertThat(segment.scene).isNull()
    }

    @Test
    fun `carries an animation kind with its scene`() {
        val segment = VideoSegment(
            index = 2,
            title = "Diagram",
            narration = "Watch this.",
            kind = VideoKind.ANIMATION,
            scene = "MoveSquare",
        )

        assertThat(segment.kind).isEqualTo(VideoKind.ANIMATION)
        assertThat(segment.scene).isEqualTo("MoveSquare")
    }

    @Test
    fun `rejects blank title`() {
        assertThrows<IllegalArgumentException> {
            VideoSegment(index = 1, title = "  ", narration = "Hello.")
        }
    }

    @Test
    fun `rejects blank narration`() {
        assertThrows<IllegalArgumentException> {
            VideoSegment(index = 1, title = "Opening", narration = "  ")
        }
    }

    @Test
    fun `legacy accessors reflect the neutral fields`() {
        val segment = VideoSegment(
            index = 3,
            title = "Diagram",
            narration = "Body.",
            kind = VideoKind.ANIMATION,
            scene = "Scene1",
        )

        @Suppress("DEPRECATION")
        assertThat(segment.speakerNote).isEqualTo("Body.")

        @Suppress("DEPRECATION")
        assertThat(segment.manimScene).isEqualTo("Scene1")
    }
}
