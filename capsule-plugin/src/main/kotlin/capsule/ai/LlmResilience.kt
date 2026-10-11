package capsule.ai

import org.gradle.api.logging.Logging

/**
 * Shared resilience guard for the capsule LLM adapters (EPIC LLM-RESILIENCE).
 *
 * The capsule adapters ([capsule.viral.ChatModelViralHookGenerator],
 * [capsule.transcript.ChatModelTranscriptEnhancer]) already degrade on a **blank**
 * LLM answer (economy of ink: a blank answer is a no-op, not a failure). A metered
 * cloud LLM also fails **transiently** — HTTP 429/503, timeouts — and today that
 * exception propagates and fails the whole build (observed during the CAP-SHORT
 * US-4 dogfooding: `too many concurrent requests`).
 *
 * [guarded] wraps a model call: on any exception it logs a warning and returns the
 * caller's fallback — the same degradation the adapters apply to a blank answer.
 * The failure is **logged, never silent**, so a permanent misconfiguration stays
 * visible.
 *
 * Scope: this guard applies only where the caller has a **safe fallback** (the viral
 * hook falls back to the storyboard message; the transcript falls back to its
 * deterministic template). The content pipeline ([capsule.pipeline.ChatModelCapsuleLlm])
 * intentionally stays fail-fast: a generated content plan has no safe fallback.
 */
object LlmResilience {

    private val logger = Logging.getLogger(LlmResilience::class.java)

    /**
     * Runs [block], returning its result — or [fallback] when it throws.
     *
     * @param operation human-readable label for the warning (e.g. "viral hook").
     * @param fallback  value returned when the block fails.
     * @param block     the model call (and its parsing) to guard.
     */
    fun <T> guarded(operation: String, fallback: T, block: () -> T): T =
        try {
            block()
        } catch (e: Exception) {
            logger.warn(
                "CAPSULE LLM → {} degraded on LLM error ({}): falling back (economy of ink).",
                operation,
                e.message,
            )
            fallback
        }
}
