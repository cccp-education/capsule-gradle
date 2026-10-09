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
 * Renders one variant to [ViralVariantRenderEntry.videoFile].
 *
 * The heavy capture + FFmpeg socle lives behind this seam so the orchestration
 * itself stays pure and unit-testable (the Gradle task supplies the real
 * engine; tests supply a fake).
 */
fun interface ViralVariantRender {
    /** @return `true` when the variant video was produced. */
    fun render(entry: ViralVariantRenderEntry): Boolean
}

/**
 * Pure orchestration of the viral short-form render (CAP-SHORT US-1b).
 *
 * The campaign domain (CAP-VIRAL) knows the (language × platform) matrix; the
 * FFmpeg socle knows how to capture a deck. This object is the *column that
 * links them*: it names one MP4 per variant, skips the ones already rendered
 * (economy of ink), and drives the render through the injected [ViralVariantRender].
 *
 * Pure — no Gradle, no I/O of its own (the [ViralVariantRender] does the work,
 * and the validity predicate is injected).
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
     * Drives the render over [plan]: skips renderable videos, renders the rest
     * via [render], and accumulates the outcome by variant id.
     */
    fun render(
        plan: ViralVariantRenderPlan,
        isRenderable: (File) -> Boolean,
        render: ViralVariantRender,
    ): ViralRenderResult {
        val rendered = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        val failed = mutableListOf<String>()
        plan.entries.forEach { entry ->
            when {
                isRenderable(entry.videoFile) -> skipped += entry.variant.id
                render.render(entry) -> rendered += entry.variant.id
                else -> failed += entry.variant.id
            }
        }
        return ViralRenderResult(rendered = rendered, skipped = skipped, failed = failed)
    }
}
