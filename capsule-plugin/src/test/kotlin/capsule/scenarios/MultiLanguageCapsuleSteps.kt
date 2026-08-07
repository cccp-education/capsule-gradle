package capsule.scenarios

import capsule.multilang.CapsuleVideoPlan
import capsule.multilang.CapsuleVideoPlanner
import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File

/**
 * BDD steps for the CAP-29.5 multi-language capsule pipeline.
 *
 * Two levels are exercised:
 * - pure domain scenarios drive [CapsuleVideoPlanner] directly against isolated
 *   temp directories (plan coverage, output naming, skip rules, fail-fast);
 * - GradleRunner scenarios run the real `generateCapsuleVideoAllLanguages`
 *   task in an isolated project with NoOp engines and assert the produced WebM
 *   artifacts (one per target language, `<deckName>_<lang>.webm` convention).
 */
class MultiLanguageCapsuleSteps {

    private var deckDir: File? = null
    private var scriptDir: File? = null
    private var outputDir: File? = null
    private var plan: CapsuleVideoPlan? = null
    private var planFailure: Throwable? = null
    private var projectDir: File? = null
    private var lastBuildOutput: String = ""

    private fun ensureDirs() {
        if (deckDir == null) {
            val base = File.createTempFile("multilang", ".dir").apply { delete(); mkdirs() }
            deckDir = File(base, "decks").apply { mkdirs() }
            scriptDir = File(base, "scripts").apply { mkdirs() }
            outputDir = File(base, "out").apply { mkdirs() }
        }
    }

    private fun writePair(lang: String) {
        deckDir!!.resolve("demo_$lang-deck.html").writeText(deckHtml(lang))
        scriptDir!!.resolve("demo_$lang-script.txt").writeText(scriptText(lang))
    }

    private fun deckHtml(lang: String): String = """
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Title</h2></section>
  </div>
</div>
</body></html>
    """.trimIndent()

    private fun scriptText(lang: String): String = """
=== CAPSULE SCRIPT : demo_$lang ===
--- SLIDE 1 : Title ---
Note content in $lang.
    """.trimIndent()

    private fun allCatalogLanguages(): List<SupportedLanguage> =
        LanguageCatalog.supportedCodes()
            .mapNotNull { code -> LanguageCatalog.findByCode(code) }

    private fun languagesFor(vararg codes: String): List<SupportedLanguage> =
        codes.map { code -> LanguageCatalog.findByCode(code) ?: error("Unknown LanguageCatalog code: $code") }

    private fun buildPlan(targets: List<SupportedLanguage>) {
        try {
            plan = CapsuleVideoPlanner.plan(
                deckDir = deckDir!!,
                scriptDir = scriptDir!!,
                outputDir = outputDir!!,
                targetLanguages = targets,
            )
            planFailure = null
        } catch (t: Throwable) {
            plan = null
            planFailure = t
        }
    }

    // ─── Given ─────────────────────────────────────────────────────

    @Given("translated decks and scripts for all 10 LanguageCatalog languages")
    fun translatedDecksAndScriptsForAll10LanguageCatalogLanguages() {
        ensureDirs()
        allCatalogLanguages().forEach { writePair(it.code) }
    }

    @Given("translated decks and scripts for languages {string} and {string}")
    fun translatedDecksAndScriptsForLanguages(first: String, second: String) {
        ensureDirs()
        writePair(first)
        writePair(second)
    }

    @Given("translated decks and scripts for RTL languages {string} and {string}")
    fun translatedDecksAndScriptsForRtlLanguages(first: String, second: String) {
        ensureDirs()
        writePair(first)
        writePair(second)
    }

    @Given("no translated decks or scripts")
    fun noTranslatedDecksOrScripts() {
        ensureDirs()
    }

    @Given("a Gradle project with the capsule plugin applied and NoOp engines")
    fun aGradleProjectWithTheCapsulePluginAppliedAndNoOpEngines() {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-multilang-${System.currentTimeMillis()}")
            .also { it.mkdirs() }

        projectDir!!.resolve("settings.gradle").writeText("")
        projectDir!!.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                outputDir = "capsules"
            }
        """.trimIndent())

        deckDir = projectDir!!.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        scriptDir = projectDir!!.resolve("build/capsule").also { it.mkdirs() }
        outputDir = projectDir!!.resolve("build/capsules").also { it.mkdirs() }
    }

    // ─── When ──────────────────────────────────────────────────────

    @When("the multi-language video plan is built")
    fun theMultiLanguageVideoPlanIsBuilt() {
        buildPlan(allCatalogLanguages())
    }

    @When("the multi-language video plan is built for {string} and {string}")
    fun theMultiLanguageVideoPlanIsBuiltForTwo(first: String, second: String) {
        buildPlan(languagesFor(first, second))
    }

    @When("the multi-language video plan is built for {string}, {string} and {string}")
    fun theMultiLanguageVideoPlanIsBuiltForThree(first: String, second: String, third: String) {
        buildPlan(languagesFor(first, second, third))
    }

    @When("I run the multi-language video generation task")
    fun iRunTheMultiLanguageVideoGenerationTask() {
        val project = projectDir ?: error("No Gradle project set up")
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateCapsuleVideoAllLanguages")
            .withProjectDir(project)
            .build()
        lastBuildOutput = result.output
    }

    // ─── Then ──────────────────────────────────────────────────────

    @Then("the plan contains {int} entries")
    fun thePlanContainsEntries(count: Int) {
        Assertions.assertEquals(count, plan?.size(), "Plan size")
    }

    @And("the plan languages include {string} and {string}")
    fun thePlanLanguagesInclude(first: String, second: String) {
        val codes = plan?.languages()?.map { it.code } ?: emptyList()
        Assertions.assertTrue(codes.contains(first), "Plan languages $codes should include '$first'")
        Assertions.assertTrue(codes.contains(second), "Plan languages $codes should include '$second'")
    }

    @And("the plan entry for {string} outputs {string}")
    fun thePlanEntryForOutputs(lang: String, fileName: String) {
        val entry = plan?.entries?.firstOrNull { it.language.code == lang }
            ?: error("No plan entry for language '$lang'")
        Assertions.assertEquals(fileName, entry.outputVideo.name)
    }

    @And("the plan does not include language {string}")
    fun thePlanDoesNotIncludeLanguage(lang: String) {
        val codes = plan?.languages()?.map { it.code } ?: emptyList()
        Assertions.assertFalse(codes.contains(lang), "Plan languages $codes should not include '$lang'")
    }

    @Then("the plan build fails with a message containing {string}")
    fun thePlanBuildFailsWithMessageContaining(fragment: String) {
        Assertions.assertNotNull(planFailure, "Expected the plan build to fail")
        Assertions.assertTrue(
            planFailure!!.message?.contains(fragment) == true,
            "Expected failure message to contain '$fragment' but was: ${planFailure!!.message}",
        )
    }

    @Then("the build output reports {int} rendered languages")
    fun theBuildOutputReportsRenderedLanguages(count: Int) {
        Assertions.assertTrue(
            lastBuildOutput.contains("CAPSULE MULTILANG → $count/$count languages rendered"),
            "Expected 'CAPSULE MULTILANG → $count/$count languages rendered' in build output. " +
                "Got: ${lastBuildOutput.take(2000)}",
        )
    }

    @Then("a video file {string} is generated in the capsules build directory")
    fun aVideoFileIsGeneratedInTheCapsulesBuildDirectory(fileName: String) {
        val project = projectDir ?: error("No Gradle project set up")
        val video = project.resolve("build/capsules/$fileName")
        Assertions.assertTrue(video.exists(), "Expected video at ${video.absolutePath}")
        Assertions.assertTrue(video.length() > 0, "Expected non-empty video at ${video.absolutePath}")
    }
}
