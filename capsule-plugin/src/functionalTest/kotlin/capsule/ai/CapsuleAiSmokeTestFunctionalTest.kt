package capsule.ai

import com.sun.net.httpserver.HttpServer
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import kotlin.test.assertTrue

/**
 * Functional tests for the `capsuleAiSmokeTest` Gradle task (CAP-ARCH-1c).
 *
 * The task resolves a langchain4j [dev.langchain4j.model.chat.ChatModel] via
 * the codebase [codebase.koog.llm.service.LlmBuildService] bridge and issues
 * a minimal smoke call. These tests verify the Gradle surface — registration
 * and a full execution against a mock Ollama-compatible HTTP server
 * (`-Pollama.baseUrl`) — with zero network and zero LLM pool.
 */
class CapsuleAiSmokeTestFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun startMockLlm(responseBody: String): Int {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val port = server.address.port
        val ollamaResponse = """
            {
              "model": "smollm:135m",
              "message": { "role": "assistant", "content": "${responseBody.replace("\"", "\\\"")}" },
              "done": true
            }
        """.trimIndent().toByteArray()
        server.createContext("/api/chat") { exchange ->
            exchange.sendResponseHeaders(200, ollamaResponse.size.toLong())
            exchange.responseBody.use { it.write(ollamaResponse) }
        }
        server.executor = null
        server.start()
        return port
    }

    private fun writeBuildFile() {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Test
    fun `capsuleAiSmokeTest task is registered`() {
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--quiet")
            .withProjectDir(projectDir)
            .build()
        assertTrue(result.output.contains("capsuleAiSmokeTest"), "Expected task in tasks list")
    }

    @Test
    fun `capsuleAiSmokeTest calls the mock LLM and logs the response`() {
        val port = startMockLlm("ok")
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "capsuleAiSmokeTest",
                "-Pollama.baseUrl=http://localhost:$port",
            )
            .withProjectDir(projectDir)
            .build()
        assertTrue(result.output.contains("CAPSULE AI"), "Expected smoke test summary log line")
        assertTrue(result.output.contains("ok"), "Expected mock LLM response in the log")
    }

    @Test
    fun `capsuleAiSmokeTest fails with a clear message when the LLM is unreachable`() {
        writeBuildFile()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "capsuleAiSmokeTest",
                "-Pollama.baseUrl=http://localhost:1",
                "-Pollama.timeout=1",
            )
            .withProjectDir(projectDir)
            .buildAndFail()
        assertTrue(result.output.contains("capsuleAiSmokeTest"), "Expected task name in failure output")
    }
}
