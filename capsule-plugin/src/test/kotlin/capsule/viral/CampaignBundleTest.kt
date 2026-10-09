package capsule.viral

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the campaign bundle contract (CAP-VIRAL US-10):
 * [CampaignBundle] + [CampaignBundleSerializer].
 *
 * The bundle is the passation contract capsule → GBL-002. It carries the video,
 * the metadata (read from bakery, US-9) and the thumbnail; the distribution
 * layer never re-crops nor re-generates.
 */
class CampaignBundleTest {

    private fun copy() = MarketCopy(
        language = "fr",
        platform = ViralPlatform.TIKTOK,
        hook = "Et si 30 secondes suffisaient ?",
        title = "Devenir formateur",
        caption = "La formation FPA en accéléré",
        hashtags = listOf("#fpa", "#formation"),
    )

    private fun bundle(thumbnail: String = "thumb.png") = CampaignBundle(
        language = "fr",
        platform = ViralPlatform.TIKTOK,
        videoFile = "build/capsule/fr-tiktok-30.mp4",
        thumbnailFile = thumbnail,
        metadata = copy(),
    )

    @Test
    fun `bundle exposes thumbnail presence`() {
        assertTrue(bundle().hasThumbnail)
        assertFalse(bundle(thumbnail = "").hasThumbnail)
    }

    @Test
    fun `bundle rejects blank video or language`() {
        assertTrue(runCatching { bundle().copy(videoFile = "") }.isFailure)
        assertTrue(runCatching { bundle().copy(language = "") }.isFailure)
    }

    @Test
    fun `serializer produces the stable json contract`() {
        val json = CampaignBundleSerializer.toJson(bundle())
        val parsed = ObjectMapper().readTree(json)
        assertEquals("fr", parsed.get("language").asText())
        assertEquals("TIKTOK", parsed.get("platform").asText())
        assertEquals("build/capsule/fr-tiktok-30.mp4", parsed.get("videoFile").asText())
        assertEquals("thumb.png", parsed.get("thumbnailFile").asText())
        val metadata = parsed.get("metadata")
        assertEquals("Devenir formateur", metadata.get("title").asText())
        assertEquals(2, metadata.get("hashtags").size())
        assertEquals("#fpa", metadata.get("hashtags").get(0).asText())
    }

    @Test
    fun `serializer is deterministic`() {
        assertEquals(CampaignBundleSerializer.toJson(bundle()), CampaignBundleSerializer.toJson(bundle()))
    }

    @Test
    fun `serializer round-trips the metadata`() {
        val parsed = ObjectMapper().readTree(CampaignBundleSerializer.toJson(bundle()))
        assertEquals(copy().hook, parsed.get("metadata").get("hook").asText())
        assertEquals(copy().caption, parsed.get("metadata").get("caption").asText())
    }

    @Test
    fun `manifest serializer produces a deterministic json array of bundles`() {
        val bundles = listOf(bundle(), bundle(thumbnail = ""))
        val json = CampaignBundleSerializer.toManifestJson(bundles)
        val parsed = ObjectMapper().readTree(json)
        assertTrue(parsed.isArray)
        assertEquals(2, parsed.size())
        assertEquals("fr", parsed.get(0).get("language").asText())
        assertEquals("build/capsule/fr-tiktok-30.mp4", parsed.get(0).get("videoFile").asText())
        assertEquals(json, CampaignBundleSerializer.toManifestJson(bundles), "manifest must be deterministic")
    }

    @Test
    fun `manifest serializer produces an empty array for an empty campaign`() {
        val parsed = ObjectMapper().readTree(CampaignBundleSerializer.toManifestJson(emptyList()))
        assertTrue(parsed.isArray)
        assertEquals(0, parsed.size())
    }
}
