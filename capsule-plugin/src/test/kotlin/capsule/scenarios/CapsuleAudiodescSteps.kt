package capsule.scenarios

import capsule.a11y.A11yAcceptance
import capsule.a11y.A11yInputs
import capsule.a11y.A11yVerdict
import capsule.a11y.AudioDescriptionCue
import capsule.a11y.AudioDescriptionInput
import capsule.a11y.AudioDescriptionMixCommand
import capsule.a11y.AudioDescriptionMixSettings
import capsule.a11y.AudioDescriptionPlanner
import capsule.a11y.AudioDescriptionScript
import capsule.a11y.NarrationWindow
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File

/**
 * CAP-AUDIODESC US-4 — acceptance of the accessibility audio description.
 *
 * Pure: no TTS, no FFmpeg, no Gradle task. Steps are prefixed (`audio
 * description …`) to avoid glue collisions (S-088).
 */
class CapsuleAudiodescSteps {
    private var cues: List<AudioDescriptionCue> = emptyList()
    private var windows: List<NarrationWindow> = emptyList()
    private var plan: AudioDescriptionScript? = null
    private var graph: String = ""
    private var plannedStart: Double = 0.0
    private var verdict: A11yVerdict? = null

    @Given("a narration with two windows of {double} and {double} seconds separated by {double} seconds")
    fun narrationWithTwoWindows(
        first: Double,
        second: Double,
        gap: Double,
    ) {
        windows =
            listOf(
                NarrationWindow(0.0, first),
                NarrationWindow(first + gap, first + gap + second),
            )
    }

    @Given("a description cue {string} lasting {double} seconds")
    fun descriptionCue(
        text: String,
        duration: Double,
    ) {
        cues = listOf(AudioDescriptionCue(text, duration))
    }

    @When("I plan the audio description")
    fun planAudioDescription() {
        plan = AudioDescriptionPlanner.plan(cues, windows, totalDurationSeconds = windows.last().endSeconds)
    }

    @Then("the plan places {int} cue at {double} seconds")
    fun planPlaces(
        count: Int,
        start: Double,
    ) {
        val actual = plan ?: error("no plan computed")
        check(actual.segments.size == count) { "expected $count placed cue(s), got ${actual.segments.size}" }
        check(actual.segments[0].startSeconds == start) {
            "expected the cue to start at $start, got ${actual.segments[0].startSeconds}"
        }
    }

    @Then("the plan declares {int} unplaced cue")
    fun planDeclaresUnplaced(count: Int) {
        val actual = plan ?: error("no plan computed")
        check(actual.unplaced.size == count) { "expected $count unplaced cue(s), got ${actual.unplaced.size}" }
    }

    @Given("a planned cue at {double} seconds")
    fun plannedCueAt(seconds: Double) {
        plannedStart = seconds
    }

    @When("I build the audio description mix")
    fun buildMix() {
        graph =
            AudioDescriptionMixCommand.buildFilterGraph(
                inputs = listOf(AudioDescriptionInput(File("description-01.mp3"), plannedStart)),
                settings = AudioDescriptionMixSettings(),
            )
    }

    @Then("the mix delays the cue to {int} milliseconds")
    fun mixDelays(millis: Int) {
        check(graph.contains("adelay=$millis|$millis")) { "expected adelay=$millis in the graph: $graph" }
    }

    @Then("the mix ducks the description with a sidechain")
    fun mixDucks() {
        check(graph.contains("sidechaincompress")) { "expected sidechaincompress in the graph: $graph" }
    }

    @Given("subtitles, a transcript and an audio description")
    fun completeChannels() {
        verdict =
            A11yAcceptance.evaluate(
                A11yInputs(
                    subtitlesPresent = true,
                    transcriptPresent = true,
                    audioDescriptionRequested = true,
                    audioDescriptionPresent = true,
                ),
            )
    }

    @Given("subtitles and an audio description but no transcript")
    fun missingTranscript() {
        verdict =
            A11yAcceptance.evaluate(
                A11yInputs(
                    subtitlesPresent = true,
                    transcriptPresent = false,
                    audioDescriptionRequested = true,
                    audioDescriptionPresent = true,
                ),
            )
    }

    @Given("a requested audio description that is absent")
    fun missingAudioDescription() {
        verdict =
            A11yAcceptance.evaluate(
                A11yInputs(
                    subtitlesPresent = true,
                    transcriptPresent = true,
                    audioDescriptionRequested = true,
                    audioDescriptionPresent = false,
                ),
            )
    }

    @Then("the verdict is accepted")
    fun verdictAccepted() {
        check(verdict is A11yVerdict.Accepted) { "expected an accepted verdict, got $verdict" }
    }

    @Then("the verdict is degraded")
    fun verdictDegraded() {
        check(verdict is A11yVerdict.Degraded) { "expected a degraded verdict, got $verdict" }
    }

    @Then("the limits are declared")
    fun limitsDeclared() {
        val limitations = verdict?.limitations ?: emptyList()
        check(limitations.contains(A11yAcceptance.EXTENDED_AUDIO_DESCRIPTION_LIMIT)) {
            "expected the v1 limitation to be declared, got $limitations"
        }
    }

    @Then("the degradation names the transcription")
    fun degradationNamesTranscription() {
        val reasons = (verdict as? A11yVerdict.Degraded)?.reasons ?: emptyList()
        check(reasons.any { it.contains("transcription") }) { "expected the transcription reason, got $reasons" }
    }

    @Then("the degradation names the audio description")
    fun degradationNamesAudioDescription() {
        val reasons = (verdict as? A11yVerdict.Degraded)?.reasons ?: emptyList()
        check(reasons.any { it.contains("audio description") }) { "expected the audio description reason, got $reasons" }
    }
}
