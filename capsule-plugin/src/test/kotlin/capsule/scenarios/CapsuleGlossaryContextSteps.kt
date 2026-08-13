package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_glossary_context.feature` (CAP-GLOSSARY-3).
 *
 * All step definitions are prefixed with "glossary context" signatures to
 * avoid `DuplicateStepDefinitionException` with the shared
 * `capsule.scenarios` glue (bug S-088 — glue is classpath-wide, step texts
 * must be unique).
 */
class CapsuleGlossaryContextSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""

    @Given("a Capsule glossary context Gradle project with the capsule plugin applied")
    fun aCapsuleGlossaryContextGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-glossary-context-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Given("EAGER governance files are present for glossary context")
    fun eagerGovernanceFilesArePresentForGlossaryContext() {
        val agents = projectDir!!.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText("= INDEX — Glossary context demo\nEPIC DEMO-1 : in progress")
        projectDir!!.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir!!.resolve("AGENT.adoc").writeText("Rules : English only.")
    }

    @And("a glossary adoc file is present with valid terms")
    fun aGlossaryAdocFileIsPresentWithValidTerms() {
        val glossaryFile = projectDir!!.resolve("glossary.adoc")
        glossaryFile.writeText(
            """
            = Formation FPA — Glossaire AFNOR/REAC

            == Glossary

            - competence transversale: capacity mobilizable across contexts, not domain-specific
            - evaluation formative: assessment during learning to improve, not just measure
            - socle de competences: foundational skill set required before advanced modules

            == Other Section

            - unrelated: this bullet must be ignored by the glossary parser
            """.trimIndent(),
        )
    }

    @And("a glossary adoc file is present with no Glossary section")
    fun aGlossaryAdocFileIsPresentWithNoGlossarySection() {
        val glossaryFile = projectDir!!.resolve("glossary.adoc")
        glossaryFile.writeText(
            """
            = Formation FPA — Document without glossary

            == Introduction

            This document has no Glossary section.
            Only introductory content lives here.
            """.trimIndent(),
        )
    }

    @When("I collect the augmented context with glossary file pointing to the glossary adoc")
    fun iCollectTheAugmentedContextWithGlossaryFilePointingToTheGlossaryAdoc() {
        val glossaryPath = projectDir!!.resolve("glossary.adoc").absolutePath
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.glossaryFile=$glossaryPath",
        ).build().output
    }

    @When("I collect the augmented context with no glossary configuration")
    fun iCollectTheAugmentedContextWithNoGlossaryConfiguration() {
        buildOutput = gradleRunner("collectCapsuleAugmentedContext").build().output
    }

    @Then("the glossary context build succeeds")
    fun theGlossaryContextBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the glossary context artefact contains {string}")
    fun theGlossaryContextArtefactContains(fragment: String) {
        val content = artefact().readText()
        Assertions.assertTrue(
            content.contains(fragment),
            "Expected '$fragment' in artefact. Got: ${content.take(2000)}",
        )
    }

    @Then("the glossary context artefact omits {string}")
    fun theGlossaryContextArtefactOmits(fragment: String) {
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