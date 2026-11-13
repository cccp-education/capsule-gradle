package capsule.audio.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * HF-MUSIC-LIBRARY US-6 — deterministic library selection for capsule audio post.
 */
class MusicTrackSelectorTest {
    private val calm =
        MusicTrack(id = "calm-piano", title = "Calm piano", file = "calm/piano.mp3", licence = MusicLicence.OWN, mood = "calm")
    private val epic =
        MusicTrack(id = "epic-drums", title = "Epic drums", file = "epic/drums.mp3", licence = MusicLicence.OWN, mood = "epic")
    private val unusable =
        MusicTrack(id = "unknown-track", title = "Unknown", file = "x.mp3", licence = MusicLicence.UNKNOWN, mood = "calm")
    private val library = MusicLibrary(tracks = listOf(epic, calm, unusable))

    @Test
    fun `an explicit track id wins`() {
        val selection = MusicTrackSelector.select(library, trackId = "epic-drums", mood = "calm")

        assertIs<MusicSelection.Selected>(selection)
        assertEquals("epic-drums", selection.track.id)
    }

    @Test
    fun `a mood selects the first usable track by id`() {
        val selection = MusicTrackSelector.select(library, mood = "calm")

        assertIs<MusicSelection.Selected>(selection)
        assertEquals("calm-piano", selection.track.id)
    }

    @Test
    fun `no selection falls back to any usable track`() {
        val selection = MusicTrackSelector.select(library)

        assertIs<MusicSelection.Selected>(selection)
        assertEquals("calm-piano", selection.track.id)
    }

    @Test
    fun `an unknown track id is rejected never substituted`() {
        val selection = MusicTrackSelector.select(library, trackId = "missing")

        assertIs<MusicSelection.Rejected>(selection)
        assertTrue(selection.reason.contains("missing"))
    }

    @Test
    fun `an unusable licence is rejected`() {
        val selection = MusicTrackSelector.select(library, trackId = "unknown-track")

        assertIs<MusicSelection.Rejected>(selection)
        assertTrue(selection.reason.contains("not usable"))
    }

    @Test
    fun `an unknown mood is rejected`() {
        val selection = MusicTrackSelector.select(library, mood = "dreamy")

        assertIs<MusicSelection.Rejected>(selection)
        assertTrue(selection.reason.contains("dreamy"))
    }
}
