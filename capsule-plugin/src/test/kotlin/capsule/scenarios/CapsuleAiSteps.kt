package capsule.scenarios

import com.sun.net.httpserver.HttpServer
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File
import java.net.InetSocketAddress

/**
 * BDD steps for the CAP-ARCH-1 capsule AI bridge (codebase LlmBuildService).
 *
 * The smoke test task runs against a local mock Ollama HTTP server (no real
 * LLM, no pool dependency), verifying registration, model resolution and the
 * end-to-end chat call through the CapsuleLlmService bridge.
 */
class CapsuleAiSteps {

    private var projectDir: File? = null
    private var mockServer: HttpServer? = null
    private var buildOutput: String = ""

    // ─── Given ─────────────────────────────────────────────────────

    @Given("a Capsule AI Gradle project with the capsule plugin applied")
    fun aCapsuleAiGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-ai-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Given("a mock Ollama LLM server answering {string}")
    fun aMockOllamaLlmServerAnswering(responseBody: String) {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        val ollamaResponse = """
            {
              "model": "smollm:135m",
              "message": { "role": "assistant", "content": "$responseBody" },
              "done": true
            }
        """.trimIndent().toByteArray()
        server.createContext("/api/chat") { exchange ->
            exchange.sendResponseHeaders(200, ollamaResponse.size.toLong())
            exchange.responseBody.use { it.write(ollamaResponse) }
        }
        server.executor = null
        server.start()
        mockServer = server
    }

    // ─── When ──────────────────────────────────────────────────────

    @When("I list the Gradle tasks")
    fun iListTheGradleTasks() {
        buildOutput = gradleRunner("tasks", "--all").build().output
    }

    @When("I run the capsule AI smoke test against the mock LLM")
    fun iRunTheCapsuleAiSmokeTestAgainstTheMockLlm() {
        val port = mockServer!!.address.port
        buildOutput = gradleRunner(
            "capsuleAiSmokeTest",
            "-Pollama.baseUrl=http://localhost:$port",
        ).build().output
    }

    @When("I run the capsule AI smoke test without a reachable LLM")
    fun iRunTheCapsuleAiSmokeTestWithoutAReachableLlm() {
        buildOutput = gradleRunner(
            "capsuleAiSmokeTest",
            "-Pollama.baseUrl=http://localhost:1",
            "-Pollama.timeout=1",
        ).buildAndFail().output
    }

    // ─── Then ──────────────────────────────────────────────────────

    @Then("the task {string} is listed in the output")
    fun theTaskIsListedInTheOutput(taskName: String) {
        Assertions.assertTrue(
            buildOutput.contains(taskName),
            "Expected task '$taskName' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the build succeeds")
    fun theBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the build output logs {string}")
    fun theBuildOutputLogs(fragment: String) {
        Assertions.assertTrue(
            buildOutput.contains(fragment),
            "Expected '$fragment' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the build output logs the mock response {string}")
    fun theBuildOutputLogsTheMockResponse(response: String) {
        Assertions.assertTrue(
            buildOutput.contains(response),
            "Expected mock response '$response' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the build fails when the LLM is unreachable")
    fun theBuildFailsWhenTheLlmIsUnreachable() {
        Assertions.assertTrue(
            buildOutput.contains("FAILURE"),
            "Expected a failing build. Got: ${buildOutput.take(2000)}",
        )
    }

    private fun gradleRunner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir!!)
}
