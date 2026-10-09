package capsule.viral

/**
 * Pure builder of the viral hook/message/CTA prompt (CAP-VIRAL US-4).
 *
 * Consumed by [ViralHookGenerator] (LLM). The prompt embeds the deck's slide
 * segments **and** the N1 augmented context block produced by codebase
 * (RAG/Graphify/Docs/EAGER) so the hook is a **derivation anchored** on the
 * corpus, not an ungrounded invention. When [ViralHookPlan.augmentedContext] is
 * blank the prompt still works, but explicitly tells the LLM it is ungrounded.
 *
 * Pattern: [capsule.transcript.TranscriptPromptBuilder] (pure prompt builder,
 * no I/O, no LLM call).
 */
object ViralHookPromptBuilder {

    /**
     * Build the hook generation prompt for [plan].
     *
     * @param plan the viral hook plan (deck, segments, language, platform,
     *        duration, augmented context).
     * @return the prompt string sent to the LLM.
     */
    fun buildHookPrompt(plan: ViralHookPlan): String = buildString {
        appendLine("You write short-form viral hooks for a training product.")
        appendLine()
        appendLine(
            "Deck '${plan.deckName}', language '${plan.language}', platform '${plan.platform}', " +
                "target duration ${plan.targetDurationSeconds}s.",
        )
        appendLine()
        if (plan.augmentedContext.isNotBlank()) {
            appendLine("Augmented context (EAGER/RAG/Graphify/Docs) — anchor every factual claim on it:")
            appendLine(plan.augmentedContext)
            appendLine()
            appendLine("Rules: derive the hook from the augmented context. Do not invent facts or figures.")
        } else {
            appendLine("No augmented context available — base the hook only on the deck's own content, avoid specific claims.")
        }
        appendLine()
        appendLine("Deck slide segments:")
        plan.segments.forEach { segment ->
            appendLine("- ${segment.title}: ${segment.speakerNote.trim()}")
        }
        appendLine()
        appendLine("Produce a single strict JSON object with fields:")
        appendLine("hook (string, <= 3 seconds read aloud), message (string, the point of view), cta (string, closing call to action).")
        appendLine("Respond with the JSON object only.")
    }
}
