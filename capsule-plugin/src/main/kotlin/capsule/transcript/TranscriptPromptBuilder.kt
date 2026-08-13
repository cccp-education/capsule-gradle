package capsule.transcript

/**
 * Pure builder of the LLM enrichment prompt for the transcript feature
 * (CAP-TRANSCRIPT US-2).
 *
 * Consumed by [TranscriptLlmEnhancer] (US-3) when the [TranscriptStrategy] is
 * `LLM`. The prompt embeds the template article produced by [TranscriptBuilder]
 * and instructs the LLM to enrich it with pedagogical transitions, keep the
 * `== ` headings and speaker note content, add introductory and concluding
 * paragraphs, and respond with AsciiDoc only.
 *
 * Pattern: [capsule.pipeline.DefaultCapsulePromptBuilder] (pure prompt builder,
 * no I/O, no LLM call).
 */
object TranscriptPromptBuilder {

    /**
     * Build the enrichment prompt for [plan].
     *
     * @param plan the transcript plan (deck name, segments, language).
     * @return the prompt string sent to the LLM for enrichment.
     */
    fun buildEnhancePrompt(plan: TranscriptPlan): String = buildString {
        appendLine("You are a pedagogical editor for a training capsule transcript.")
        appendLine()
        appendLine("Enrich the AsciiDoc article for deck '${plan.deckName}' in language '${plan.language}'.")
        appendLine()
        appendLine("Template article (keep the structure, enrich the content):")
        appendLine(TranscriptBuilder.build(plan))
        appendLine()
        appendLine("Rules:")
        appendLine("- Keep the '== ' level-2 headings (one per slide).")
        appendLine("- Preserve the speaker note content under each heading.")
        appendLine("- Add pedagogical transitions between sections.")
        appendLine("- Add an engaging introductory paragraph and a concluding paragraph.")
        appendLine("- Respond with the AsciiDoc article only.")
    }
}