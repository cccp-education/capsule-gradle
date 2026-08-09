package capsule.ai

import codebase.koog.llm.service.LlmBuildService
import capsule.ai.CapsuleLlmService.aiProvider
import capsule.ai.CapsuleLlmService.resolveModel
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task: `capsuleAiSmokeTest`
 *
 * Thin adapter that exercises the codebase LLM bridge (CAP-ARCH-1): it
 * resolves a langchain4j [dev.langchain4j.model.chat.ChatModel] via
 * [CapsuleLlmService.resolveModel] and issues a minimal smoke call, logging
 * the response. It is the wiring proof that the `capsule.ai` socle works
 * end-to-end in a real Gradle build (registration → resolution → call).
 *
 * Mock-LLM test path: `-Pollama.baseUrl=<url>` (see [CapsuleLlmService]).
 *
 * Usage:
 *   ./gradlew capsuleAiSmokeTest
 */
@DisableCachingByDefault(because = "LLM smoke test")
abstract class CapsuleAiSmokeTestTask : DefaultTask() {

    @get:ServiceReference
    abstract val llmService: Property<LlmBuildService>

    @TaskAction
    fun run() {
        val provider = project.aiProvider
        val model = project.resolveModel(provider, llmService)
        val response = model.chat("Reply with the single word: ok")
        logger.lifecycle("CAPSULE AI → smoke test response: ${response.trim()}")
        if (response.isBlank()) {
            throw IllegalStateException(
                "capsuleAiSmokeTest received a blank response from provider '$provider'"
            )
        }
    }
}
