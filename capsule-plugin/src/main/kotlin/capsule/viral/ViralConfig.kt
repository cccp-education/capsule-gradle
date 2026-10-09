package capsule.viral

/**
 * Configuration section for the viral campaign engine (CAP-VIRAL US-3).
 *
 * When [enabled] is true, the `generateViralCampaign` task produces
 * short-form vertical videos (YouTube Shorts / TikTok / Reels) from a
 * validated storyboard, using the deterministic FFmpeg assembly as the
 * socle and Remotion + Manim as the content-enrichment layer.
 *
 * All fields default to disabled/default values to preserve backward
 * compatibility — existing configs without a `viral` section keep the
 * pedagogical-only behavior.
 *
 * Resolution follows the 4-source precedence:
 * ENV (`CAPSULE_VIRAL_*`) < gradle.properties (`capsule.viral.*`)
 * < YAML (`viral.*`) < CLI (`-Pcapsule.viral.*`).
 *
 * @param enabled                `true` to enable viral campaign generation
 *        (default `false` — backward compat, opt-in).
 * @param platform               target platform preset
 *        (default [ViralPlatform.TIKTOK] — 9:16 vertical, 15-60 s).
 * @param targetDurationSeconds  target video duration in seconds, clamped
 *        to the 15-60 s short-form range by the domain (default `30`).
 * @param hook                   opening 3-second hook / message angle
 *        (default empty — must be provided by the storyboard or the
 *        context-anchored LLM step).
 * @param cta                    closing call-to-action (default empty).
 * @param storyboardRequired     when `true`, no video is rendered before a
 *        validated storyboard exists (editorial gate, default `true`).
 * @param storyboardFile         path to the storyboard AsciiDoc
 *        (default empty — resolved per campaign).
 */
data class ViralConfig(
    val enabled: Boolean = false,
    val platform: ViralPlatform = ViralPlatform.TIKTOK,
    val targetDurationSeconds: Int = DEFAULT_DURATION_SECONDS,
    val hook: String = "",
    val cta: String = "",
    val storyboardRequired: Boolean = true,
    val storyboardFile: String = ""
) {

    /** [targetDurationSeconds] coerced into the short-form range [15, 60]. */
    val effectiveDurationSeconds: Int
        get() = targetDurationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)

    companion object {
        /** Shortest accepted short-form duration (seconds). */
        const val MIN_DURATION_SECONDS: Int = 15

        /** Longest accepted short-form duration (seconds). */
        const val MAX_DURATION_SECONDS: Int = 60

        /** Default target duration (seconds). */
        const val DEFAULT_DURATION_SECONDS: Int = 30
    }
}

/**
 * Target platform preset for a viral campaign (CAP-VIRAL US-3).
 *
 * All three platforms consume 1080×1920 (9:16) vertical short-form video;
 * the preset carries the platform identity used by the metadata and the
 * `CampaignBundle` contract, not a different encoding.
 *
 * - [YOUTUBE_SHORT] — YouTube Shorts (≤ 60 s, 9:16).
 * - [TIKTOK] — TikTok (15-60 s, 9:16). Default.
 * - [REELS] — Instagram Reels (≤ 90 s, 9:16; viral preset capped at 60 s).
 */
enum class ViralPlatform {
    YOUTUBE_SHORT,
    TIKTOK,
    REELS;

    companion object {
        /**
         * Case-insensitive parse. Falls back to [TIKTOK] for
         * null/blank/unknown values (backward compat — existing configs
         * without `viral.platform` keep the TikTok default).
         */
        fun fromString(value: String?): ViralPlatform =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TIKTOK
    }
}
