package capsule.scenarios

import capsule.voice.FidelityScorer
import capsule.voice.TextNormalizer
import capsule.voice.VoiceScore
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_voice_quality.feature` (CAP-QC-VOICE US-1).
 *
 * Exercises [TextNormalizer] and [FidelityScorer] directly — no Gradle, no ASR.
 * Step texts are prefixed with "voice" (S-088 — the glue is classpath-wide).
 */
class CapsuleVoiceQualitySteps {

    private var narration: String = ""
    private var transcribed: String = ""
    private var normalized: List<String> = emptyList()
    private var score: VoiceScore? = null

    @Given("the voice narration {string}")
    fun theVoiceNarration(text: String) {
        narration = text
    }

    @Given("the expected voice narration {string}")
    fun theExpectedVoiceNarration(text: String) {
        narration = text
    }

    @Given("the transcribed voice narration {string}")
    fun theTranscribedVoiceNarration(text: String) {
        transcribed = text
    }

    @When("the voice narration is normalised")
    fun theVoiceNarrationIsNormalised() {
        normalized = TextNormalizer.normalize(narration)
    }

    @When("the voice fidelity is scored")
    fun theVoiceFidelityIsScored() {
        score = FidelityScorer.score(narration, transcribed)
    }

    @Then("the normalised voice words are {string}")
    fun theNormalisedVoiceWordsAre(csv: String) {
        assertEquals(csv.split(",").map { it.trim() }, normalized)
    }

    @Then("the voice word error rate is {double}")
    fun theVoiceWordErrorRateIs(value: Double) {
        assertEquals(value, score!!.wordErrorRate, 1e-9)
    }

    @Then("the voice word error rate is greater than {double}")
    fun theVoiceWordErrorRateIsGreaterThan(value: Double) {
        assertTrue(score!!.wordErrorRate > value, "expected wordErrorRate > $value, got ${score!!.wordErrorRate}")
    }

    @Then("the voice surplus word count is {int}")
    fun theVoiceSurplusWordCountIs(value: Int) {
        assertEquals(value, score!!.surplus)
    }
}
