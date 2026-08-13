package capsule.transcript

/**
 * Domain port — enriches a transcript template article via an LLM
 * (CAP-TRANSCRIPT US-3).
 *
 * Consumed by the `generateCapsuleTranscript` task (US-4) when the
 * [TranscriptStrategy] is `LLM`. The port takes the deterministic template
 * produced by [TranscriptBuilder] and the source [TranscriptPlan], and
 * returns an enriched AsciiDoc article (pedagogical transitions, engaging
 * introductions, reformulations).
 *
 * Synchronous contract (pattern [capsule.pipeline.CapsuleLlm]) so the
 * domain stays Gradle-free, coroutine-free, and unit-testable with a
 * plain fake. The langchain4j `ChatModel` bridge lives in the adapter
 * [ChatModelTranscriptEnhancer] — the domain knows nothing about message
 * types.
 *
 * Fallback degraded (pattern [capsule.audio.AudioPostProcessor.process]):
 * implementations return the original [template] when the LLM response is
 * blank/whitespace-only, so the caller always gets a valid AsciiDoc article.
 */
interface TranscriptLlmEnhancer {

    /**
     * Enrich [template] using [plan] as the enrichment context.
     *
     * @param template the deterministic AsciiDoc article from [TranscriptBuilder].
     * @param plan the source transcript plan (deck name, segments, language).
     * @return the enriched AsciiDoc article, or the original [template] when
     *         the LLM returns a blank response (degraded mode).
     */
    fun enhance(template: String, plan: TranscriptPlan): String
}