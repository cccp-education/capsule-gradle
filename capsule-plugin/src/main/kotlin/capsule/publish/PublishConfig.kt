package capsule.publish

/**
 * Configuration section for the YouTube publication feature
 * (HF-YOUTUBE-PUBLISH US-3b).
 *
 * When [enabled] is true, `publishCapsuleToYouTube` uploads the produced MP4 to
 * the configured channel through the shared N1 `video.kernel.publish.VideoPublisher`
 * port (OAuth 2.0). Disabled by default — backward compatible.
 *
 * Resolution follows the 4-source precedence:
 * ENV (`CAPSULE_PUBLISH_*`) < gradle.properties (`capsule.publish.*`) < YAML
 * (`publish.*`) < CLI (`-Pcapsule.publish.*`).
 *
 * @param enabled         `true` to publish (default `false`).
 * @param platform        target platform (default `youtube`).
 * @param credentialsFile gitignored credentials file (default `youtube.yaml`).
 * @param title           video title (≤ 100 characters).
 * @param description     base description body.
 * @param tags            keyword tags.
 * @param hashtags        hashtags rendered in the description.
 * @param links           identity links, as `Label=url` entries.
 * @param nextVideo       URL of the next video in the series.
 * @param previousVideo   URL of the previous video in the series.
 * @param thumbnailFile   optional thumbnail image path.
 * @param visibility      `private` (default), `unlisted` or `public`.
 * @param categoryId      platform category id (e.g. `27` = Education).
 * @param languageCode    BCP-47 language tag (e.g. `fr`).
 * @param publishAt       optional ISO-8601 instant to schedule the publication.
 * @param strict          promote a validation failure / API refusal to a failure.
 */
data class PublishConfig(
    val enabled: Boolean = false,
    val platform: String = "youtube",
    val credentialsFile: String = "youtube.yaml",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val hashtags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val nextVideo: String = "",
    val previousVideo: String = "",
    val thumbnailFile: String = "",
    val visibility: String = "private",
    val categoryId: String = "",
    val languageCode: String = "",
    val publishAt: String = "",
    val strict: Boolean = false,
)
