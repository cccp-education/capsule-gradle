package capsule.viral

import java.io.File
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral render orchestration (CAP-SHORT US-1b + US-3):
 * [ViralVariantRenderer].
 *
 * The batch names one MP4 per (language × platform) variant, skips already
 * rendered ones (economy of ink), and — since every variant shares the same
 * source deck — captures it **once** and replicates the video to each variant.
 */
class ViralVariantRendererTest {

    @field:TempDir
    lateinit var dir: File

    private fun variants() = ViralBatchPlanner.plan(
        languages = listOf("fr", "en"),
        platforms = listOf(ViralPlatform.TIKTOK, ViralPlatform.YOUTUBE_SHORT),
        duration = 30,
    )

    @Test
    fun `plan names one mp4 per variant under the rendered dir`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        assertEquals(4, plan.size)
        assertEquals(
            listOf("fr-tiktok-30", "fr-youtube_short-30", "en-tiktok-30", "en-youtube_short-30"),
            plan.ids,
        )
        assertTrue(plan.entries.all { it.videoFile.parentFile == dir && it.videoFile.name.endsWith(".mp4") })
    }

    @Test
    fun `plan is empty when there is no variant`() {
        assertTrue(ViralVariantRenderer.plan(emptyList(), dir).isEmpty)
    }

    @Test
    fun `pending returns only the entries whose video is not renderable`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val done = plan.entries.first().videoFile
        val pending = ViralVariantRenderer.pending(plan) { it == done }
        assertEquals(3, pending.size)
        assertFalse(pending.any { it.videoFile == done })
    }

    @Test
    fun `render captures the source once and replicates it to every pending variant`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val existing = plan.entries.first().videoFile
        var captures = 0
        var replicates = 0
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { it == existing },
            capture = { target ->
                captures++
                target.writeText("mp4")
                true
            },
            replicate = { source, target ->
                replicates++
                target.writeText(source.readText())
                true
            },
        )
        assertEquals(listOf("fr-tiktok-30"), result.skipped)
        assertEquals(listOf("fr-youtube_short-30", "en-tiktok-30", "en-youtube_short-30"), result.rendered)
        assertEquals(1, captures, "the shared source must be captured once for the whole batch")
        assertEquals(2, replicates)
        assertTrue(result.failed.isEmpty())
    }

    @Test
    fun `render skips every variant when all are already renderable (no capture)`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        var captures = 0
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { true },
            capture = { captures++; true },
            replicate = { _, _ -> true },
        )
        assertEquals(4, result.skippedCount)
        assertEquals(0, result.renderedCount)
        assertEquals(0, captures, "a fully rendered batch must not capture at all")
    }

    @Test
    fun `render fails every pending variant when the capture fails`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { false },
            capture = { false },
            replicate = { _, _ -> true },
        )
        assertEquals(0, result.renderedCount)
        assertEquals(4, result.failedCount)
        assertEquals(0, result.skippedCount)
    }

    @Test
    fun `render records a failed replication without failing the batch`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { false },
            capture = { target -> target.writeText("mp4"); true },
            replicate = { _, target -> !target.name.startsWith("en") },
        )
        assertEquals(2, result.renderedCount)
        assertEquals(listOf("en-tiktok-30", "en-youtube_short-30"), result.failed)
    }
}
