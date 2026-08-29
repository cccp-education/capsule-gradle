package capsule.anim

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RemotionTimingTest {

    @Test
    fun `beatSeconds resolves known selectors`() {
        assertThat(RemotionTiming.beatSeconds("rule")).isEqualTo(0.5)
        assertThat(RemotionTiming.beatSeconds("lede")).isEqualTo(1.0)
        assertThat(RemotionTiming.beatSeconds("stat")).isEqualTo(4.5)
        assertThat(RemotionTiming.beatSeconds("statlabel")).isEqualTo(5.0)
    }

    @Test
    fun `beatSeconds carried blocks appear immediately`() {
        RemotionTiming.CARRIED_BY_TRANSITION.forEach { block ->
            assertThat(RemotionTiming.beatSeconds(block))
                .describedAs("carried block $block must beat at 0")
                .isEqualTo(0.0)
        }
    }

    @Test
    fun `beatSeconds falls back to default for unknown block`() {
        assertThat(RemotionTiming.beatSeconds("unknown")).isEqualTo(RemotionTiming.DEFAULT_BEAT_SECS)
        assertThat(RemotionTiming.beatSeconds("h3")).isEqualTo(RemotionTiming.DEFAULT_BEAT_SECS)
    }

    @Test
    fun `applyCues converts data-at into style declaration`() {
        val html = """<section><p class="lede" data-at="7.5">Hello</p></section>"""
        val out = RemotionTiming.applyCues(html)
        assertThat(out).contains("""style="--d: 7.5;""")
        assertThat(out).doesNotContain("data-at")
    }

    @Test
    fun `applyCues merges into existing style attribute`() {
        val html = """<div class="stat" style="color: red;" data-at="3.25">42</div>"""
        val out = RemotionTiming.applyCues(html)
        assertThat(out).contains("""style="--d: 3.25;color: red;""")
        assertThat(out).doesNotContain("data-at")
    }

    @Test
    fun `applyCues preserves markup without data-at`() {
        val html = """<section><h1>Title</h1><p class="rule">x</p></section>"""
        assertThat(RemotionTiming.applyCues(html)).isEqualTo(html)
    }

    @Test
    fun `applyCues rounds decimal beats`() {
        val html = """<p data-at="4.0">x</p>"""
        assertThat(RemotionTiming.applyCues(html)).contains("--d: 4.0;")
    }

    @Test
    fun `clipPlaybackRate clamps speed-up`() {
        // clip 50 frames for a 40-frame slide → 1.25 raw, clamped to 1.25
        assertThat(RemotionTiming.clipPlaybackRate(50, 40)).isEqualTo(1.25)
        // clip 100 frames → 2.5 raw, clamped to 1.25
        assertThat(RemotionTiming.clipPlaybackRate(100, 40)).isEqualTo(1.25)
    }

    @Test
    fun `clipPlaybackRate clamps slow-down`() {
        // clip 20 frames for 40-frame slide → 0.5 raw, clamped to 0.8
        assertThat(RemotionTiming.clipPlaybackRate(20, 40)).isEqualTo(0.8)
        // clip 30 frames → 0.75 raw, clamped to 0.8
        assertThat(RemotionTiming.clipPlaybackRate(30, 40)).isEqualTo(0.8)
    }

    @Test
    fun `clipPlaybackRate natural within bounds`() {
        assertThat(RemotionTiming.clipPlaybackRate(40, 40)).isEqualTo(1.0)
        assertThat(RemotionTiming.clipPlaybackRate(44, 40)).isEqualTo(1.1)
    }

    @Test
    fun `clipPlaybackRate missing clip plays at natural speed`() {
        assertThat(RemotionTiming.clipPlaybackRate(0, 40)).isEqualTo(1.0)
        assertThat(RemotionTiming.clipPlaybackRate(-1, 40)).isEqualTo(1.0)
    }

    @Test
    fun `compress pulls in short slides`() {
        // LAST_BEAT_SECS = 5.55; a 2.775s slide compresses to exactly 0.5
        assertThat(RemotionTiming.compress(2.775)).isEqualTo(0.5)
        assertThat(RemotionTiming.compress(RemotionTiming.LAST_BEAT_SECS)).isEqualTo(1.0)
        assertThat(RemotionTiming.compress(10.0)).isEqualTo(1.0)
    }

    @Test
    fun `backgroundAppear first slide fully shown`() {
        assertThat(RemotionTiming.backgroundAppear(0, first = true)).isEqualTo(1.0)
        assertThat(RemotionTiming.backgroundAppear(10, first = true)).isEqualTo(1.0)
    }

    @Test
    fun `backgroundAppear fades in over crossfade`() {
        assertThat(RemotionTiming.backgroundAppear(0, first = false)).isEqualTo(0.0)
        assertThat(RemotionTiming.backgroundAppear(10, first = false))
            .isGreaterThan(0.0).isLessThan(1.0)
        assertThat(RemotionTiming.backgroundAppear(20, first = false)).isEqualTo(1.0)
    }

    @Test
    fun `contentAppear waits for background to cover outgoing slide`() {
        // first slide: text always shown
        assertThat(RemotionTiming.contentAppear(0, first = true)).isEqualTo(1.0)
        // incoming: text hidden until past CONTENT_GATE
        assertThat(RemotionTiming.contentAppear(0, first = false)).isEqualTo(0.0)
        assertThat(RemotionTiming.contentAppear(14, first = false)).isEqualTo(0.0)
        assertThat(RemotionTiming.contentAppear(20, first = false)).isEqualTo(1.0)
    }

    @Test
    fun `sequenceDurationInFrames overlaps all but last`() {
        assertThat(RemotionTiming.sequenceDurationInFrames(100, isLast = false)).isEqualTo(120)
        assertThat(RemotionTiming.sequenceDurationInFrames(100, isLast = true)).isEqualTo(100)
    }
}
