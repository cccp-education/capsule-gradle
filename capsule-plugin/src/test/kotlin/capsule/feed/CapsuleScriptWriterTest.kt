package capsule.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CapsuleScriptWriterTest {

    @Test
    fun `write should produce header with deck name`() {
        val script = CapsuleScript(
            deckName = "kotlin-intro",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome.")),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).startsWith("=== CAPSULE SCRIPT : kotlin-intro ===")
    }

    @Test
    fun `write should emit one segment block per slide`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome."),
                SlideSegment(index = 2, title = "Topic", speakerNote = "Details."),
            ),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).contains("--- SLIDE 1 : Intro ---")
        assertThat(out).contains("--- SLIDE 2 : Topic ---")
        assertThat(out.lines().count { it.startsWith("--- SLIDE") }).isEqualTo(2)
    }

    @Test
    fun `write should put speaker note on the line after the slide header`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome.")),
        )

        val out = CapsuleScriptWriter.write(script)
        val lines = out.lines()

        val headerIdx = lines.indexOf("--- SLIDE 1 : Intro ---")
        assertThat(headerIdx).isNotEqualTo(-1)
        assertThat(lines[headerIdx + 1]).isEqualTo("Welcome.")
    }

    @Test
    fun `write should separate segments with a blank line`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome."),
                SlideSegment(index = 2, title = "Topic", speakerNote = "Details."),
            ),
        )

        val out = CapsuleScriptWriter.write(script)
        val lines = out.lines()

        val firstNoteIdx = lines.indexOf("Welcome.")
        val secondHeaderIdx = lines.indexOf("--- SLIDE 2 : Topic ---")
        assertThat(lines[firstNoteIdx + 1]).isEqualTo("")
        assertThat(lines[secondHeaderIdx - 1]).isEqualTo("")
    }

    @Test
    fun `write should emit a blank line after the header`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Welcome.")),
        )

        val out = CapsuleScriptWriter.write(script)
        val lines = out.lines()

        val headerIdx = lines.indexOf("=== CAPSULE SCRIPT : deck ===")
        assertThat(lines[headerIdx + 1]).isEqualTo("")
    }

    @Test
    fun `write should trim trailing whitespace from speaker notes`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "  Welcome.  ")),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).contains("--- SLIDE 1 : Intro ---\nWelcome.\n")
    }

    @Test
    fun `write should preserve multi-line speaker notes`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", speakerNote = "Line one.\nLine two."),
            ),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).contains("--- SLIDE 1 : Intro ---\nLine one.\nLine two.\n")
    }

    @Test
    fun `CapsuleScript should reject blank deck name at construction`() {
        try {
            CapsuleScript(
                deckName = "",
                segments = listOf(SlideSegment(index = 1, title = "Intro", speakerNote = "Note.")),
            )
            error("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("deckName")
        }
    }

    @Test
    fun `write should reject empty segments`() {
        val script = CapsuleScript(deckName = "deck", segments = emptyList())

        try {
            CapsuleScriptWriter.write(script)
            error("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("segments")
        }
    }

    @Test
    fun `SlideSegment should reject blank title at construction`() {
        try {
            SlideSegment(index = 1, title = "   ", speakerNote = "Note.")
            error("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("title")
        }
    }

    @Test
    fun `SlideSegment should reject blank speaker note at construction`() {
        try {
            SlideSegment(index = 1, title = "Intro", speakerNote = "  ")
            error("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("speakerNote")
        }
    }

    @Test
    fun `CapsuleScript should preserve explicit segment indices`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(index = 7, title = "Intro", speakerNote = "Note."),
            ),
        )

        assertThat(script.segments[0].index).isEqualTo(7)
    }

    @Test
    fun `write should emit manim marker when slide type is MANIM`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(
                    index = 1,
                    title = "Animation",
                    speakerNote = "Watch.",
                    type = SlideType.MANIM,
                    manimScene = "MoveSquare",
                ),
            ),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).contains("--- SLIDE 1 : Animation [manim:MoveSquare] ---")
    }

    @Test
    fun `write should not emit manim marker when slide type is HTML`() {
        val script = CapsuleScript(
            deckName = "deck",
            segments = listOf(
                SlideSegment(index = 1, title = "Intro", speakerNote = "Note.", type = SlideType.HTML),
            ),
        )

        val out = CapsuleScriptWriter.write(script)

        assertThat(out).contains("--- SLIDE 1 : Intro ---")
        assertThat(out).doesNotContain("[manim:")
    }
}