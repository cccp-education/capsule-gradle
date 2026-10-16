package capsule.publish

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import video.kernel.publish.VideoPublicationLimits

/**
 * TDD unit tests for [VideoDescriptionBuilder] (PRT-10). Pure — no I/O.
 */
class VideoDescriptionBuilderTest {

    @Test
    fun `forbidden chars are the shared neutral kernel set (HF-YOUTUBE-PUBLISH US-3)`() {
        assertEquals(VideoPublicationLimits.FORBIDDEN_CHARS, VideoDescriptionBuilder.FORBIDDEN_CHARS)
    }

    @Test
    fun `builds title chapters and tags`() {
        val result = VideoDescriptionBuilder.build(
            title = "My capsule",
            chapterLines = listOf("0:00 Intro", "1:05 Part"),
            tags = listOf("capsule", "#video"),
        )
        assertTrue(result.text.contains("My capsule"), result.text)
        assertTrue(result.text.contains("CHAPTERS"), result.text)
        assertTrue(result.text.contains("0:00 Intro"), result.text)
        assertTrue(result.text.contains("#capsule #video"), result.text)
        assertEquals(0, result.strippedForbiddenChars)
    }

    @Test
    fun `strips angle brackets and counts them`() {
        val result = VideoDescriptionBuilder.build(title = "a < b > c")
        assertFalse(result.text.contains('<'), result.text)
        assertFalse(result.text.contains('>'), result.text)
        assertEquals(2, result.strippedForbiddenChars)
        assertTrue(result.text.contains("a  b  c"), result.text)
    }

    @Test
    fun `blank inputs produce a minimal description`() {
        val result = VideoDescriptionBuilder.build(title = "")
        assertEquals("\n", result.text)
        assertEquals(0, result.strippedForbiddenChars)
    }

    @Test
    fun `header is prepended when present`() {
        val result = VideoDescriptionBuilder.build(title = "T", header = "Header < line")
        assertTrue(result.text.startsWith("Header  line"), result.text)
        assertEquals(1, result.strippedForbiddenChars)
    }
}
