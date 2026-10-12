package capsule.scenarios

import capsule.FishAudioKey
import capsule.FishAudioParams
import capsule.FishAudioResponse
import capsule.FishAudioTransport
import capsule.FishAudioTtsEngine
import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import java.io.File

/**
 * CAP-FISH-AUDIO US-4 — Cucumber steps for the Fish Audio engine.
 *
 * **Fish Audio is never called**: the HTTP transport is a fake that returns canned
 * bytes. Steps are prefixed "Fish Audio"/"the transport"/"the request" to avoid
 * collisions on the shared `capsule.scenarios` glue (bug S-088).
 */
class CapsuleFishAudioSteps {

    private val params = FishAudioParams(voice = "voice-123")

    private lateinit var transport: FakeTransport
    private lateinit var engine: FishAudioTtsEngine
    private lateinit var outputFile: File
    private var failure: String? = null

    /** Fake transport: records the calls, replays a scripted list of responses. */
    private class FakeTransport : FishAudioTransport {
        var responses: List<FishAudioResponse> = listOf(FishAudioResponse(200, ByteArray(64) { 7 }))
        val endpoints = mutableListOf<String>()
        val headers = mutableListOf<Map<String, String>>()
        val bodies = mutableListOf<ByteArray>()

        override fun post(endpoint: String, headers: Map<String, String>, body: ByteArray): FishAudioResponse {
            this.endpoints += endpoint
            this.headers += headers
            this.bodies += body
            return responses[minOf(this.bodies.size - 1, responses.size - 1)]
        }
    }

    private fun freshOutput(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "fish-bdd-${System.nanoTime()}")
        return File(dir, "slide-01.mp3")
    }

    @Given("a Fish Audio engine with a key")
    fun `a Fish Audio engine with a key`() {
        transport = FakeTransport()
        outputFile = freshOutput()
        failure = null
        engine = FishAudioTtsEngine(params, listOf(FishAudioKey("sk-fish-test")), transport = transport)
    }

    @Given("a Fish Audio engine without a key")
    fun `a Fish Audio engine without a key`() {
        transport = FakeTransport()
        outputFile = freshOutput()
        failure = null
        engine = FishAudioTtsEngine(params, emptyList(), transport = transport)
    }

    @Given("the transport returns {int} then a success")
    fun `the transport returns then a success`(status: Int) {
        transport.responses = listOf(FishAudioResponse(status, ByteArray(0)), FishAudioResponse(200, ByteArray(64) { 7 }))
    }

    @Given("the transport always returns {int}")
    fun `the transport always returns`(status: Int) {
        transport.responses = listOf(FishAudioResponse(status, ByteArray(0)))
    }

    @When("the engine synthesises {string}")
    fun `the engine synthesises`(text: String) {
        failure = null
        try {
            engine.synthesize(text, outputFile)
        } catch (e: Exception) {
            failure = e.message
        }
    }

    @Then("the request endpoint is {string}")
    fun `the request endpoint is`(value: String) {
        assertThat(transport.endpoints.last()).isEqualTo(value)
    }

    @Then("the request authorization header is {string}")
    fun `the request authorization header is`(value: String) {
        assertThat(transport.headers.last()["authorization"]).isEqualTo(value)
    }

    @Then("the request model header is {string}")
    fun `the request model header is`(value: String) {
        assertThat(transport.headers.last()["model"]).isEqualTo(value)
    }

    @Then("the request body text is {string}")
    fun `the request body text is`(value: String) {
        assertThat(bodyJson()["text"].asText()).isEqualTo(value)
    }

    @Then("the request body reference_id is {string}")
    fun `the request body reference_id is`(value: String) {
        assertThat(bodyJson()["reference_id"].asText()).isEqualTo(value)
    }

    @Then("the take is written")
    fun `the take is written`() {
        assertThat(outputFile).exists()
        assertThat(outputFile.length()).isGreaterThan(0)
    }

    @Then("synthesising fails with {string}")
    fun `synthesising fails with`(fragment: String) {
        assertThat(failure).isNotNull()
        assertThat(failure).contains(fragment)
    }

    @Then("the transport received {int} requests")
    fun `the transport received requests`(count: Int) {
        assertThat(transport.bodies).hasSize(count)
    }

    @Then("the Fish Audio engine is unavailable")
    fun `the Fish Audio engine is unavailable`() {
        assertThat(engine.isAvailable()).isFalse()
    }

    @Then("the engine rendering does not contain the key")
    fun `the engine rendering does not contain the key`() {
        assertThat(engine.toString()).doesNotContain("sk-fish-test")
    }

    private fun bodyJson() = ObjectMapper().readTree(transport.bodies.last())
}
