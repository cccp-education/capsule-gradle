package capsule.viral

import java.io.File
import org.junit.jupiter.api.io.TempDir
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral render orchestration (CAP-SHORT US-1b):
 * [ViralVariantRenderer].
 *
 * The campaign domain knows the (language × platform) matrix; the FFmpeg socle
 * knows how to capture a deck. This object links them: it names one MP4 per
 * variant, skips already-rendered ones (economy of ink) and drives the render
 * through the injected [ViralVariantRender].
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
    fun `render produces the pending variants and skips the renderable ones`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val existing = plan.entries.first().videoFile
        val produced = mutableListOf<String>()
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { it == existing },
            render = { entry ->
                produced += entry.variant.id
                entry.videoFile.writeText("mp4")
                true
            },
        )
        assertEquals(listOf("fr-tiktok-30"), result.skipped)
        assertEquals(listOf("fr-youtube_short-30", "en-tiktok-30", "en-youtube_short-30"), result.rendered)
        assertTrue(result.failed.isEmpty())
        assertEquals(3, result.renderedCount)
        assertEquals(1, result.skippedCount)
        assertEquals(3, produced.size)
    }

    @Test
    fun `render records a failed variant without failing the batch`() {
        val plan = ViralVariantRenderer.plan(variants(), dir)
        val result = ViralVariantRenderer.render(
            plan = plan,
            isRenderable = { false },
            render = { entry -> entry.variant.language != "en" },
        )
        assertEquals(2, result.renderedCount)
        assertEquals(0, result.skippedCount)
        assertEquals(listOf("en-tiktok-30", "en-youtube_short-30"), result.failed)
    }
}
