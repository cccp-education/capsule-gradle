package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_scenario_context.feature` (CAP-SPD-4).
 *
 * All step definitions are prefixed with "scenario context" signatures to
 * avoid `DuplicateStepDefinitionException` with the shared
 * `capsule.scenarios` glue (bug S-088 — glue is classpath-wide, step texts
 * must be unique).
 */
class CapsuleScenarioContextSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""

    @Given("a Capsule scenario context Gradle project with the capsule plugin applied")
    fun aCapsuleScenarioContextGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-scenario-context-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Given("EAGER governance files are present for scenario context")
    fun eagerGovernanceFilesArePresentForScenarioContext() {
        val agents = projectDir!!.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText("= INDEX — Scenario context demo\nEPIC DEMO-1 : in progress")
        projectDir!!.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir!!.resolve("AGENT.adoc").writeText("Rules : English only.")
    }

    @And("a pedagogical scenario directory is present with metadata and adoc")
    fun aPedagogicalScenarioDirectoryIsPresentWithMetadataAndAdoc() {
        val scenarioDir = projectDir!!.resolve("scenario").apply { mkdirs() }
        scenarioDir.resolve("metadata.json").writeText(
            """{"source":"producer","type":"SPD","sessions":1,"generatedAt":"2026-08-11T10:00:00Z","model":"convention","version":"1.0","dependencies":[]}""",
        )
        scenarioDir.resolve("001_bienvenue.adoc").writeText(
            """
            = Bienvenue dans la Formation FPA
            :module: accueil

            == Objectifs
            - Comprendre le cadre de la formation
            - Connaître les modalités d'évaluation

            == Prérequis
            - Connaissances de base en programmation

            == Modalités d'évaluation
            QCM final + mise en situation
            """.trimIndent(),
        )
    }

    @And("a pedagogical scenario directory is present with malformed metadata and valid adoc")
    fun aPedagogicalScenarioDirectoryIsPresentWithMalformedMetadataAndValidAdoc() {
        val scenarioDir = projectDir!!.resolve("scenario").apply { mkdirs() }
        scenarioDir.resolve("metadata.json").writeText("{not valid json")
        scenarioDir.resolve("001_objectifs.adoc").writeText(
            """
            = Objectifs Pédagogiques
            :module: core

            == Objectifs
            - Goal from valid adoc
            """.trimIndent(),
        )
    }

    @When("I collect the augmented context with scenario file pointing to the scenario directory")
    fun iCollectTheAugmentedContextWithScenarioFilePointingToTheScenarioDirectory() {
        val scenarioPath = projectDir!!.resolve("scenario").absolutePath
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.scenarioFile=$scenarioPath",
        ).build().output
    }

    @When("I collect the augmented context with no scenario configuration")
    fun iCollectTheAugmentedContextWithNoScenarioConfiguration() {
        buildOutput = gradleRunner("collectCapsuleAugmentedContext").build().output
    }

    @Then("the scenario context build succeeds")
    fun theScenarioContextBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the scenario context artefact contains {string}")
    fun theScenarioContextArtefactContains(fragment: String) {
        val content = artefact().readText()
        Assertions.assertTrue(
            content.contains(fragment),
            "Expected '$fragment' in artefact. Got: ${content.take(2000)}",
        )
    }

    @Then("the scenario context artefact omits {string}")
    fun theScenarioContextArtefactOmits(fragment: String) {
        val content = artefact().readText()
        Assertions.assertFalse(
            content.contains(fragment),
            "Expected '$fragment' to be absent from artefact. Got: ${content.take(2000)}",
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