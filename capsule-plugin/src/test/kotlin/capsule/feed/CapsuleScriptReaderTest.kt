package capsule.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CapsuleScriptReaderTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `read should parse deck name from header`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : cours ===

            --- SLIDE 1 : Intro ---
            Bienvenue dans la formation.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.deckName).isEqualTo("cours")
    }

    @Test
    fun `read should parse all slide segments`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : deck ===

            --- SLIDE 1 : Intro ---
            Welcome.

            --- SLIDE 2 : Topic ---
            Details here.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments).hasSize(2)
        assertThat(parsed.segments[0].index).isEqualTo(1)
        assertThat(parsed.segments[0].title).isEqualTo("Intro")
        assertThat(parsed.segments[0].speakerNote).isEqualTo("Welcome.")
        assertThat(parsed.segments[1].index).isEqualTo(2)
        assertThat(parsed.segments[1].title).isEqualTo("Topic")
        assertThat(parsed.segments[1].speakerNote).isEqualTo("Details here.")
    }

    @Test
    fun `read should default type to HTML without markers`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : simple ===

            --- SLIDE 1 : Slide simple ---
            Juste du texte.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments[0].type).isEqualTo(SlideType.HTML)
        assertThat(parsed.segments[0].manimScene).isNull()
    }

    @Test
    fun `read should extract MANIM type and manimScene from title marker`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : cours ===

            --- SLIDE 1 : Intro ---
            Bienvenue.

            --- SLIDE 2 : Anim [manim:MoveSquare] ---
            Voici l'animation.

            --- SLIDE 3 : Fin [html] ---
            Conclusion.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments).hasSize(3)
        assertThat(parsed.segments[0].type).isEqualTo(SlideType.HTML)
        assertThat(parsed.segments[0].manimScene).isNull()
        assertThat(parsed.segments[1].type).isEqualTo(SlideType.MANIM)
        assertThat(parsed.segments[1].manimScene).isEqualTo("MoveSquare")
        assertThat(parsed.segments[1].title).isEqualTo("Anim")
        assertThat(parsed.segments[2].type).isEqualTo(SlideType.HTML)
        assertThat(parsed.segments[2].title).isEqualTo("Fin")
    }

    @Test
    fun `read should preserve multi-line speaker notes`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : deck ===

            --- SLIDE 1 : Intro ---
            Line one.
            Line two.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments[0].speakerNote).isEqualTo("Line one.\nLine two.")
    }

    @Test
    fun `read should fallback to filename when header missing`() {
        val scriptFile = File(tempDir, "mydeck-script.txt")
        scriptFile.writeText(
            """
            --- SLIDE 1 : Intro ---
            Note.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.deckName).isEqualTo("mydeck-script")
    }

    @Test
    fun `read should return empty segments when no slide markers`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : empty ===
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments).isEmpty()
        assertThat(parsed.isEmpty).isTrue()
    }

    @Test
    fun `read should skip blank lines between segments without adding to notes`() {
        val scriptFile = writeScript(
            """
            === CAPSULE SCRIPT : deck ===

            --- SLIDE 1 : Intro ---
            Welcome.

            --- SLIDE 2 : Topic ---
            Details.
            """.trimIndent()
        )

        val parsed = CapsuleScriptReader.read(scriptFile)

        assertThat(parsed.segments[0].speakerNote).doesNotContain("\n\n")
        assertThat(parsed.segments[1].speakerNote).isEqualTo("Details.")
    }

    private fun writeScript(content: String): File {
        val file = File(tempDir, "test-script.txt")
        file.writeText(content)
        return file
    }
}