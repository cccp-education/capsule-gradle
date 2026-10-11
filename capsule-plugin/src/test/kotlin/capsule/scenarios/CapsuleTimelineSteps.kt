package capsule.scenarios

import capsule.timeline.Timeline
import capsule.timeline.TimelineBuilder
import capsule.timeline.TimelineConfig
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Pure-domain BDD steps for `capsule_timeline.feature` (PRT-07).
 *
 * Exercises [TimelineBuilder] directly — no Gradle, no capture. Step texts are
 * prefixed with "timeline" (S-088 — the glue is classpath-wide).
 */
class CapsuleTimelineSteps {

    private var durations: List<Double> = emptyList()
    private var config: TimelineConfig = TimelineConfig()
    private var timeline: Timeline? = null

    @Given("a timeline with durations {string} at {int} fps")
    fun aTimelineWithDurations(durationsCsv: String, fps: Int) {
        durations = durationsCsv.split(",").map { it.trim().toDouble() }
        config = TimelineConfig(fps = fps)
    }

    @Given("the timeline lead is {double} seconds and tail is {double} seconds")
    fun theTimelineLeadAndTail(lead: Double, tail: Double) {
        config = config.copy(leadSeconds = lead, tailSeconds = tail)
    }

    @Given("the timeline avance is {double} seconds")
    fun theTimelineAvance(avance: Double) {
        config = config.copy(avanceSeconds = avance)
    }

    @When("the timeline is built")
    fun theTimelineIsBuilt() {
        timeline = TimelineBuilder.build(durations, config)
    }

    @Then("the timeline totals {long} frames")
    fun theTimelineTotalsFrames(frames: Long) {
        assertEquals(frames, timeline!!.totalFrames)
    }

    @Then("the timeline totals {double} seconds")
    fun theTimelineTotalsSeconds(seconds: Double) {
        assertEquals(seconds, timeline!!.totalSeconds, 1e-9)
    }

    @Then("timeline slide {int} starts at frame {long} with {long} frames")
    fun timelineSlideStarts(slideIndex: Int, startFrame: Long, frameCount: Long) {
        val entry = timeline!!.entries[slideIndex]
        assertEquals(startFrame, entry.startFrame)
        assertEquals(frameCount, entry.frameCount)
    }

    @Then("timeline slide {int} voice starts at frame {long}")
    fun timelineSlideVoiceStarts(slideIndex: Int, voiceStartFrame: Long) {
        assertEquals(voiceStartFrame, timeline!!.entries[slideIndex].voiceStartFrame)
    }
}
