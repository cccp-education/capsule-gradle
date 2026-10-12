package capsule.kernel

import capsule.feed.deckName
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * TDD — engine-neutral [VideoScript] aggregate (CAP-ENGINE-DECOUPLING US-1).
 */
class VideoScriptTest {

    @Test
    fun `holds a name and ordered segments`() {
        val script = VideoScript(
            name = "production",
            segments = listOf(
                VideoSegment(index = 1, title = "A", narration = "a."),
                VideoSegment(index = 2, title = "B", narration = "b."),
            ),
        )

        assertThat(script.name).isEqualTo("production")
        assertThat(script.segments).hasSize(2)
        assertThat(script.isEmpty).isFalse()
    }

    @Test
    fun `empty segment list is allowed and reported`() {
        val script = VideoScript(name = "production", segments = emptyList())

        assertThat(script.isEmpty).isTrue()
    }

    @Test
    fun `rejects blank name`() {
        assertThrows<IllegalArgumentException> {
            VideoScript(name = "  ", segments = emptyList())
        }
    }

    @Test
    fun `legacy deck-name accessor reflects the neutral name`() {
        val script = VideoScript(name = "production", segments = emptyList())

        @Suppress("DEPRECATION")
        assertThat(script.deckName).isEqualTo("production")
    }
}
