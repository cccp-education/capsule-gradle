package capsule.viral

/**
 * Verdict of the viral campaign delivery gate (CAP-VIRAL-E2E US-1).
 *
 * The campaign manifest references, per bundle, a rendered [CampaignBundle.videoFile]
 * and an optional [CampaignBundle.thumbnailFile]. This result states whether every
 * referenced asset actually exists on disk.
 */
sealed class ViralCampaignGateResult {

    /** Every referenced asset exists. */
    object Valid : ViralCampaignGateResult()

    /**
     * At least one referenced asset is missing.
     *
     * @property missing human-readable descriptors of the missing assets
     *                   (`<language>/<platform> videoFile: <path>`), in bundle order.
     */
    data class Invalid(val missing: List<String>) : ViralCampaignGateResult() {
        init {
            require(missing.isNotEmpty()) { "ViralCampaignGateResult.Invalid must carry at least one missing asset" }
        }
    }
}

/**
 * Pure delivery gate for a viral campaign manifest (CAP-VIRAL-E2E US-1).
 *
 * The `CampaignBundle.videoFile` is a *constructed* path: `generateViralCampaign`
 * writes the manifest before `renderViralVariants` produces the MP4s. Without a
 * gate, a campaign can be declared delivered while its videos never existed — the
 * distribution layer (GBL-002) would then push a broken campaign.
 *
 * This object is the last-mile contract: given the assembled [CampaignBundle]s and
 * an existence predicate, it reports every referenced asset that is missing. It
 * checks the mandatory [CampaignBundle.videoFile] and, when non-blank, the optional
 * [CampaignBundle.thumbnailFile] and [CampaignBundle.subtitleFile] (a blank optional
 * asset is a valid, weaker render — not a violation).
 *
 * Pure — no I/O of its own (the existence predicate is injected), no Gradle.
 */
object ViralCampaignGate {

    /**
     * Computes the verdict for [bundles].
     *
     * @param bundles the campaign bundles read from the manifest.
     * @param exists  decides whether a referenced path is a valid, non-empty asset.
     * @return [ViralCampaignGateResult.Valid] or [ViralCampaignGateResult.Invalid]
     *         listing every missing asset in bundle order.
     */
    fun verdict(
        bundles: List<CampaignBundle>,
        exists: (String) -> Boolean,
    ): ViralCampaignGateResult {
        val missing = bundles.flatMap { bundle ->
            buildList {
                if (!exists(bundle.videoFile)) {
                    add("${bundle.language}/${bundle.platform.name} videoFile: ${bundle.videoFile}")
                }
                if (bundle.thumbnailFile.isNotBlank() && !exists(bundle.thumbnailFile)) {
                    add("${bundle.language}/${bundle.platform.name} thumbnailFile: ${bundle.thumbnailFile}")
                }
                if (bundle.subtitleFile.isNotBlank() && !exists(bundle.subtitleFile)) {
                    add("${bundle.language}/${bundle.platform.name} subtitleFile: ${bundle.subtitleFile}")
                }
            }
        }
        return if (missing.isEmpty()) ViralCampaignGateResult.Valid else ViralCampaignGateResult.Invalid(missing)
    }
}
