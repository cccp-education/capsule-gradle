package capsule.ai

import codebase.koog.llm.LlmProvider
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [LlmProviderChatModelAdapter] — the bridge between
 * codebase's N1 [LlmProvider] (koog, suspend) and langchain4j [ChatModel].
 *
 * Baby-step CAP-ARCH-1b (TDD RED → GREEN): the adapter is a pure bridge —
 * the [LlmProvider] is a `fun interface`, so tests inject a fake lambda and
 * assert the adapter forwards the prompt verbatim and returns the provider
 * response. No Gradle scope, no network.
 */
class LlmProviderChatModelAdapterTest {

    @Test
    fun `adapter returns the LlmProvider response for a single user message`() {
        val adapter = LlmProviderChatModelAdapter(LlmProvider { "mock answer" })
        val response = adapter.chat(listOf(UserMessage("hello")))
        assertEquals("mock answer", response.aiMessage().text())
    }

    @Test
    fun `adapter forwards the exact prompt to the LlmProvider`() {
        val prompts = mutableListOf<String>()
        val adapter = LlmProviderChatModelAdapter(LlmProvider { prompt ->
            prompts.add(prompt)
            "ack"
        })
        adapter.chat(listOf(UserMessage("hello")))
        assertEquals(listOf("hello"), prompts)
    }

    @Test
    fun `adapter concatenates system then user messages into a single prompt`() {
        val prompts = mutableListOf<String>()
        val adapter = LlmProviderChatModelAdapter(LlmProvider { prompt ->
            prompts.add(prompt)
            "ack"
        })
        adapter.chat(listOf(SystemMessage.from("be helpful"), UserMessage("hello")))
        assertEquals(listOf("be helpful\nhello"), prompts)
    }

    @Test
    fun `adapter preserves a multi-user-message prompt order`() {
        val prompts = mutableListOf<String>()
        val adapter = LlmProviderChatModelAdapter(LlmProvider { prompt ->
            prompts.add(prompt)
            "ack"
        })
        adapter.chat(listOf(UserMessage("first"), UserMessage("second")))
        assertTrue(prompts.single().contains("first"))
        assertTrue(prompts.single().contains("second"))
    }
}
