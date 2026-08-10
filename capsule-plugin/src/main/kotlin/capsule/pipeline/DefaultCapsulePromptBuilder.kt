package capsule.pipeline

/**
 * Production [CapsulePromptBuilder] — pedagogical prompts for the capsule
 * content pipeline.
 *
 * The pipeline port returns a single flat prompt string (system instructions
 * + user request concatenated). The generated speaker notes must follow the
 * `capsule.feed` contract ([capsule.feed.AsciidocSpeakerNoteParser]) so that
 * [TtsScriptDeriver] can extract the `[NOTE.speaker]` blocks — the enriched
 * adoc is therefore asked to keep one speaker-note block per `== ` slide.
 */
class DefaultCapsulePromptBuilder : CapsulePromptBuilder {

    override fun buildProposePrompt(state: CapsuleState): String = buildString {
        appendLine("You are a pedagogical content planner for a training capsule.")
        appendLine()
        appendLine(
            "Propose a content plan JSON for deck '${state.deckName}' in language '${state.language}'.",
        )
        appendLine()
        appendLine("Source deck AsciiDoc:")
        appendLine(state.sourceAdoc)
        if (state.augmentedContext.isNotBlank()) {
            appendLine()
            appendLine("Augmented context (EAGER/RAG/Graphify/Docs):")
            appendLine(state.augmentedContext)
        }
        appendLine()
        appendLine("Respond with a single strict JSON object with fields:")
        appendLine("deckName (string), languageCode (ISO 639-1), slideCount (int), speakerNotesFile (string), ttsScriptFile (string).")
    }

    override fun buildGeneratePrompt(state: CapsuleState): String = buildString {
        appendLine("You are a pedagogical scriptwriter for a training capsule.")
        appendLine()
        appendLine(
            "Generate enriched speaker notes AsciiDoc for deck '${state.deckName}' in language '${state.language}'.",
        )
        appendLine()
        appendLine("Content plan JSON:")
        appendLine(state.contentPlanJson)
        appendLine()
        appendLine("Source deck AsciiDoc:")
        appendLine(state.sourceAdoc)
        if (state.augmentedContext.isNotBlank()) {
            appendLine()
            appendLine("Augmented context (EAGER/RAG/Graphify/Docs):")
            appendLine(state.augmentedContext)
            if (state.augmentedContext.contains("SPD Pedagogical Context")) {
                appendLine()
                appendLine(
                    "Speaker notes must address the SPD objectives. Target duration and prerequisites are specified in the SPD Pedagogical Context section.",
                )
            }
        }
        appendLine()
        appendLine("Rules: keep one `== ` level-2 heading per slide; after each heading add a speaker note block:")
        appendLine("[NOTE.speaker]")
        appendLine("--")
        appendLine("<narration for the slide, enriched and pedagogical>")
        appendLine("--")
        appendLine("Respond with the AsciiDoc only.")
    }
}
