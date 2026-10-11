package capsule.scenarios

import capsule.ai.LlmResilience
import capsule.viral.ChatModelViralHookGenerator
import capsule.viral.ViralHook
import capsule.viral.ViralHookPlan
import capsule.viral.ViralPlatform
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps for the LLM resilience guard.
 *
 * Steps are prefixed "guarded"/"viral hook generator"/"failing chat model" to
 * avoid collisions on the shared `capsule.scenarios` glue (bug S-088). Pure domain.
 */
class CapsuleLlmResilienceSteps {

    private var result: String = ""
    private var model: ChatModel? = null
    private var hook: ViralHook? = ViralHook("sentinel", "sentinel", "sentinel")

    @When("a guarded LLM call returns {string}")
    fun `a guarded LLM call returns`(value: String) {
        result = LlmResilience.guarded("op", "fallback") { value }
    }

    @When("a guarded LLM call fails with {string}")
    fun `a guarded LLM call fails with`(message: String) {
        result = LlmResilience.guarded("op", "fallback") { throw RuntimeException(message) }
    }

    @Then("the guarded result is {string}")
    fun `the guarded result is`(expected: String) {
        assertThat(result).isEqualTo(expected)
    }

    @Given("a failing chat model")
    fun `a failing chat model`() {
        model = object : ChatModel {
            override fun doChat(request: ChatRequest): ChatResponse =
                throw RuntimeException("too many concurrent requests")
        }
    }

    @When("the viral hook generator runs")
    fun `the viral hook generator runs`() {
        val plan = ViralHookPlan(
            deckName = "content-decouverte",
            segments = emptyList(),
            language = "fr",
            platform = ViralPlatform.TIKTOK,
            targetDurationSeconds = 30,
            augmentedContext = "",
            outputPath = "hook.json",
        )
        hook = ChatModelViralHookGenerator(model!!).generate(plan)
    }

    @Then("the viral hook is absent")
    fun `the viral hook is absent`() {
        assertThat(hook).isNull()
    }
}
