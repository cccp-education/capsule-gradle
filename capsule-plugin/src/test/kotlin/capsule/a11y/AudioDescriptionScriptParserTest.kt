package capsule.a11y

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CAP-AUDIODESC US-2 — explicit audio-description script (silent degradation).
 */
class AudioDescriptionScriptParserTest {
    @Test
    fun `parses one cue per line`() {
        val cues =
            AudioDescriptionScriptParser.parse(
                """
                # scene 1
                2.5 | A container appears on the left.
                3.0 | The terminal shows the failing build.
                """.trimIndent(),
            )

        assertEquals(2, cues.size)
        assertEquals("A container appears on the left.", cues[0].text)
        assertEquals(2.5, cues[0].durationSeconds)
        assertEquals(3.0, cues[1].durationSeconds)
    }

    @Test
    fun `ignores comments and blank lines`() {
        val cues = AudioDescriptionScriptParser.parse("\n# comment\n\n1.0 | Visible thing.\n\n")

        assertEquals(1, cues.size)
    }

    @Test
    fun `skips a malformed line instead of failing`() {
        val cues =
            AudioDescriptionScriptParser.parse(
                """
                not a cue
                1.0 | A valid cue.
                abc | not a number
                0 | zero is not a duration
                -2 | negative is not a duration
                2.0 |
                """.trimIndent(),
            )

        assertEquals(1, cues.size)
        assertEquals("A valid cue.", cues[0].text)
    }

    @Test
    fun `keeps the separator inside the text`() {
        val cues = AudioDescriptionScriptParser.parse("1.5 | The label reads a | b | c.")

        assertEquals("The label reads a | b | c.", cues.single().text)
    }

    @Test
    fun `an empty script yields no cue`() {
        assertTrue(AudioDescriptionScriptParser.parse("").isEmpty())
    }

    @Test
    fun `a missing file degrades silently`() {
        val missing = File(Files.createTempDirectory("audiodesc").toFile(), "absent.txt")

        assertTrue(AudioDescriptionScriptParser.load(missing).isEmpty())
    }

    @Test
    fun `a file is parsed like its content`() {
        val file = Files.createTempDirectory("audiodesc").toFile().resolve("description.txt")
        file.writeText("1.0 | From a file.\n")

        val cues = AudioDescriptionScriptParser.load(file)

        assertEquals("From a file.", cues.single().text)
    }
}
