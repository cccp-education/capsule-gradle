package capsule.pipeline

import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel

/**
 * Production adapter for [CapsuleLlm] — bridges the pipeline port to a
 * langchain4j [ChatModel].
 *
 * The pipeline port is synchronous and returns raw strings; this adapter
 * forwards the full prompt (system instructions + user request already
 * concatenated by [DefaultCapsulePromptBuilder]) as a single [UserMessage].
 * Local Ollama models and most chat models handle the merged prompt
 * identically to a split system+user pair, and keeping a single string
 * contract keeps [CapsulePipelineGraph] free from langchain4j message types.
 */
class ChatModelCapsuleLlm(private val model: ChatModel) : CapsuleLlm {

    override fun propose(prompt: String): String = chat(prompt)

    override fun generate(prompt: String): String = chat(prompt)

    private fun chat(prompt: String): String =
        model.chat(listOf(UserMessage.from(prompt))).aiMessage().text()
}
