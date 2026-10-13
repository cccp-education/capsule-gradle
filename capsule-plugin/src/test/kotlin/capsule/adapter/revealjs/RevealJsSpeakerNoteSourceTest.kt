package capsule.adapter.revealjs

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * TDD — reveal.js [capsule.kernel.segment.SegmentSource] adapter
 * (CAP-ENGINE-DECOUPLING US-3).
 */
class RevealJsSpeakerNoteSourceTest {

    @TempDir
    lateinit var tempDir: Path

    private fun adocFile(name: String, content: String): File =
        tempDir.resolve(name).toFile().apply { writeText(content) }

    private val deck = """
        = Demo Deck

        == Intro
        [NOTE.speaker]
        --
        Welcome to the capsule.
        --

        == Silent
        A slide without any speaker note.

        == Closing
        [NOTE.speaker]
        --
        Goodbye.
        --
    """.trimIndent()

    @Test
    fun `yields one segment per narrated slide, reindexed from 1`() {
        val source = RevealJsSpeakerNoteSource
        val segments = source.segments(adocFile("demo-deck.adoc", deck))

        assertThat(segments).hasSize(2)
        assertThat(segments[0].index).isEqualTo(1)
        assertThat(segments[0].title).isEqualTo("Intro")
        assertThat(segments[0].narration).isEqualTo("Welcome to the capsule.")
        assertThat(segments[1].index).isEqualTo(2)
        assertThat(segments[1].title).isEqualTo("Closing")
    }

    @Test
    fun `skips slides that carry no speaker note`() {
        val source = RevealJsSpeakerNoteSource
        val segments = source.segments(adocFile("demo-deck.adoc", deck))

        assertThat(segments.map { it.title }).doesNotContain("Silent")
    }

    @Test
    fun `exposes a stable engine-neutral source name`() {
        assertThat(RevealJsSpeakerNoteSource.name()).isEqualTo("revealjs-speaker-notes")
    }

    @Test
    fun `an empty deck yields no segment`() {
        val source = RevealJsSpeakerNoteSource
        assertThat(source.segments(adocFile("empty-deck.adoc", "= Empty\n\nNo notes here."))).isEmpty()
    }

    @Test
    fun `parser rejects a blank deck name`() {
        assertThatThrownBy { RevealJsSpeakerNoteParser.parse("= Deck", "  ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
