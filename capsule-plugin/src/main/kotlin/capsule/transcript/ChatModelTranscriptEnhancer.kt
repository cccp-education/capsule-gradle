package capsule.transcript

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel

/**
 * Production adapter for [TranscriptLlmEnhancer] — bridges the transcript
 * port to a langchain4j [ChatModel] (CAP-TRANSCRIPT US-3).
 *
 * The port is synchronous and returns a raw string; this adapter builds the
 * enrichment prompt via [TranscriptPromptBuilder], forwards it as a single
 * [UserMessage] (pattern [capsule.pipeline.ChatModelCapsuleLlm] — local
 * Ollama models and most chat models handle the merged prompt identically
 * to a split system+user pair), and reads back the AI message text.
 *
 * Fallback degraded (pattern [capsule.audio.AudioPostProcessor.process]):
 * when the LLM returns a blank/whitespace-only response, the adapter
 * returns the original [template] unchanged — the caller keeps a valid
 * AsciiDoc article rather than an empty one (economy of ink: a blank LLM
 * answer is not a failure, it is a no-op).
 */
class ChatModelTranscriptEnhancer(private val model: ChatModel) : TranscriptLlmEnhancer {

    override fun enhance(template: String, plan: TranscriptPlan): String {
        if (template.isBlank()) return template
        val prompt = TranscriptPromptBuilder.buildEnhancePrompt(plan)
        val response = model.chat(UserMessage.from(prompt)).aiMessage().text()
        return response.takeUnless { it.isNullOrBlank() }?.trim()?.plus("\n") ?: template
    }
}