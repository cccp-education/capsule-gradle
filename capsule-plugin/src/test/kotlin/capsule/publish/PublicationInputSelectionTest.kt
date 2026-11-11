package capsule.publish

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for [PublicationInputSelection] (HF-YOUTUBE-PUBLISH US-3b).
 * Pure — real files in a temp directory, no Gradle.
 */
class PublicationInputSelectionTest {
    @TempDir
    lateinit var tempDir: Path

    private fun video(name: String): File = tempDir.resolve(name).toFile().apply { writeText("mp4") }

    @Test
    fun `a single mp4 is selected unambiguously`() {
        val only = video("deck_fr.mp4")

        val selection = PublicationInputSelection.selectVideo(listOf(only), languageCode = "")

        assertEquals(only, selection.file)
        assertEquals(1, selection.candidates)
        assertFalse(selection.ambiguous)
    }

    @Test
    fun `no video yields no selection`() {
        val selection = PublicationInputSelection.selectVideo(emptyList(), languageCode = "")

        assertNull(selection.file)
        assertEquals(0, selection.candidates)
        assertFalse(selection.ambiguous)
    }

    @Test
    fun `the language code picks its own mp4 among several`() {
        val fr = video("deck_fr.mp4")
        val en = video("deck_en.mp4")

        val selection = PublicationInputSelection.selectVideo(listOf(fr, en), languageCode = "en")

        assertEquals(en, selection.file)
        assertEquals(2, selection.candidates)
        assertFalse(selection.ambiguous)
    }

    @Test
    fun `several mp4s without a language are ambiguous but deterministic`() {
        val en = video("deck_en.mp4")
        val fr = video("deck_fr.mp4")

        val selection = PublicationInputSelection.selectVideo(listOf(fr, en), languageCode = "")

        assertEquals(en, selection.file)
        assertTrue(selection.ambiguous)
    }

    @Test
    fun `a language matching no video is ambiguous`() {
        val fr = video("deck_fr.mp4")
        val en = video("deck_en.mp4")

        val selection = PublicationInputSelection.selectVideo(listOf(fr, en), languageCode = "de")

        assertEquals(en, selection.file)
        assertTrue(selection.ambiguous)
    }

    @Test
    fun `missing files are ignored`() {
        val present = video("deck_fr.mp4")
        val missing = tempDir.resolve("deck_en.mp4").toFile()

        val selection = PublicationInputSelection.selectVideo(listOf(present, missing), languageCode = "")

        assertEquals(present, selection.file)
        assertEquals(1, selection.candidates)
    }

    @Test
    fun `the chapters file wins over the configured lines`() {
        val chaptersFile = tempDir.resolve("chapters.txt").toFile()
        chaptersFile.writeText("0:00 Intro\n1:05 Part\n\n")

        val lines = PublicationInputSelection.chapterLines(chaptersFile, listOf("9:99 configured"))

        assertEquals(listOf("0:00 Intro", "1:05 Part"), lines)
    }

    @Test
    fun `an absent or blank chapters file falls back to the configured lines`() {
        val blank = tempDir.resolve("chapters.txt").toFile().apply { writeText("\n") }

        assertEquals(listOf("0:00 Intro"), PublicationInputSelection.chapterLines(null, listOf("0:00 Intro")))
        assertEquals(listOf("0:00 Intro"), PublicationInputSelection.chapterLines(blank, listOf("0:00 Intro")))
    }
}
