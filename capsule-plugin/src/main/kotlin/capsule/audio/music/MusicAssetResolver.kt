package capsule.audio.music

/**
 * One library asset resolved to real paths (HF-MUSIC-LIBRARY US-6).
 *
 * @property trackId stable library id (diagnostic + attribution).
 * @property sourcePath absolute-or-relative path of the audio file to copy.
 * @property targetName file name the BGM should take in the build output
 *   (`<track-id>.<ext>`), so a build is reproducible.
 * @property sha256 expected content hash from the manifest (blank = unknown).
 * @property requiresAttribution `true` when the licence obliges a credit line.
 * @property attribution credit line supplied by the manifest (may be blank).
 */
data class ResolvedMusicAsset(
    val trackId: String,
    val sourcePath: String,
    val targetName: String,
    val sha256: String = "",
    val requiresAttribution: Boolean = false,
    val attribution: String = "",
)

/**
 * Outcome of resolving a library selection to a usable BGM asset
 * (HF-MUSIC-LIBRARY US-6).
 */
sealed interface MusicResolution {
    /** A usable asset was resolved. */
    data class Resolved(
        val asset: ResolvedMusicAsset,
    ) : MusicResolution

    /** Nothing can be used; [reason] is actionable and secret-free. */
    data class Rejected(
        val reason: String,
    ) : MusicResolution
}

/**
 * Resolves the capsule audio-post library selection
 * (HF-MUSIC-LIBRARY US-6 / cadrage D8).
 *
 * Pure: the manifest is injected, the file system is not touched. The same
 * configuration resolves to the same asset (deterministic selector).
 */
object MusicAssetResolver {
    /**
     * Resolves the selection [trackId]/[mood] against [library].
     *
     * @param audioRoot directory the manifest `file` paths are relative to
     *   (the manifest directory, in practice).
     */
    fun resolve(
        library: MusicLibrary,
        audioRoot: String,
        trackId: String = "",
        mood: String = "",
    ): MusicResolution =
        when (val selection = MusicTrackSelector.select(library, trackId, mood)) {
            is MusicSelection.Rejected -> MusicResolution.Rejected(selection.reason)
            is MusicSelection.Selected -> {
                val track = selection.track
                val extension =
                    track.file
                        .substringAfterLast('.', "")
                        .lowercase()
                        .ifBlank { "mp3" }
                MusicResolution.Resolved(
                    ResolvedMusicAsset(
                        trackId = track.id,
                        sourcePath = join(audioRoot, track.file),
                        targetName = "${track.id}.$extension",
                        sha256 = track.sha256,
                        requiresAttribution = track.requiresAttribution(),
                        attribution = track.attribution,
                    ),
                )
            }
        }

    /**
     * `true` when the target must be (re)copied: expected hash unknown, target
     * absent, or content differs — economy of ink when `sha256` matches.
     */
    fun needsCopy(
        existingSha256: String?,
        expectedSha256: String,
    ): Boolean =
        expectedSha256.isBlank() ||
            existingSha256 == null ||
            !existingSha256.equals(expectedSha256, ignoreCase = true)

    private fun join(
        root: String,
        file: String,
    ): String {
        val cleanRoot = root.trim().trimEnd('/', '\\')
        val cleanFile = file.trim().trimStart('/', '\\')
        return if (cleanRoot.isEmpty()) cleanFile else "$cleanRoot/$cleanFile"
    }
}
