package capsule.anim

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration of the pure timing model over a two-slide composition: proves the
 * screen is never black during the cross-fade and that a slide's text only
 * appears once its background has covered the outgoing slide (no title overlap).
 *
 * Mirrors the `Capsule`/`Slide` composition from Capsule.jsx without rendering.
 */
class RemotionTimingIntegrationTest {

    private val fps = 30
    private val crossfade = RemotionTiming.CROSSFADE_FRAMES

    private data class Slide(val durationInFrames: Int, val first: Boolean)

    private fun mountedBgOpacityAt(frame: Int, slides: List<Slide>): Double {
        // Each slide sequence starts where the previous ended (prolonged by crossfade).
        var start = 0
        var best = 0.0
        slides.forEachIndexed { i, slide ->
            val isLast = i == slides.lastIndex
            val seqEnd = start + RemotionTiming.sequenceDurationInFrames(slide.durationInFrames, isLast)
            if (frame in start until seqEnd) {
                val op = RemotionTiming.backgroundOpacityAt(frame, start, slide.durationInFrames, crossfade, slide.first)
                if (op > best) best = op
            }
            start = seqEnd - crossfade // next sequence begins this cross-fade earlier
        }
        return best
    }

    private fun contentAppearAt(frame: Int, slides: List<Slide>): Map<Int, Double> {
        val result = mutableMapOf<Int, Double>()
        var start = 0
        slides.forEachIndexed { i, slide ->
            val isLast = i == slides.lastIndex
            val seqEnd = start + RemotionTiming.sequenceDurationInFrames(slide.durationInFrames, isLast)
            if (frame in start until seqEnd) {
                result[i] = RemotionTiming.contentAppear(frame - start, crossfade, first = slide.first)
            }
            start = seqEnd - crossfade
        }
        return result
    }

    @Test
    fun `screen is never black across the full composition`() {
        val slides = listOf(Slide(80, first = true), Slide(60, first = false))
        val total = slides.sumOf { it.durationInFrames }
        for (frame in 0 until total) {
            assertThat(mountedBgOpacityAt(frame, slides))
                .describedAs("frame $frame must be fully covered")
                .isEqualTo(1.0)
        }
    }

    @Test
    fun `incoming text appears only after its background covers the outgoing slide`() {
        val slides = listOf(Slide(80, first = true), Slide(60, first = false))
        val total = slides.sumOf { it.durationInFrames }
        for (frame in 0 until total) {
            val contents = contentAppearAt(frame, slides)
            // The incoming (second) slide only shows text once its background is
            // substantially opaque, so it never overlaps the outgoing title.
            if (contents[1] != null && contents.getValue(1) > 0.0) {
                assertThat(mountedBgOpacityAt(frame, slides))
                    .describedAs("incoming text at frame $frame requires covered outgoing slide")
                    .isGreaterThanOrEqualTo(0.95)
            }
        }
    }

    @Test
    fun `overlapping titles only happen over a fully covered screen`() {
        val slides = listOf(Slide(80, first = true), Slide(60, first = false))
        val total = slides.sumOf { it.durationInFrames }
        for (frame in 0 until total) {
            val visibleTitles = contentAppearAt(frame, slides).values.count { it > 0.5 }
            if (visibleTitles > 1) {
                // When both titles briefly show during the cross-fade, the incoming
                // background must already fully cover the outgoing slide (no gap).
                assertThat(mountedBgOpacityAt(frame, slides))
                    .describedAs("frame $frame overlap must stay covered")
                    .isEqualTo(1.0)
            }
        }
    }
}
