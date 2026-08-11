package capsule.scenarios

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_provenance.feature` (CAP-PROVENANCE US-2).
 *
 * All step definitions are prefixed with "provenance" signatures to avoid
 * `DuplicateStepDefinitionException` with the shared `capsule.scenarios` glue
 * (bug S-088 — glue is classpath-wide, step texts must be unique).
 */
class CapsuleProvenanceSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""

    private val mapper: ObjectMapper = ObjectMapper()

    @Given("a Capsule provenance Gradle project with the capsule plugin applied")
    fun aCapsuleProvenanceGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-provenance-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @And("EAGER governance files are present for provenance")
    fun eagerGovernanceFilesArePresentForProvenance() {
        val agents = projectDir!!.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText("= INDEX — Provenance demo\nEPIC DEMO-1 : in progress")
        projectDir!!.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir!!.resolve("AGENT.adoc").writeText("Rules : English only, no commits without permission.")
    }

    @When("I collect the augmented context with RAG and Graphify content for provenance")
    fun iCollectTheAugmentedContextWithRagAndGraphifyContentForProvenance() {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=similarity: 0.98 — RAG nugget about grading",
            "-Pcontext.graphifyContent=node: capsule -> edge: feeds -> node: video",
        ).build().output
    }

    @When("I collect the augmented context with RAG content for provenance")
    fun iCollectTheAugmentedContextWithRagContentForProvenance() {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.ragContent=similarity: 0.98 — RAG nugget about grading",
        ).build().output
    }

    @When("I collect the augmented context with no content for provenance")
    fun iCollectTheAugmentedContextWithNoContentForProvenance() {
        buildOutput = gradleRunner("collectCapsuleAugmentedContext").build().output
    }

    @Then("the provenance build succeeds")
    fun theProvenanceBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the provenance artefact contains {int} channels")
    fun theProvenanceArtefactContainsChannels(expected: Int) {
        Assertions.assertEquals(
            expected,
            provenance()["channels"].size(),
            "Expected $expected provenance channels. Got: ${provenance().toPrettyString()}",
        )
    }

    @Then("the provenance EAGER channel has {int} sources")
    fun theProvenanceEagerChannelHasSources(expected: Int) {
        val eager = provenance()["channels"].first { it["channel"].asText() == "EAGER" }
        Assertions.assertEquals(
            expected,
            eager["sources"].size(),
            "Expected $expected EAGER sources",
        )
    }

    @Then("the provenance artefact contains a {string} source")
    fun theProvenanceArtefactContainsASource(fileName: String) {
        val channels = provenance()["channels"]
        Assertions.assertTrue(
            channels.any { channel ->
                channel["sources"].any { it["fileName"].asText() == fileName }
            },
            "Expected a source named '$fileName' in provenance. Got: ${provenance().toPrettyString()}",
        )
    }

    @Then("the provenance log line mentions the provenance artefact")
    fun theProvenanceLogLineMentionsTheProvenanceArtefact() {
        Assertions.assertTrue(
            buildOutput.contains("CAPSULE PROVENANCE →") && buildOutput.contains("context-provenance.json"),
            "Expected CAPSULE PROVENANCE log line. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the provenance EAGER sources are named adoc governance files")
    fun theProvenanceEagerSourcesAreNamedAdocGovernanceFiles() {
        val eager = provenance()["channels"].first { it["channel"].asText() == "EAGER" }
        Assertions.assertTrue(
            eager["sources"].all { it["fileName"].asText().endsWith(".adoc") },
            "EAGER sources must be the adoc governance files. Got: ${eager.toPrettyString()}",
        )
    }

    @Then("the provenance EAGER sources have positive chars and tokens")
    fun theProvenanceEagerSourcesHavePositiveCharsAndTokens() {
        val eager = provenance()["channels"].first { it["channel"].asText() == "EAGER" }
        Assertions.assertTrue(
            eager["sources"].all { it["chars"].asInt() > 0 && it["tokens"].asInt() > 0 },
            "EAGER sources must carry measured chars + estimated tokens. Got: ${eager.toPrettyString()}",
        )
    }

    @Then("the provenance total tokens is positive")
    fun theProvenanceTotalTokensIsPositive() {
        Assertions.assertTrue(
            provenance()["totalTokens"].asInt() > 0,
            "Expected positive total tokens. Got: ${provenance().toPrettyString()}",
        )
    }

    @Then("the provenance JSON has empty channels")
    fun theProvenanceJsonHasEmptyChannels() {
        Assertions.assertTrue(
            provenance()["channels"].isEmpty,
            "Expected empty provenance channels. Got: ${provenance().toPrettyString()}",
        )
    }

    @Then("the provenance JSON has zero total chars and tokens")
    fun theProvenanceJsonHasZeroTotalCharsAndTokens() {
        Assertions.assertEquals(0, provenance()["totalChars"].asInt())
        Assertions.assertEquals(0, provenance()["totalTokens"].asInt())
    }

    private fun provenance(): JsonNode {
        val file = projectDir!!.resolve("build/capsule/context-provenance.json")
        Assertions.assertTrue(file.exists(), "Expected provenance artefact at ${file.absolutePath}")
        return mapper.readTree(file.readText())
    }

    private fun gradleRunner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir!!)
}
