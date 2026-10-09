package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral campaign assembler (CAP-VIRAL câblage bout-en-bout):
 * [ViralCampaignAssembler].
 *
 * Verifies the three boundaries meet correctly:
 * - metadata read from [MarketCopySource] (US-9), never re-generated;
 * - one [CampaignBundle] (US-10) per [ViralVariant] (US-6);
 * - degraded metadata when no copy is available (economy of ink).
 */
class ViralCampaignAssemblerTest {

    private fun storyboard() = ViralStoryboard(
        message = "Devenir formateur professionnel",
        angle = "Le métier en 30 secondes",
        beats = listOf(
            StoryboardBeat("hook", "Et si 30 secondes suffisaient ?", 3.0),
            StoryboardBeat("development", "Les 4 activités types", 10.0),
            StoryboardBeat("development", "Le rôle du formateur", 10.0),
            StoryboardBeat("cta", "Découvrez FPA", 7.0),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 30,
    )

    private fun variants() = ViralBatchPlanner.plan(
        languages = listOf("fr", "en"),
        platforms = listOf(ViralPlatform.TIKTOK),
        duration = 30,
    )

    /** In-memory copy source for tests. */
    private class FakeCopySource(private val copies: Map<String, MarketCopy> = emptyMap()) : MarketCopySource {
        override fun read(language: String, platform: ViralPlatform): MarketCopy? =
            copies["$language-${platform.name.lowercase()}"]

        override fun isAvailable(): Boolean = copies.isNotEmpty()
    }

    private fun copy(language: String, platform: ViralPlatform) = MarketCopy(
        language = language,
        platform = platform,
        hook = "H-$language",
        title = "T-$language",
        caption = "C-$language",
        hashtags = listOf("#fpa"),
    )

    @Test
    fun `assemble produces one bundle per variant using the market copy`() {
        val copies = mapOf(
            "fr-tiktok" to copy("fr", ViralPlatform.TIKTOK),
            "en-tiktok" to copy("en", ViralPlatform.TIKTOK),
        )
        val bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard(),
            variants = variants(),
            hook = ViralHook("H", "M", "CTA"),
            copySource = FakeCopySource(copies),
            videoPathFor = { "build/viral/${it.id}.mp4" },
            thumbnailFor = { "build/viral/${it.id}.png" },
        )
        assertEquals(2, bundles.size)
        assertEquals("fr", bundles[0].language)
        assertEquals(ViralPlatform.TIKTOK, bundles[0].platform)
        assertEquals("T-fr", bundles[0].metadata.title)
        assertEquals("T-en", bundles[1].metadata.title)
        assertTrue(bundles.all { it.hasThumbnail })
        assertEquals("build/viral/fr-tiktok-30.mp4", bundles[0].videoFile)
    }

    @Test
    fun `assemble degrades to the hook when no market copy is available`() {
        val bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard(),
            variants = variants(),
            hook = ViralHook("Accroche", "Message", "CTA"),
            copySource = FakeCopySource(),
            videoPathFor = { "build/viral/${it.id}.mp4" },
        )
        assertEquals(2, bundles.size)
        assertEquals("Accroche", bundles[0].metadata.title)
        assertEquals("Message", bundles[0].metadata.caption)
    }

    @Test
    fun `assemble degrades to the storyboard when neither copy nor hook is available`() {
        val bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard(),
            variants = variants(),
            hook = null,
            copySource = FakeCopySource(),
            videoPathFor = { "build/viral/${it.id}.mp4" },
        )
        assertEquals("Devenir formateur professionnel", bundles[0].metadata.title)
        assertEquals("Le métier en 30 secondes", bundles[0].metadata.caption)
    }

    @Test
    fun `assemble returns an empty list for an empty batch`() {
        val bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard(),
            variants = emptyList(),
            hook = null,
            copySource = FakeCopySource(),
            videoPathFor = { it.id },
        )
        assertTrue(bundles.isEmpty())
    }
}
