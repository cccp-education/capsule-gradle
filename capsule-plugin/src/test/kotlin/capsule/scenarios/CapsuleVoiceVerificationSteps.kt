package capsule.scenarios

import capsule.voice.FidelityScorer
import capsule.voice.SpeechRecognizer
import capsule.voice.SpeechRecognizerException
import capsule.voice.TakeSelector
import capsule.voice.TakeVerification
import capsule.voice.VoiceQualityGate
import capsule.voice.VoiceReport
import capsule.voice.VoiceSelection
import capsule.voice.VoiceTake
import capsule.voice.VoiceThresholds
import capsule.voice.VoiceVerdict
import capsule.voice.VoiceVerifier
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_voice_verification.feature` (CAP-QC-VOICE US-2).
 *
 * Exercises [VoiceVerifier] with a fake [SpeechRecognizer] — no Gradle, no ASR
 * binary. Step texts are prefixed with "voice verification" (S-088 — the glue
 * is classpath-wide).
 */
class CapsuleVoiceVerificationSteps {

    private val takes = mutableListOf<VoiceTake>()
    private var transcript: String = ""
    private var recognitionFails: Boolean = false
    private var asrUnavailable: Boolean = false
    private var envelope: DoubleArray? = null
    private var report: VoiceReport? = null

    private val candidates = mutableListOf<TakeVerification>()
    private var selections: List<VoiceSelection> = emptyList()

    @Given("a voice verification take {int} expecting {string}")
    fun aVoiceVerificationTake(index: Int, expected: String) {
        takes.add(
            VoiceTake(
                deckName = "demo",
                slideIndex = index,
                expectedText = expected,
                audioFile = File("slide-%02d.mp3".format(index)),
            )
        )
    }

    @Given("the voice recognition returns {string}")
    fun theVoiceRecognitionReturns(text: String) {
        transcript = text
    }

    @Given("the voice recognition fails")
    fun theVoiceRecognitionFails() {
        recognitionFails = true
    }

    @Given("the voice recognition is unavailable")
    fun theVoiceRecognitionIsUnavailable() {
        asrUnavailable = true
    }

    @Given("the voice verification audio envelope is silent")
    fun theVoiceVerificationAudioEnvelopeIsSilent() {
        envelope = DoubleArray(20) { 0.0 }
    }

    @Given("the voice verification audio envelope is audible")
    fun theVoiceVerificationAudioEnvelopeIsAudible() {
        envelope = DoubleArray(20) { 0.1 }
    }

    @When("the voice verification runs")
    fun theVoiceVerificationRuns() {
        val thresholds = VoiceThresholds()
        val envelopeProvider: (File) -> DoubleArray? = { envelope }
        report = if (asrUnavailable) {
            VoiceVerifier.degraded(takes.toList(), thresholds, envelopeProvider = envelopeProvider)
        } else {
            val fake = SpeechRecognizer { _, _ ->
                if (recognitionFails) throw SpeechRecognizerException("fake ASR failure") else transcript
            }
            VoiceVerifier.verify(takes.toList(), fake, thresholds, "fr", envelopeProvider)
        }
    }

    @Then("the voice verification has no failure")
    fun theVoiceVerificationHasNoFailure() {
        assertTrue(report!!.failures.isEmpty(), "expected no failure, got ${report!!.failures}")
    }

    @Then("the voice verification has {int} failure")
    fun theVoiceVerificationHasFailure(count: Int) {
        assertEquals(count, report!!.failures.size, "unexpected failures: ${report!!.failures}")
    }

    @Then("the voice verification is degraded")
    fun theVoiceVerificationIsDegraded() {
        assertTrue(report!!.degraded, "expected the report to be degraded")
    }

    @Then("the voice verification take {int} is {word}")
    fun theVoiceVerificationTakeIs(index: Int, verdictName: String) {
        val take = report!!.takes.first { it.slideIndex == index }
        val matches = when (verdictName.uppercase()) {
            "PASS" -> take.verdict == VoiceVerdict.Pass
            "FAIL" -> take.verdict is VoiceVerdict.Fail
            "DEGRADED" -> take.verdict is VoiceVerdict.Degraded
            else -> false
        }
        assertTrue(matches, "expected take $index to be $verdictName, got ${take.verdict}")
    }

    @Then("the voice verification report lists reason {string}")
    fun theVoiceVerificationReportListsReason(reason: String) {
        val reasons = report!!.takes.flatMap { take ->
            when (val verdict = take.verdict) {
                is VoiceVerdict.Fail -> verdict.reasons
                is VoiceVerdict.Degraded -> listOf(verdict.reason)
                is VoiceVerdict.Pass -> emptyList()
            }
        }
        assertTrue(reason in reasons, "expected reason '$reason' in $reasons")
    }

    @Given("a voice verification candidate {int} named {string} expecting {string} transcribed {string}")
    fun aVoiceVerificationCandidate(index: Int, name: String, expected: String, spoken: String) {
        candidates.add(candidateVerification(index, name, expected, spoken))
    }

    @Given("a degraded voice verification candidate {int} named {string}")
    fun aDegradedVoiceVerificationCandidate(index: Int, name: String) {
        candidates.add(
            TakeVerification(
                deckName = "demo",
                slideIndex = index,
                audioFile = name,
                expectedText = "bonjour",
                transcript = "",
                wordErrorRate = null,
                surplus = null,
                snrDb = null,
                verdict = VoiceVerdict.Degraded(VoiceVerifier.REASON_ASR_ERROR),
            )
        )
    }

    @When("the best voice take is selected")
    fun theBestVoiceTakeIsSelected() {
        selections = TakeSelector.selectBySlide(candidates.toList())
    }

    @Then("the chosen voice take for slide {int} is {string}")
    fun theChosenVoiceTakeForSlideIs(index: Int, name: String) {
        assertEquals(name, selections.first { it.slideIndex == index }.chosenAudioFile)
    }

    @Then("the chosen voice take count for slide {int} is {int}")
    fun theChosenVoiceTakeCountForSlideIs(index: Int, count: Int) {
        assertEquals(count, selections.first { it.slideIndex == index }.candidateCount)
    }

    private fun candidateVerification(index: Int, name: String, expected: String, spoken: String): TakeVerification {
        val thresholds = VoiceThresholds()
        val score = FidelityScorer.score(expected, spoken)
        return TakeVerification(
            deckName = "demo",
            slideIndex = index,
            audioFile = name,
            expectedText = expected,
            transcript = spoken,
            wordErrorRate = score.wordErrorRate,
            surplus = score.surplus,
            snrDb = null,
            verdict = VoiceQualityGate.verdict(score, null, thresholds),
        )
    }
}
