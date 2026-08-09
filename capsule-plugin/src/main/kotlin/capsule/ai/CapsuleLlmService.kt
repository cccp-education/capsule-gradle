@file:Suppress("MemberVisibilityCanBePrivate")

package capsule.ai

import codebase.koog.llm.service.LlmBuildService
import dev.langchain4j.model.chat.ChatModel
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceSpec
import java.time.Duration

/**
 * Central LLM socle for the Capsule Gradle plugin.
 *
 * EPIC CAP-ARCH (decision S-083): capsule consumes codebase (N1) as the
 * unified LLM socle, mirroring the slider SLD-8 pattern. Provider resolution
 * delegates to [LlmBuildService] (Gradle-managed DI) via
 * [LlmProviderChatModelAdapter].
 *
 * Responsibilities:
 * - [registerLlmBuildService] — [LlmBuildService] registration (Gradle DI)
 * - [resolveModel] — bridges codebase's [codebase.koog.llm.LlmProvider] to
 *   langchain4j [ChatModel]
 * - [aiProvider] — reads `-Pai.provider`, defaulting to `"ollama"`
 *
 * ## Mock-LLM fallback (test compat)
 *
 * When `-Pollama.baseUrl` is set (typically to a test mock HTTP server),
 * resolution returns a langchain4j `OllamaChatModel` pointed at that URL —
 * the GradleTestKit Cucumber scenarios (`capsule_ai.feature`) inject a mock
 * via `-Pollama.baseUrl`, no pool required.
 *
 * In production (no `-Pollama.baseUrl`), the codebase [LlmBuildService] is
 * used: [LlmServiceResolver] resolves the provider from the pool, wrapped in
 * a [LlmProviderChatModelAdapter].
 */
object CapsuleLlmService {

    // =========================================================================
    // Provider selection
    // =========================================================================

    const val PROP_AI_PROVIDER = "ai.provider"
    const val PROVIDER_OLLAMA = "ollama"

    /** Reads `-Pai.provider`, defaulting to `"ollama"` when absent or blank. */
    val Project.aiProvider: String
        get() = (findProperty(PROP_AI_PROVIDER) as? String
            ?: PROVIDER_OLLAMA).lowercase().trim()

    // =========================================================================
    // LlmBuildService registration + model resolution
    // =========================================================================

    /**
     * Registers the [LlmBuildService] (Gradle-managed DI) and returns its
     * [Provider]. The service exposes a codebase [codebase.koog.llm.LlmProvider]
     * resolved by [codebase.koog.llm.service.LlmServiceResolver].
     *
     * Call once per build; inject the returned [Provider] into tasks via
     * `@ServiceReference`.
     */
    fun Project.registerLlmBuildService(): Provider<LlmBuildService> =
        gradle.sharedServices.registerIfAbsent(
            "capsuleLlmService", LlmBuildService::class.java
        ) { spec: BuildServiceSpec<LlmBuildService.Params> ->
            spec.parameters.model.convention(project.aiProvider)
            spec.maxParallelUsages.set(1)
        }

    /**
     * Resolves the langchain4j [ChatModel] for the given [provider] by
     * delegating to the codebase [LlmBuildService] and wrapping the returned
     * [codebase.koog.llm.LlmProvider] in a [LlmProviderChatModelAdapter].
     *
     * ## Mock-LLM fallback (test compat)
     *
     * When `-Pollama.baseUrl` is set (typically to a test mock HTTP server),
     * resolution returns an `OllamaChatModel` honoring that property. This
     * keeps the GradleTestKit Cucumber scenarios (`capsule_ai.feature`)
     * working without an LLM pool.
     *
     * In production (no `-Pollama.baseUrl`), the codebase [LlmBuildService]
     * is used: the provider is resolved from the pool and wrapped in a
     * [LlmProviderChatModelAdapter].
     */
    fun Project.resolveModel(
        provider: String,
        serviceProvider: Provider<LlmBuildService>,
    ): ChatModel {
        val mockOllamaUrl = findProperty("ollama.baseUrl") as? String
        if (provider == PROVIDER_OLLAMA && mockOllamaUrl != null) {
            return createOllamaChatModel(mockOllamaUrl)
        }
        val llmProvider = serviceProvider.get().provider()
        return LlmProviderChatModelAdapter(llmProvider)
    }

    /**
     * Builds a langchain4j `OllamaChatModel` honoring the `ollama.*` Gradle
     * properties (`baseUrl`, `modelName`, `temperature`, `timeout`). Used by
     * the mock-LLM fallback in [resolveModel].
     */
    fun Project.createOllamaChatModel(baseUrl: String): ChatModel =
        dev.langchain4j.model.ollama.OllamaChatModel.builder().apply {
            this.baseUrl(baseUrl)
            modelName(findProperty("ollama.modelName")?.toString() ?: "smollm:135m")
            temperature(findProperty("ollama.temperature")?.toString()?.toDoubleOrNull() ?: 0.8)
            timeout(Duration.ofSeconds(findProperty("ollama.timeout")?.toString()?.toLongOrNull() ?: 6L))
            logRequests(true)
            logResponses(true)
        }.build()
}
