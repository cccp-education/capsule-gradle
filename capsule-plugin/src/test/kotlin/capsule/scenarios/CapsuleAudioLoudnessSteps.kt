package capsule.scenarios

import capsule.audio.AudioPostCommand
import capsule.audio.AudioPostConfig
import capsule.audio.LoudnormAnalysisParser
import capsule.audio.LoudnormMeasurement
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for the PRT-09 two-pass loudness scenarios in
 * `capsule_audio_post.feature`.
 *
 * Exercises [AudioPostCommand] and [LoudnormAnalysisParser] directly — no
 * Gradle, no ffmpeg. Step texts are prefixed with "loudness" / "loudnorm"
 * (S-088 — the glue is classpath-wide).
 */
class CapsuleAudioLoudnessSteps {

    private var measured: LoudnormMeasurement? = null
    private var chain: String = ""
    private var output: String = ""

    @Given("a measured loudness input of {double} {double} {double} {double} {double}")
    fun aMeasuredLoudnessInput(inputI: Double, inputTp: Double, inputLra: Double, inputThresh: Double, targetOffset: Double) {
        measured = LoudnormMeasurement(inputI, inputTp, inputLra, inputThresh, targetOffset)
    }

    @When("the two-pass loudness chain is built for target {double}")
    fun buildTwoPassChain(target: Double) {
        chain = AudioPostCommand.buildTwoPassLoudnorm(AudioPostConfig(loudnessTarget = target), measured!!)
    }

    @Then("the loudness chain contains {string}")
    fun theLoudnessChainContains(fragment: String) {
        assertTrue(chain.contains(fragment), "chain must contain '$fragment': $chain")
    }

    @Given("the loudnorm ffmpeg output block")
    fun theLoudnormFfmpegOutputBlock() {
        output = """{"input_i":"-23.03","input_tp":"-2.19","input_lra":"4.30","input_thresh":"-33.31","target_offset":"0.21"}"""
    }

    @When("the loudnorm measurement is parsed")
    fun theLoudnormMeasurementIsParsed() {
        measured = LoudnormAnalysisParser.parse(output)
    }

    @Then("the parsed loudness input_i is {double}")
    fun theParsedLoudnessInputIIs(value: Double) {
        assertEquals(value, measured!!.inputI, 1e-9)
    }

    @Then("the parsed loudness target_offset is {double}")
    fun theParsedLoudnessTargetOffsetIs(value: Double) {
        assertEquals(value, measured!!.targetOffset, 1e-9)
    }
}
