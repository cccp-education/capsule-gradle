package capsule.viral

/**
 * Pure assembler of a viral campaign (CAP-VIRAL — câblage bout-en-bout).
 *
 * Wires the domain bricks built across CAP-VIRAL into the final campaign:
 * for each [ViralVariant] of the batch plan, it picks the hook (context-anchored
 * [ViralHook], falling back to the storyboard message), reads the market copy
 * ([MarketCopy] from bakery, falling back to the hook as a degraded metadata) and
 * produces the [CampaignBundle] that carries the rendered video and its metadata.
 *
 * The assembler is **pure** — it does not render, read files, or call the LLM. It
 * is the deterministic join that the Gradle task drives: the task supplies the
 * per-language rendered video paths and the [MarketCopySource], the assembler
 * produces the bundles.
 *
 * This is where the three boundaries meet:
 * - US-9: metadata **read** from GBL-006 ([MarketCopySource]) — never re-generated;
 * - US-2: the video is the rendered vertical (path supplied per variant);
 * - US-10: the output is the [CampaignBundle] contract for GBL-002.
 */
object ViralCampaignAssembler {

    /**
     * Assembles the campaign bundles.
     *
     * @param storyboard    the validated storyboard (message/angle/beats).
     * @param variants      he batch variants (language × platform × duration).
     * @param hook          the context-anchored hook (may be null when the LLM
     *                      gave nothing usable — the storyboard message is used).
     * @param copySource    the market copy source (bakery GBL-006).
     * @param videoPathFor  maps a variant id to the rendered video path.
     * @param thumbnailFor  maps a variant id to the thumbnail path (may return blank).
     * @return one [CampaignBundle] per variant, in variant order.
     */
    fun assemble(
        storyboard: ViralStoryboard,
        variants: List<ViralVariant>,
        hook: ViralHook?,
        copySource: MarketCopySource,
        videoPathFor: (ViralVariant) -> String,
        thumbnailFor: (ViralVariant) -> String = { "" },
    ): List<CampaignBundle> = variants.map { variant ->
        val metadata = copySource.read(variant.language, variant.platform)
            ?: degradedMetadata(variant, storyboard, hook)
        CampaignBundle(
            language = variant.language,
            platform = variant.platform,
            videoFile = videoPathFor(variant),
            thumbnailFile = thumbnailFor(variant),
            metadata = metadata,
        )
    }

    /**
     * Degraded metadata when no market copy is available: the hook becomes both
     * the title and the caption, so the bundle still carries a usable message
     * (the campaign is never blocked by a missing copy — economy of ink).
     */
    private fun degradedMetadata(
        variant: ViralVariant,
        storyboard: ViralStoryboard,
        hook: ViralHook?,
    ): MarketCopy {
        val title = hook?.hook?.takeIf { it.isNotBlank() } ?: storyboard.message
        val caption = hook?.message?.takeIf { it.isNotBlank() } ?: storyboard.angle
        return MarketCopy(
            language = variant.language,
            platform = variant.platform,
            hook = hook?.hook.orEmpty(),
            title = title,
            caption = caption,
        )
    }
}
