package capsule.viral

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for the market copy reading port (CAP-VIRAL US-9):
 * [MarketCopy], [MarketCopySource] + [FileMarketCopySource].
 *
 * Capsule **reads** the copy produced by bakery (GBL-006) — it never
 * regenerates it (boundary: bakery écrit, capsule consomme).
 */
class MarketCopySourceTest {

    private fun write(root: File, name: String, content: String) =
        File(root, name).also { it.parentFile.mkdirs(); it.writeText(content) }

    @Test
    fun `read resolves the language-platform file`(@TempDir root: File) {
        write(
            root, "fr-tiktok.json",
            """{"language":"fr","platform":"TIKTOK","hook":"H","title":"T","caption":"C","hashtags":["#fpa"]}""",
        )
        val source = FileMarketCopySource(root)
        val copy = source.read("fr", ViralPlatform.TIKTOK)
        assertEquals("H", copy?.hook)
        assertEquals("T", copy?.title)
        assertEquals(listOf("#fpa"), copy?.hashtags)
        assertEquals(ViralPlatform.TIKTOK, copy?.platform)
    }

    @Test
    fun `read falls back to the language-only file`(@TempDir root: File) {
        write(root, "en.json", """{"language":"en","platform":"REELS","title":"Hello"}""")
        val source = FileMarketCopySource(root)
        val copy = source.read("en", ViralPlatform.YOUTUBE_SHORT)
        assertEquals("Hello", copy?.title)
    }

    @Test
    fun `read returns null when no file matches`(@TempDir root: File) {
        val source = FileMarketCopySource(root)
        assertNull(source.read("fr", ViralPlatform.TIKTOK))
        assertNull(source.read("", ViralPlatform.TIKTOK))
    }

    @Test
    fun `read returns null on malformed or contentless file`(@TempDir root: File) {
        write(root, "fr-tiktok.json", "not json")
        write(root, "de.json", """{"language":"de","hook":"","title":"","caption":""}""")
        val source = FileMarketCopySource(root)
        assertNull(source.read("fr", ViralPlatform.TIKTOK))
        assertNull(source.read("de", ViralPlatform.TIKTOK))
    }

    @Test
    fun `isAvailable reflects the presence of copy files`(@TempDir root: File) {
        assertFalse(FileMarketCopySource(root).isAvailable())
        write(root, "fr.json", """{"language":"fr","title":"T"}""")
        assertTrue(FileMarketCopySource(root).isAvailable())
    }

    @Test
    fun `MarketCopy rejects an all-blank piece`() {
        assertTrue(runCatching { MarketCopy("fr", ViralPlatform.TIKTOK, "", "", "") }.isFailure)
    }

    @Test
    fun `MarketCopy ignores unknown json keys`(@TempDir root: File) {
        write(root, "fr.json", """{"language":"fr","title":"T","unknown":"x","extra":42}""")
        val copy = FileMarketCopySource(root).read("fr", ViralPlatform.TIKTOK)
        assertEquals("T", copy?.title)
    }
}
