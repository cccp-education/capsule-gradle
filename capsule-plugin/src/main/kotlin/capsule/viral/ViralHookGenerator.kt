package capsule.viral

/**
 * Domain port — generates a viral hook/message/CTA from a [ViralHookPlan]
 * via an LLM (CAP-VIRAL US-4).
 *
 * Consumed by the viral campaign task. The plan carries the N1 augmented
 * context (rendered by `capsule.context.CapsuleContextBuilder`), so the
 * generated hook is a **derivation anchored** on the corpus (RAG/Graphify/
 * Docs/EAGER) — never an ungrounded invention. The port returns a [ViralHook];
 * implementations degrade to `null` when the LLM gives nothing usable, letting
 * the caller fall back to the deck's own content.
 *
 * Synchronous contract (pattern [capsule.transcript.TranscriptLlmEnhancer]) so
 * the domain stays Gradle-free, coroutine-free, and unit-testable with a plain
 * fake. The langchain4j `ChatModel` bridge lives in the adapter
 * [ChatModelViralHookGenerator].
 */
interface ViralHookGenerator {

    /**
     * Generate the hook/message/CTA for [plan].
     *
     * @param plan the viral hook plan (deck, segments, language, platform,
     *        duration, augmented context).
     * @return the parsed [ViralHook], or `null` when the LLM response carries
     *         no usable content (degraded mode).
     */
    fun generate(plan: ViralHookPlan): ViralHook?
}
