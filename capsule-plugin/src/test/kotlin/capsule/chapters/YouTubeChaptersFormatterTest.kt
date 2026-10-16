package capsule.chapters

import kotlin.test.Test
import kotlin.test.assertEquals
import video.kernel.publish.VideoDescriptionComposer

/**
 * TDD unit tests for [YouTubeChaptersFormatter] (PRT-10). Pure — no I/O.
 */
class YouTubeChaptersFormatterTest {

    @Test
    fun `empty entries format to an empty string`() {
        assertEquals("", YouTubeChaptersFormatter.format(emptyList()))
    }

    @Test
    fun `timestamp uses M colon SS below one hour`() {
        assertEquals("0:00", YouTubeChaptersFormatter.formatTimestamp(0))
        assertEquals("1:05", YouTubeChaptersFormatter.formatTimestamp(65_000))
        assertEquals("59:59", YouTubeChaptersFormatter.formatTimestamp(3_599_000))
    }

    @Test
    fun `timestamp uses H colon MM colon SS from one hour`() {
        assertEquals("1:00:00", YouTubeChaptersFormatter.formatTimestamp(3_600_000))
        assertEquals("1:02:05", YouTubeChaptersFormatter.formatTimestamp(3_725_000))
    }

    @Test
    fun `timestamp matches the neutral kernel composer (HF-YOUTUBE-PUBLISH US-3)`() {
        for (ms in listOf(0L, 65_000L, 90_000L, 3_599_000L, 3_600_000L, 3_725_000L)) {
            assertEquals(
                VideoDescriptionComposer.chapterTimestamp((ms / 1000).coerceAtLeast(0).toInt()),
                YouTubeChaptersFormatter.formatTimestamp(ms),
                "mismatch at ${ms}ms",
            )
        }
    }

    @Test
    fun `format renders one line per entry`() {
        val text = YouTubeChaptersFormatter.format(
            listOf(
                ChapterEntry("Introduction", 0, 5_000),
                ChapterEntry("First part", 65_000, 130_000),
            )
        )
        assertEquals("0:00 Introduction\n1:05 First part\n", text)
    }
}
