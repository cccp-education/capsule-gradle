package capsule.audio.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * HF-MUSIC-LIBRARY US-6 — resolution of a library selection to a capsule BGM asset.
 */
class MusicAssetResolverTest {
    private val track =
        MusicTrack(
            id = "calm-piano",
            title = "Calm piano",
            file = "calm/piano.mp3",
            sha256 = "abc123",
            licence = MusicLicence.CC_BY,
            attribution = "Cheroliv — CC BY 4.0",
            mood = "calm",
        )
    private val library = MusicLibrary(tracks = listOf(track))

    @Test
    fun `a track id resolves to the source, target name and attribution`() {
        val resolution = MusicAssetResolver.resolve(library, audioRoot = "/lib/audio", trackId = "calm-piano")

        assertIs<MusicResolution.Resolved>(resolution)
        val asset = resolution.asset
        assertEquals("/lib/audio/calm/piano.mp3", asset.sourcePath)
        assertEquals("calm-piano.mp3", asset.targetName)
        assertEquals("abc123", asset.sha256)
        assertTrue(asset.requiresAttribution)
        assertEquals("Cheroliv — CC BY 4.0", asset.attribution)
    }

    @Test
    fun `a mood resolves deterministically`() {
        val resolution = MusicAssetResolver.resolve(library, audioRoot = "/lib", mood = "calm")

        assertIs<MusicResolution.Resolved>(resolution)
        assertEquals("calm-piano", resolution.asset.trackId)
    }

    @Test
    fun `a file without extension defaults to mp3`() {
        val noExtension = MusicTrack(id = "bed", title = "Bed", file = "bed", licence = MusicLicence.OWN)

        val resolution =
            MusicAssetResolver.resolve(MusicLibrary(tracks = listOf(noExtension)), audioRoot = "/lib", trackId = "bed")

        assertIs<MusicResolution.Resolved>(resolution)
        assertEquals("bed.mp3", resolution.asset.targetName)
    }

    @Test
    fun `an unknown track is rejected`() {
        val resolution = MusicAssetResolver.resolve(library, audioRoot = "/lib", trackId = "missing")

        assertIs<MusicResolution.Rejected>(resolution)
        assertTrue(resolution.reason.contains("missing"))
    }

    @Test
    fun `an empty audio root yields the relative path`() {
        val resolution = MusicAssetResolver.resolve(library, audioRoot = "", trackId = "calm-piano")

        assertIs<MusicResolution.Resolved>(resolution)
        assertEquals("calm/piano.mp3", resolution.asset.sourcePath)
    }

    @Test
    fun `copy is needed when the target is absent or differs`() {
        assertTrue(MusicAssetResolver.needsCopy(existingSha256 = null, expectedSha256 = "abc123"))
        assertTrue(MusicAssetResolver.needsCopy(existingSha256 = "other", expectedSha256 = "abc123"))
        assertTrue(MusicAssetResolver.needsCopy(existingSha256 = "abc123", expectedSha256 = ""))
        assertFalse(MusicAssetResolver.needsCopy(existingSha256 = "ABC123", expectedSha256 = "abc123"))
    }
}
