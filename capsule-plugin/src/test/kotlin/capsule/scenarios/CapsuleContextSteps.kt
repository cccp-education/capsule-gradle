package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for the CAP-ARCH-2 augmented context collection
 * (`collectCapsuleAugmentedContext`).
 *
 * The task reads the consumer EAGER governance files (INDEX.adoc /
 * PROMPT_REPRISE.adoc / AGENT.adoc) plus the injected RAG / Graphify / Docs
 * channels, assembles the budgeted [contracts.context.CompositeContext] and
 * writes the prompt-ready artefact. No LLM, no network — pure local Gradle
 * execution.
 */
class CapsuleContextSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""
    private var secondRunOutput: String = ""

    // ─── Given ─────────────────────────────────────────────────────

    @Given("a Capsule context Gradle project with the capsule plugin applied")
    fun aCapsuleContextGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-context-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Given("EAGER governance files are present")
    fun eagerGovernanceFilesArePresent() {
        val agents = projectDir!!.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText(
            """
            = INDEX — Cucumber demo
            EPIC DEMO-1 : in progress
            """.trimIndent(),
        )
        projectDir!!.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir!!.resolve("AGENT.adoc").writeText("Rules : English only.")
    }

    // ─── When ──────────────────────────────────────────────────────

    @When("I list the Gradle tasks for the augmented context")
    fun iListTheGradleTasksForTheAugmentedContext() {
        buildOutput = gradleRunner("tasks", "--all").build().output
    }

    @When("I collect the augmented context with RAG content {string}")
    fun iCollectTheAugmentedContextWithRagContent(rag: String) {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=$rag",
        ).build().output
    }

    @When("I collect the augmented context again with RAG content {string}")
    fun iCollectTheAugmentedContextAgainWithRagContent(rag: String) {
        secondRunOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=$rag",
        ).build().output
    }

    @When("I collect the augmented context with a token budget of {int} and a large RAG block")
    fun iCollectTheAugmentedContextWithTightBudgetAndLargeRag(budget: Int) {
        val rag = (1..50).joinToString("\n") { "rag line number $it with filler words" }
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.tokenBudget=$budget",
            "-Pcontext.ragContent=$rag",
        ).build().output
    }

    // ─── Then ──────────────────────────────────────────────────────

    @Then("the augmented context task is listed in the output")
    fun theAugmentedContextTaskIsListedInTheOutput() {
        Assertions.assertTrue(
            buildOutput.contains("collectCapsuleAugmentedContext"),
            "Expected 'collectCapsuleAugmentedContext' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the augmented context build succeeds")
    fun theAugmentedContextBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the augmented context build logs {string}")
    fun theAugmentedContextBuildLogs(fragment: String) {
        Assertions.assertTrue(
            buildOutput.contains(fragment),
            "Expected '$fragment' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the augmented context artefact contains {string}")
    fun theAugmentedContextArtefactContains(fragment: String) {
        val content = artefact().readText()
        Assertions.assertTrue(
            content.contains(fragment),
            "Expected '$fragment' in artefact. Got: ${content.take(2000)}",
        )
    }

    @Then("the augmented context artefact omits the RAG tail {string}")
    fun theAugmentedContextArtefactOmitsTheRagTail(tail: String) {
        val content = artefact().readText()
        Assertions.assertTrue(
            !content.contains(tail),
            "Expected truncated RAG to omit '$tail'. Got: ${content.take(2000)}",
        )
    }

    @Then("the augmented context second run is up-to-date")
    fun theAugmentedContextSecondRunIsUpToDate() {
        Assertions.assertTrue(
            secondRunOutput.contains("UP-TO-DATE"),
            "Expected UP-TO-DATE in second run. Got: ${secondRunOutput.take(2000)}",
        )
    }

    private fun artefact(): File = projectDir!!.resolve("build/capsule/augmented-context.txt")

    private fun gradleRunner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir!!)
}
