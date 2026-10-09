package capsule.viral

/**
 * Pure planner of the viral campaign batch matrix (CAP-VIRAL US-6).
 *
 * Produces the cartesian product (languages × platforms × duration) as an
 * ordered list of [ViralVariant]s. The order is deterministic — language-major,
 * then platform, then duration — so a run is reproducible and the
 * economy-of-ink skip keys on stable IDs.
 *
 * Degradation: blank/empty languages or platforms yield an empty plan (the
 * caller treats it as a no-op); a single duration outside [15, 60] is clamped
 * via [ViralConfig.effectiveDurationSeconds]'s range, but the caller is expected
 * to pass an already-coerced duration.
 *
 * Pure — no Gradle, no I/O.
 */
object ViralBatchPlanner {

    /**
     * Builds the campaign variant matrix.
     *
     * @param languages  target ISO language codes (order preserved).
     * @param platforms  target platform presets (order preserved).
     * @param duration   short-form duration in [15, 60].
     * @return the ordered, de-duplicated list of variants.
     */
    fun plan(
        languages: List<String>,
        platforms: List<ViralPlatform>,
        duration: Int,
    ): List<ViralVariant> {
        val cleanLanguages = languages.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val cleanPlatforms = platforms.distinct()
        if (cleanLanguages.isEmpty() || cleanPlatforms.isEmpty()) return emptyList()
        val coerced = duration.coerceIn(ViralConfig.MIN_DURATION_SECONDS, ViralConfig.MAX_DURATION_SECONDS)
        return cleanLanguages.flatMap { language ->
            cleanPlatforms.map { platform ->
                ViralVariant(language = language, platform = platform, targetDuration = coerced)
            }
        }
    }
}
