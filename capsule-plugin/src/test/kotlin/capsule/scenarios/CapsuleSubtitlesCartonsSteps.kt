package capsule.scenarios

import capsule.SubtitleCartoner
import capsule.SubtitleEntry
import capsule.SubtitleTextNormalizer
import capsule.SubtitleTimingCalculator
import capsule.SubtitleTimingDistributor
import capsule.feed.SlideSegment
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_subtitles_cartons.feature` (PRT-03).
 *
 * Exercises the subtitle helpers directly — [SubtitleTextNormalizer],
 * [SubtitleCartoner], [SubtitleTimingDistributor] and
 * [SubtitleTimingCalculator.calculateFromCartons] — with no Gradle, no
 * Playwright and no ffmpeg.
 *
 * Step texts are prefixed with "subtitle" (S-088 — the glue is
 * classpath-wide, so step texts must be unique).
 */
class CapsuleSubtitlesCartonsSteps {

    private var note: String = ""
    private var display: String = ""
    private var cartons: List<String> = emptyList()
    private var ranges: List<ClosedRange<Double>> = emptyList()
    private val slides = mutableListOf<SlideSegment>()
    private val durations = mutableListOf<Double>()
    private var entries: List<SubtitleEntry> = emptyList()

    @Given("the subtitle speaker note {string}")
    fun theSubtitleSpeakerNote(note: String) {
        this.note = note
    }

    @When("the subtitle speaker note is normalised for display")
    fun normaliseForDisplay() {
        display = SubtitleTextNormalizer.spokenToDisplay(note)
    }

    @Then("the subtitle display text equals {string}")
    fun displayTextEquals(expected: String) {
        assertEquals(expected, display)
    }

    @When("the subtitle speaker note is split into cartons of at most {int} characters")
    fun splitIntoCartons(maxChars: Int) {
        display = SubtitleTextNormalizer.spokenToDisplay(note)
        cartons = SubtitleCartoner.split(display, maxChars)
    }

    @Then("every subtitle carton is at most {int} characters")
    fun everyCartonAtMost(maxChars: Int) {
        assertTrue(cartons.isNotEmpty(), "expected at least one carton")
        assertTrue(cartons.all { it.length <= maxChars }, "every carton must be <= $maxChars chars: $cartons")
    }

    @And("the subtitle cartons rejoin the normalised note")
    fun cartonsRejoinTheNote() {
        assertEquals(display, cartons.joinToString(" "))
    }

    @Given("subtitle cartons {string} and {string}")
    fun subtitleCartons(first: String, second: String) {
        cartons = listOf(first, second)
    }

    @When("a {int} second take starting at {int} is distributed across the subtitle cartons")
    fun distributeTake(seconds: Int, start: Int) {
        ranges = SubtitleTimingDistributor.distribute(cartons, start.toDouble(), seconds.toDouble())
    }

    @Then("the subtitle timing ranges cover {double} to {double} contiguously")
    fun rangesCoverContiguously(from: Double, to: Double) {
        assertEquals(from, ranges.first().start, 1e-9)
        assertEquals(to, ranges.last().endInclusive, 1e-9)
        for (i in 0 until ranges.size - 1) {
            assertEquals(ranges[i].endInclusive, ranges[i + 1].start, 1e-9)
        }
    }

    @And("the longer subtitle carton lasts longer")
    fun theLongerCartonLastsLonger() {
        val shortestIndex = cartons.indices.minByOrNull { cartons[it].length }!!
        val longestIndex = cartons.indices.maxByOrNull { cartons[it].length }!!
        val shortest = ranges[shortestIndex].let { it.endInclusive - it.start }
        val longest = ranges[longestIndex].let { it.endInclusive - it.start }
        assertTrue(longest > shortest, "the longest carton must last longer: $ranges")
    }

    @Given("two subtitle slides with notes {string} and {string}")
    fun twoSubtitleSlides(first: String, second: String) {
        slides.clear()
        durations.clear()
        slides.add(SlideSegment(index = 0, title = "s0", narration = first))
        slides.add(SlideSegment(index = 1, title = "s1", narration = second))
        durations.add(0.5)
        durations.add(0.5)
    }

    @When("subtitle cues are calculated from cartons of at most {int} characters")
    fun calculateCues(maxChars: Int) {
        entries = SubtitleTimingCalculator.calculateFromCartons(slides.toList(), durations.toList(), maxChars)
    }

    @Then("the subtitle cue indices are sequential from 1")
    fun cueIndicesAreSequential() {
        assertEquals((1..entries.size).toList(), entries.map { it.index })
    }

    @And("the last subtitle cue ends at {double} second")
    fun lastCueEndsAt(seconds: Double) {
        assertEquals(seconds, entries.last().endTime, 1e-9)
    }
}
