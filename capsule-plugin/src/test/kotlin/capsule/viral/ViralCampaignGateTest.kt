package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral campaign delivery gate (CAP-VIRAL-E2E US-1):
 * the last-mile contract that every asset referenced by the campaign manifest
 * exists, plus the manifest round-trip parser it reads.
 */
class ViralCampaignGateTest {

    private fun bundle(
        language: String = "fr",
        platform: ViralPlatform = ViralPlatform.TIKTOK,
        video: String = "build/viral/fr-tiktok-30.mp4",
        thumbnail: String = "",
        subtitle: String = "",
    ) = CampaignBundle(
        language = language,
        platform = platform,
        videoFile = video,
        thumbnailFile = thumbnail,
        subtitleFile = subtitle,
        metadata = MarketCopy(
            language = language,
            platform = platform,
            hook = "hook",
            title = "title",
            caption = "caption",
        ),
    )

    // ─── gate ─────────────────────────────────────────────────────────

    @Test
    fun `valid when every referenced video exists`() {
        val result = ViralCampaignGate.verdict(listOf(bundle())) { it == "build/viral/fr-tiktok-30.mp4" }
        assertEquals(ViralCampaignGateResult.Valid, result)
    }

    @Test
    fun `invalid lists a missing video with the variant descriptor`() {
        val result = ViralCampaignGate.verdict(listOf(bundle())) { false }
        assertTrue(result is ViralCampaignGateResult.Invalid)
        assertEquals(
            listOf("fr/TIKTOK videoFile: build/viral/fr-tiktok-30.mp4"),
            result.missing,
        )
    }

    @Test
    fun `invalid lists a missing non-blank thumbnail`() {
        val b = bundle(thumbnail = "build/viral/fr-tiktok-30.png")
        val result = ViralCampaignGate.verdict(listOf(b)) { it.endsWith(".mp4") }
        assertTrue(result is ViralCampaignGateResult.Invalid)
        assertEquals(
            listOf("fr/TIKTOK thumbnailFile: build/viral/fr-tiktok-30.png"),
            result.missing,
        )
    }

    @Test
    fun `a blank thumbnail is not a violation`() {
        val result = ViralCampaignGate.verdict(listOf(bundle(thumbnail = ""))) { it.endsWith(".mp4") }
        assertEquals(ViralCampaignGateResult.Valid, result)
    }

    @Test
    fun `invalid lists a missing non-blank subtitle`() {
        val b = bundle(subtitle = "build/viral/fr-tiktok-30.ass")
        val result = ViralCampaignGate.verdict(listOf(b)) { it.endsWith(".mp4") }
        assertTrue(result is ViralCampaignGateResult.Invalid)
        assertEquals(
            listOf("fr/TIKTOK subtitleFile: build/viral/fr-tiktok-30.ass"),
            result.missing,
        )
    }

    @Test
    fun `a blank subtitle is not a violation`() {
        val result = ViralCampaignGate.verdict(listOf(bundle(subtitle = ""))) { it.endsWith(".mp4") }
        assertEquals(ViralCampaignGateResult.Valid, result)
    }

    @Test
    fun `aggregates every missing asset in bundle order`() {
        val bundles = listOf(
            bundle(language = "fr"),
            bundle(language = "en", platform = ViralPlatform.YOUTUBE_SHORT, video = "build/viral/en-yt.mp4"),
        )
        val result = ViralCampaignGate.verdict(bundles) { false }
        assertEquals(
            listOf(
                "fr/TIKTOK videoFile: build/viral/fr-tiktok-30.mp4",
                "en/YOUTUBE_SHORT videoFile: build/viral/en-yt.mp4",
            ),
            (result as ViralCampaignGateResult.Invalid).missing,
        )
    }

    @Test
    fun `an empty campaign is valid`() {
        assertEquals(ViralCampaignGateResult.Valid, ViralCampaignGate.verdict(emptyList()) { false })
    }

    @Test
    fun `Invalid result requires at least one missing asset`() {
        assertTrue(runCatching { ViralCampaignGateResult.Invalid(emptyList()) }.isFailure)
    }

    // ─── manifest round-trip ──────────────────────────────────────────

    @Test
    fun `manifest serialises then parses back to the same bundles`() {
        val bundles = listOf(
            bundle(language = "fr", thumbnail = "build/viral/fr-tiktok-30.png", subtitle = "build/viral/fr-tiktok-30.ass"),
            bundle(language = "en", platform = ViralPlatform.YOUTUBE_SHORT, video = "build/viral/en-yt.mp4"),
        )
        val parsed = CampaignBundleSerializer.parseManifest(CampaignBundleSerializer.toManifestJson(bundles))
        assertEquals(bundles, parsed)
    }

    @Test
    fun `an empty manifest parses to an empty campaign`() {
        assertEquals(emptyList(), CampaignBundleSerializer.parseManifest("[]"))
    }

    @Test
    fun `a non-array manifest is rejected`() {
        assertTrue(runCatching { CampaignBundleSerializer.parseManifest("{}") }.isFailure)
    }
}
