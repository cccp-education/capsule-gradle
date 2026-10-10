package capsule.viral

import java.io.File

/**
 * One (variant → output video) pair of a viral campaign render (CAP-SHORT US-1b).
 *
 * @property variant   the target of the render.
 * @property videoFile  `<variant.id>.mp4` under the rendered video directory.
 */
data class ViralVariantRenderEntry(
    val variant: ViralVariant,
    val videoFile: File,
)

/**
 * Ordered render plan for a viral campaign (CAP-SHORT US-1b): one MP4 per
 * variant, in the batch matrix order.
 */
data class ViralVariantRenderPlan(val entries: List<ViralVariantRenderEntry>) {
    val size: Int get() = entries.size
    val isEmpty: Boolean get() = entries.isEmpty()

    /** Variant ids in plan order. */
    val ids: List<String> get() = entries.map { it.variant.id }
}

/**
 * Outcome of a render pass: which variants were produced, skipped (economy of
 * ink) or failed.
 */
data class ViralRenderResult(
    val rendered: List<String>,
    val skipped: List<String>,
    val failed: List<String>,
) {
    val renderedCount: Int get() = rendered.size
    val skippedCount: Int get() = skipped.size
    val failedCount: Int get() = failed.size
}

/**
 * Pure orchestration of the viral short-form render (CAP-SHORT US-1b + US-3).
 *
 * The campaign domain (CAP-VIRAL) knows the (language × platform) matrix; the
 * FFmpeg socle knows how to capture a deck. This object links them: it names one
 * MP4 per variant, skips already-rendered ones (economy of ink), and drives the
 * batch through two injected seams.
 *
 * **Batch (US-3) — capture once, replicate**: every variant shares the same
 * portrait deck and durations, so the source is captured **once** and the
 * resulting video is replicated to each variant's `<id>.mp4`. This is the
 * economy-of-ink win of a real batch (one capture for N variants, not N
 * captures).
 *
 * Pure — no Gradle, no I/O of its own (the seams do the work; the validity
 * predicate is injected).
 */
object ViralVariantRenderer {

    /** Container of the per-variant short-form video. */
    const val VIDEO_EXTENSION: String = "mp4"

    /**
     * Names the per-variant output file `<variant.id>.mp4` under [renderedVideoDir].
     */
    fun plan(variants: List<ViralVariant>, renderedVideoDir: File): ViralVariantRenderPlan =
        ViralVariantRenderPlan(
            variants.map { variant ->
                ViralVariantRenderEntry(
                    variant = variant,
                    videoFile = File(renderedVideoDir, "${variant.id}.$VIDEO_EXTENSION"),
                )
            },
        )

    /**
     * Entries whose video is not yet renderable — the delta to produce
     * (economy of ink). [isRenderable] decides whether an existing file is a
     * valid result to reuse.
     */
    fun pending(
        plan: ViralVariantRenderPlan,
        isRenderable: (File) -> Boolean,
    ): List<ViralVariantRenderEntry> = plan.entries.filterNot { isRenderable(it.videoFile) }

    /**
     * Drives the batch render over [plan]:
     *  - skips renderable videos (economy of ink);
     *  - captures the shared source **once** into the first pending variant
     *    (`capture`);
     *  - replicates that video to every other pending variant (`replicate`).
     *
     * @param capture   renders the source into [target] (`true` on success).
     * @param replicate copies a produced video to another variant (`true` on success).
     */
    fun render(
        plan: ViralVariantRenderPlan,
        isRenderable: (File) -> Boolean,
        capture: (target: File) -> Boolean,
        replicate: (source: File, target: File) -> Boolean,
    ): ViralRenderResult {
        val rendered = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()

        val pending = plan.entries.filterNot { entry ->
            if (isRenderable(entry.videoFile)) {
                skipped += entry.variant.id
                true
            } else {
                false
            }
        }
        if (pending.isEmpty()) return ViralRenderResult(rendered, skipped, failed)

        val primary = pending.first()
        if (!capture(primary.videoFile)) {
            failed += pending.map { it.variant.id }
            return ViralRenderResult(rendered, skipped, failed)
        }
        rendered += primary.variant.id

        pending.drop(1).forEach { entry ->
            if (replicate(primary.videoFile, entry.videoFile)) {
                rendered += entry.variant.id
            } else {
                failed += entry.variant.id
            }
        }
        return ViralRenderResult(rendered, skipped, failed)
    }
}
