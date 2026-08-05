package capsule.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AsciidocSpeakerNoteParserTest {

    @Test
    fun `parse should extract one segment per slide with a NOTE speaker block`() {
        val adoc = """
            = Deck title
            author
            :revealjs_theme: sky

            == Slide One

            content

            [NOTE.speaker]
            --
            First speaker note.
            --

            == Slide Two

            content

            [NOTE.speaker]
            --
            Second speaker note.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.deckName).isEqualTo("deck")
        assertThat(script.segments).hasSize(2)
        assertThat(script.segments[0].index).isEqualTo(1)
        assertThat(script.segments[0].title).isEqualTo("Slide One")
        assertThat(script.segments[0].speakerNote).isEqualTo("First speaker note.")
        assertThat(script.segments[1].index).isEqualTo(2)
        assertThat(script.segments[1].title).isEqualTo("Slide Two")
        assertThat(script.segments[1].speakerNote).isEqualTo("Second speaker note.")
    }

    @Test
    fun `parse should skip slides without a NOTE speaker block`() {
        val adoc = """
            = Deck

            == Slide With Note

            [NOTE.speaker]
            --
            Has a note.
            --

            == Slide Without Note

            No speaker note here.
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments).hasSize(1)
        assertThat(script.segments[0].title).isEqualTo("Slide With Note")
    }

    @Test
    fun `parse should preserve multi-line speaker notes`() {
        val adoc = """
            = Deck

            == Slide

            [NOTE.speaker]
            --
            Line one.
            Line two.
            Line three.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments[0].speakerNote).isEqualTo("Line one.\nLine two.\nLine three.")
    }

    @Test
    fun `parse should handle NOTE speaker with attributes after the bracket`() {
        val adoc = """
            = Deck

            == Slide

            [NOTE.speaker,style=foo]
            --
            Note text.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments).hasSize(1)
        assertThat(script.segments[0].speakerNote).isEqualTo("Note text.")
    }

    @Test
    fun `parse should ignore slides attribute lines preceding the heading`() {
        val adoc = """
            = Deck

            [.slide,data-transition="zoom"]
            == Titled Slide

            [NOTE.speaker]
            --
            Note.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments[0].title).isEqualTo("Titled Slide")
    }

    @Test
    fun `parse should return empty segments when no slide has speaker notes`() {
        val adoc = """
            = Deck

            == Slide One

            no note

            == Slide Two

            no note either
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments).isEmpty()
    }

    @Test
    fun `parse should trim whitespace in titles`() {
        val adoc = """
            = Deck

            ==   Spaced Title  

            [NOTE.speaker]
            --
            Note.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments[0].title).isEqualTo("Spaced Title")
    }

    @Test
    fun `parse should only treat level-2 headings as slides`() {
        val adoc = """
            = Deck Title

            == Real Slide

            [NOTE.speaker]
            --
            Note.
            --

            === Subsection

            [NOTE.speaker]
            --
            Should be ignored.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments).hasSize(1)
        assertThat(script.segments[0].title).isEqualTo("Real Slide")
    }

    @Test
    fun `parse should number segments sequentially by occurrence`() {
        val adoc = """
            = Deck

            == Third

            [NOTE.speaker]
            --
            Note 3.
            --

            == First

            [NOTE.speaker]
            --
            Note 1.
            --

            == Second

            [NOTE.speaker]
            --
            Note 2.
            --
        """.trimIndent()

        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments.map { it.index }).containsExactly(1, 2, 3)
        assertThat(script.segments.map { it.title }).containsExactly("Third", "First", "Second")
    }

    @Test
    fun `parse should reject blank deck name`() {
        try {
            AsciidocSpeakerNoteParser.parse("= Deck", deckName = "  ")
            error("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("deckName")
        }
    }

    @Test
    fun `parse should return empty deck name when no heading found`() {
        val adoc = "no headings here at all"
        val script = AsciidocSpeakerNoteParser.parse(adoc, deckName = "deck")

        assertThat(script.segments).isEmpty()
    }
}