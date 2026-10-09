package capsule.viral

/**
 * One variant of a viral campaign: a (language × platform) target (CAP-VIRAL US-6).
 *
 * The batch matrix produces one entry per cell; the campaign task then renders
 * one short-form video per entry. The identifier is stable and deterministic so
 * the economy-of-ink skip (artifact already exists and is valid) can key on it.
 *
 * @property language       ISO language code (e.g. "fr").
 * @property platform       target platform preset.
 * @property targetDuration the short-form duration in [15, 60].
 */
data class ViralVariant(
    val language: String,
    val platform: ViralPlatform,
    val targetDuration: Int,
) {
    init {
        require(language.isNotBlank()) { "ViralVariant.language must not be blank" }
        require(targetDuration in ViralConfig.MIN_DURATION_SECONDS..ViralConfig.MAX_DURATION_SECONDS) {
            "ViralVariant.targetDuration must be in [${ViralConfig.MIN_DURATION_SECONDS}, ${ViralConfig.MAX_DURATION_SECONDS}], got $targetDuration"
        }
    }

    /** Deterministic variant id, e.g. `fr-tiktok-30`. */
    val id: String get() = "$language-${platform.name.lowercase()}-$targetDuration"
}
