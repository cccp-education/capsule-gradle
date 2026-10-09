package capsule.viral

/**
 * Immutable contract handed to the distribution layer (CAP-VIRAL US-10).
 *
 * The `CampaignBundle` is the **passation contract** from capsule to GBL-002
 * (`publishToYouTube` / `publishToTikTok`): it carries everything a publisher
 * needs for one (language × platform) campaign cell — the rendered [videoFile],
 * the [metadata] (title/caption/hashtags), the [thumbnailFile], the [language]
 * and the [platform]. The distribution layer consumes it; it never re-crops the
 * video (the 9:16 vertical is produced here by US-2) and never re-generates the
 * metadata (read from bakery, US-9) — the split-brain is eliminated.
 *
 * Invariants (fail-fast):
 * - [videoFile] must not be blank.
 * - [language] must not be blank.
 *
 * [thumbnailFile] may be blank (a render without thumbnail is valid but weaker);
 * [metadata] may be a degraded [MarketCopy] fallback.
 *
 * @property language      target ISO language code.
 * @property platform      target platform preset.
 * @property videoFile     path to the rendered short-form vertical video.
 * @property thumbnailFile path to the cover/thumbnail (may be blank).
 * @property metadata      the platform metadata (title/caption/hashtags).
 */
data class CampaignBundle(
    val language: String,
    val platform: ViralPlatform,
    val videoFile: String,
    val thumbnailFile: String,
    val metadata: MarketCopy,
) {
    init {
        require(language.isNotBlank()) { "CampaignBundle.language must not be blank" }
        require(videoFile.isNotBlank()) { "CampaignBundle.videoFile must not be blank" }
    }

    /** `true` when a thumbnail asset is carried. */
    val hasThumbnail: Boolean get() = thumbnailFile.isNotBlank()
}
