package capsule.scenarios

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import capsule.anim.RemotionTiming

/**
 * BDD steps for `capsule_remotion_anim.feature` (CAP-ANIM-TEST).
 *
 * Exercises the pure [RemotionTiming] model ported from Capsule.jsx — no
 * Node, no FFmpeg, no GradleRunner. Step texts are prefixed "remotion anim"
 * to avoid DuplicateStepDefinitionException with the shared glue (bug S-088).
 */
class CapsuleRemotionAnimSteps {

    private var lastBeat: Double = 0.0
    private var lastRate: Double = 0.0

    @Given("a remotion anim timing model")
    fun aRemotionAnimTimingModel() {
        // model is the RemotionTiming object; nothing to initialise
    }

    @When("the beat of block {string} is requested")
    fun theBeatOfBlockIsRequested(block: String) {
        lastBeat = RemotionTiming.beatSeconds(block)
    }

    @When("applying cue data-at {string} on {string} yields style {string}")
    fun applyingCueYieldsStyle(rawAt: String, html: String, expected: String) {
        val out = RemotionTiming.applyCues(html)
        assertThat(out).contains(expected)
        assertThat(out).doesNotContain("data-at")
    }

    @When("a {int}-frame clip is fitted to a {int}-frame slide")
    fun aClipIsFittedToASlide(clip: Int, slide: Int) {
        lastRate = RemotionTiming.clipPlaybackRate(clip, slide)
    }

    @When("a missing clip fitted to a {int}-frame slide plays at {double}")
    fun aMissingClipFitted(clip: Int, expected: Double) {
        lastRate = RemotionTiming.clipPlaybackRate(clip, 40)
        assertThat(lastRate).isEqualTo(expected)
    }

    @When("a two-slide composition of {int} and {int} frames is swept")
    fun aTwoSlideCompositionIsSwept(d0: Int, d1: Int) {
        val slides = listOf(
            RemotionTimingTestSlide(d0, first = true),
            RemotionTimingTestSlide(d1, first = false),
        )
        val crossfade = RemotionTiming.CROSSFADE_FRAMES
        val total = d0 + d1
        for (frame in 0 until total) {
            var start = 0
            var best = 0.0
            var incomingTextAfterCovered = true
            slides.forEachIndexed { i, slide ->
                val isLast = i == slides.lastIndex
                val seqEnd = start + RemotionTiming.sequenceDurationInFrames(slide.durationInFrames, isLast)
                if (frame in start until seqEnd) {
                    val local = frame - start
                    val bg = RemotionTiming.backgroundOpacityAt(frame, start, slide.durationInFrames, crossfade, slide.first)
                    if (bg > best) best = bg
                    val content = RemotionTiming.contentAppear(local, crossfade, first = slide.first)
                    if (i == 1 && content > 0.0 && bg < 0.95) incomingTextAfterCovered = false
                }
                start = seqEnd - crossfade
            }
            assertThat(best).describedAs("frame $frame must be fully covered").isEqualTo(1.0)
            assertThat(incomingTextAfterCovered).describedAs("frame $frame incoming title must follow coverage").isTrue
        }
    }

    @Then("the beat seconds is {double}")
    fun theBeatSecondsIs(expected: Double) {
        assertThat(lastBeat).isEqualTo(expected)
    }

    @Then("the beat of block {string} is {double}")
    fun theBeatOfBlockIs(block: String, expected: Double) {
        assertThat(RemotionTiming.beatSeconds(block)).isEqualTo(expected)
    }

    @Then("the beat of carried block {string} is {double}")
    fun theBeatOfCarriedBlockIs(block: String, expected: Double) {
        assertThat(RemotionTiming.beatSeconds(block)).isEqualTo(expected)
    }

    @Then("the beat of unknown block {string} is {double}")
    fun theBeatOfUnknownBlockIs(block: String, expected: Double) {
        assertThat(RemotionTiming.beatSeconds(block)).isEqualTo(expected)
    }

    @Then("the playback rate is {double}")
    fun thePlaybackRateIs(expected: Double) {
        assertThat(lastRate).isEqualTo(expected)
    }

    @Then("a {int}-frame clip fitted to a {int}-frame slide is clamped to {double}")
    fun aClipFittedIsClampedTo(clip: Int, slide: Int, expected: Double) {
        assertThat(RemotionTiming.clipPlaybackRate(clip, slide)).isEqualTo(expected)
    }

    @Then("every frame is fully covered")
    fun everyFrameIsFullyCovered() {
        // assertion performed inside the sweep step
    }

    @Then("the incoming title only appears once its background covers the outgoing slide")
    fun theIncomingTitleOnlyAppearsAfterCoverage() {
        // assertion performed inside the sweep step
    }

    private data class RemotionTimingTestSlide(val durationInFrames: Int, val first: Boolean)
}
