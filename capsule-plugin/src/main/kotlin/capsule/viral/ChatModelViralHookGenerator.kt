package capsule.viral

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel

/**
 * Production adapter for [ViralHookGenerator] — bridges the viral hook port to
 * a langchain4j [ChatModel] (CAP-VIRAL US-4).
 *
 * Builds the hook prompt via [ViralHookPromptBuilder] (which embeds the N1
 * augmented context, anchoring the hook on the corpus), forwards it as a single
 * [UserMessage] (pattern [capsule.transcript.ChatModelTranscriptEnhancer]) and
 * parses the response via [ViralHookParser].
 *
 * Fallback degraded: a blank/unparseable response returns `null` — the caller
 * falls back to the deck's own content (economy of ink: a blank LLM answer is a
 * no-op, not a failure).
 */
class ChatModelViralHookGenerator(private val model: ChatModel) : ViralHookGenerator {

    override fun generate(plan: ViralHookPlan): ViralHook? {
        val prompt = ViralHookPromptBuilder.buildHookPrompt(plan)
        val response = model.chat(UserMessage.from(prompt)).aiMessage().text()
        return ViralHookParser.parse(response)
    }
}
