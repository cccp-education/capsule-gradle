package capsule.scenarios

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for `capsule_doc_context.feature` (CAP-DOCCONTEXT-4).
 *
 * All step definitions are prefixed with "doc context" signatures to avoid
 * `DuplicateStepDefinitionException` with the shared `capsule.scenarios` glue
 * (bug S-088 — glue is classpath-wide, step texts must be unique).
 */
class CapsuleDocContextSteps {

    private var projectDir: File? = null
    private var buildOutput: String = ""

    @Given("a Capsule doc context Gradle project with the capsule plugin applied")
    fun aCapsuleDocContextGradleProjectWithTheCapsulePluginApplied() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-doc-context-${System.currentTimeMillis()}")
            .also { it.mkdirs() }
        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent())
    }

    @Given("EAGER governance files are present for doc context")
    fun eagerGovernanceFilesArePresentForDocContext() {
        val agents = projectDir!!.resolve(".agents").apply { mkdirs() }
        agents.resolve("INDEX.adoc").writeText("= INDEX — Doc context demo\nEPIC DEMO-1 : in progress")
        projectDir!!.resolve("PROMPT_REPRISE.adoc").writeText("Mission : generate a demo capsule.")
        projectDir!!.resolve("AGENT.adoc").writeText("Rules : English only.")
    }

    @Given("documentary corpus files are present under {string}")
    fun documentaryCorpusFilesArePresentUnder(dirPath: String) {
        val dir = projectDir!!.resolve(dirPath).apply { mkdirs() }
        dir.resolve("grading-rules.adoc").writeText("AFNOR grading rules for FPA formation.")
        dir.resolve("evaluation-criteria.adoc").writeText("Evaluation criteria for competency blocks.")
    }

    @When("I collect the augmented context with doc content {string}")
    fun iCollectTheAugmentedContextWithDocContent(docsContent: String) {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.docsContent=$docsContent",
        ).build().output
    }

    @When("I collect the augmented context with doc globs {string}")
    fun iCollectTheAugmentedContextWithDocGlobs(globs: String) {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcapsule.context.docsGlobs=$globs",
        ).build().output
    }

    @When("I collect the augmented context with doc content {string} and doc globs {string}")
    fun iCollectTheAugmentedContextWithDocContentAndDocGlobs(docsContent: String, globs: String) {
        buildOutput = gradleRunner(
            "collectCapsuleAugmentedContext",
            "-Pcontext.docsContent=$docsContent",
            "-Pcapsule.context.docsGlobs=$globs",
        ).build().output
    }

    @When("I collect the augmented context with no doc configuration")
    fun iCollectTheAugmentedContextWithNoDocConfiguration() {
        buildOutput = gradleRunner("collectCapsuleAugmentedContext").build().output
    }

    @Then("the doc context build succeeds")
    fun theDocContextBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the doc context artefact contains {string}")
    fun theDocContextArtefactContains(fragment: String) {
        val content = artefact().readText()
        Assertions.assertTrue(
            content.contains(fragment),
            "Expected '$fragment' in artefact. Got: ${content.take(2000)}",
        )
    }

    @Then("the doc context artefact omits {string}")
    fun theDocContextArtefactOmits(fragment: String) {
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