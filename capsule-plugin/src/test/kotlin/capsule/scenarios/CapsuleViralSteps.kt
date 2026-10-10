package capsule.scenarios

import capsule.feed.SlideType
import capsule.viral.CampaignBundle
import capsule.viral.CampaignBundleSerializer
import capsule.viral.MarketCopy
import capsule.viral.MarketCopySource
import capsule.viral.StoryboardBeat
import capsule.viral.StoryboardValidationResult
import capsule.viral.ViralBatchPlanner
import capsule.viral.ViralCampaignAssembler
import capsule.viral.ViralDeckBuilder
import capsule.viral.ViralHook
import capsule.viral.ViralHookPlan
import capsule.viral.ViralHookPromptBuilder
import capsule.viral.ViralPlatform
import capsule.viral.ViralRenderResult
import capsule.viral.ViralStoryboard
import capsule.viral.ViralStoryboardBuilder
import capsule.viral.ViralStoryboardParser
import capsule.viral.ViralStoryboardValidator
import capsule.viral.ViralTimeFitter
import capsule.viral.ViralVariantRenderPlan
import capsule.viral.ViralVariantRenderer
import capsule.viral.ViralVariant
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import java.io.File
import java.nio.file.Files

/**
 * Cucumber steps for the viral campaign engine (CAP-VIRAL US-8).
 *
 * Steps are prefixed "viral"/"batch"/"campaign"/"storyboard"/"market copy" to
 * avoid collisions on the shared `capsule.scenarios` glue (bug S-088).
 */
class CapsuleViralSteps {

    private var storyboard: ViralStoryboard? = null
    private var verdict: StoryboardValidationResult? = null
    private var document: String? = null
    private var variants: List<ViralVariant> = emptyList()
    private var hookPlan: ViralHookPlan? = null
    private var hookPrompt: String? = null
    private var marketCopy: MarketCopy? = null
    private var bundles: List<CampaignBundle> = emptyList()
    private var parsedStoryboard: ViralStoryboard? = null
    private var manifest: String? = null
    private var deck: String? = null
    private var renderPlan: ViralVariantRenderPlan? = null
    private var renderDir: File? = null
    private var renderResult: ViralRenderResult? = null
    private var fittedDurations: List<Double> = emptyList()
    private var gateTarget: Int = 0
    private var gateTolerance: Double = 0.0

    private fun buildStoryboard(deckName: String, language: String, platform: String, table: DataTable): ViralStoryboard {
        val beats = table.asMaps().map { row ->
            val type = SlideType.valueOf(row["type"]?.uppercase() ?: "HTML")
            StoryboardBeat(
                role = row["role"]!!,
                intent = row["intent"]!!,
                durationSeconds = row["duration"]!!.toDouble(),
                type = type,
            )
        }
        return ViralStoryboard(
            message = "Devenir formateur professionnel",
            angle = "Le métier en 30 secondes",
            beats = beats,
            platform = ViralPlatform.fromString(platform),
            language = language,
            targetDurationSeconds = 30,
        )
    }

    @Given("a viral storyboard for deck {string} in language {string} on platform {string}")
    fun `a viral storyboard for deck`(deckName: String, language: String, platform: String, table: DataTable) {
        storyboard = buildStoryboard(deckName, language, platform, table)
    }

    @When("the storyboard is validated")
    fun `the storyboard is validated`() {
        verdict = ViralStoryboardValidator.validate(storyboard!!)
    }

    @Then("the storyboard verdict is valid")
    fun `the storyboard verdict is valid`() {
        assertThat(verdict).isEqualTo(StoryboardValidationResult.Valid)
    }

    @Then("the storyboard verdict is invalid")
    fun `the storyboard verdict is invalid`() {
        assertThat(verdict).isInstanceOf(StoryboardValidationResult.Invalid::class.java)
    }

    @Then("the storyboard reason mentions {string}")
    fun `the storyboard reason mentions`(needle: String) {
        val invalid = verdict as StoryboardValidationResult.Invalid
        assertThat(invalid.reasons.any { it.contains(needle) })
            .`as`("expected a reason mentioning '$needle' among ${invalid.reasons}")
            .isTrue()
    }

    @When("the storyboard is rendered")
    fun `the storyboard is rendered`() {
        document = ViralStoryboardBuilder.build(storyboard!!)
    }

    @Then("the storyboard document contains {string}")
    fun `the storyboard document contains`(needle: String) {
        assertThat(document).contains(needle)
    }

    @When("the storyboard is rendered as a deck")
    fun `the storyboard is rendered as a deck`() {
        deck = ViralDeckBuilder.build(storyboard!!)
    }

    @Then("the viral deck is a portrait {int}x{int} document")
    fun `the viral deck is a portrait document`(width: Int, height: Int) {
        assertThat(deck).startsWith("<!DOCTYPE html>")
        assertThat(deck).contains("width=$width, height=$height")
    }

    @Then("the viral deck has {int} sections")
    fun `the viral deck has sections`(count: Int) {
        assertThat(Regex("<section").findAll(deck!!).count()).isEqualTo(count)
    }

    @Then("the viral deck contains {string}")
    fun `the viral deck contains`(needle: String) {
        assertThat(deck).contains(needle)
    }

    @Given("a viral batch with languages {string} platforms {string} and duration {int}")
    fun `a viral batch`(languages: String, platforms: String, duration: Int) {
        val langs = languages.split(",").map { it.trim() }
        val plats = platforms.split(",").map { ViralPlatform.fromString(it.trim()) }
        variants = ViralBatchPlanner.plan(langs, plats, duration)
    }

    @Then("the batch contains {int} variants")
    fun `the batch contains variants`(count: Int) {
        assertThat(variants).hasSize(count)
    }

    @Then("the batch variant ids are {string}")
    fun `the batch variant ids are`(csv: String) {
        assertThat(variants.map { it.id }).isEqualTo(csv.split(",").map { it.trim() })
    }

    @Given("a viral hook plan for deck {string} in language {string} with augmented context:")
    fun `a viral hook plan with augmented context`(deckName: String, language: String, context: String) {
        hookPlan = ViralHookPlan(
            deckName = deckName,
            segments = emptyList(),
            language = language,
            platform = ViralPlatform.TIKTOK,
            targetDurationSeconds = 30,
            augmentedContext = context.trim(),
            outputPath = "$deckName-hook.json",
        )
    }

    @When("the viral hook prompt is built")
    fun `the viral hook prompt is built`() {
        hookPrompt = ViralHookPromptBuilder.buildHookPrompt(hookPlan!!)
    }

    @Then("the viral hook prompt contains {string}")
    fun `the viral hook prompt contains`(needle: String) {
        assertThat(hookPrompt).contains(needle)
    }

    @Then("the viral hook prompt forbids inventing facts")
    fun `the viral hook prompt forbids inventing facts`() {
        assertThat(hookPrompt).containsIgnoringCase("do not invent")
    }

    @Given("a market copy for {string} and {string} with title {string}")
    fun `a market copy`(language: String, platform: String, title: String) {
        marketCopy = MarketCopy(
            language = language,
            platform = ViralPlatform.fromString(platform),
            hook = "Hook natif",
            title = title,
            caption = "Caption native",
        )
    }

    @Given("no market copy is available")
    fun `no market copy is available`() {
        marketCopy = null
    }

    @When("the campaign is assembled with a rendered video")
    fun `the campaign is assembled`() {
        assembleCampaign(hook = ViralHook("Accroche", "Message", "CTA"))
    }

    @When("the campaign is assembled with a rendered video and no hook")
    fun `the campaign is assembled without a hook`() {
        assembleCampaign(hook = null)
    }

    private fun assembleCampaign(hook: ViralHook?) {
        val copy = marketCopy
        val source = object : MarketCopySource {
            override fun read(language: String, platform: ViralPlatform): MarketCopy? =
                copy?.takeIf { it.language == language && it.platform == platform }

            override fun isAvailable(): Boolean = copy != null
        }
        bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard!!,
            variants = variants,
            hook = hook,
            copySource = source,
            videoPathFor = { "build/viral/${it.id}.mp4" },
            thumbnailFor = { "build/viral/${it.id}.png" },
        )
    }

    @Then("the campaign contains {int} bundle")
    fun `the campaign contains bundles`(count: Int) {
        assertThat(bundles).hasSize(count)
    }

    @Then("the first campaign bundle title is {string}")
    fun `the first campaign bundle title is`(title: String) {
        assertThat(bundles.first().metadata.title).isEqualTo(title)
    }

    @When("the storyboard is rendered then parsed back")
    fun `the storyboard is rendered then parsed back`() {
        val source = storyboard!!
        document = ViralStoryboardBuilder.build(source)
        parsedStoryboard = ViralStoryboardParser.parse(document!!)
    }

    @Then("the parsed storyboard equals the original")
    fun `the parsed storyboard equals the original`() {
        assertThat(parsedStoryboard).isEqualTo(storyboard)
    }

    @Then("the campaign manifest is a JSON array of {int} bundle")
    fun `the campaign manifest is a JSON array`(count: Int) {
        manifest = CampaignBundleSerializer.toManifestJson(bundles)
        val parsed = com.fasterxml.jackson.databind.ObjectMapper().readTree(manifest)
        assertThat(parsed.isArray).isTrue()
        assertThat(parsed.size()).isEqualTo(count)
    }

    @Then("the campaign manifest first bundle title is {string}")
    fun `the campaign manifest first bundle title is`(title: String) {
        val parsed = com.fasterxml.jackson.databind.ObjectMapper().readTree(manifest)
        assertThat(parsed.get(0).get("metadata").get("title").asText()).isEqualTo(title)
    }

    @Given("a viral render plan for languages {string} platforms {string} duration {int}")
    fun `a viral render plan`(languages: String, platforms: String, duration: Int) {
        val langs = languages.split(",").map { it.trim() }
        val plats = platforms.split(",").map { ViralPlatform.fromString(it.trim()) }
        val dir = Files.createTempDirectory("viral-render").toFile()
        renderDir = dir
        renderPlan = ViralVariantRenderer.plan(ViralBatchPlanner.plan(langs, plats, duration), dir)
    }

    @Given("the variant video {string} already exists")
    fun `the variant video already exists`(id: String) {
        File(renderDir, "$id.mp4").writeText("mp4")
    }

    @When("the render plan is executed with a fake renderer")
    fun `the render plan is executed with a fake renderer`() {
        renderResult = ViralVariantRenderer.render(
            plan = renderPlan!!,
            isRenderable = { it.exists() && it.length() > 0L },
            capture = { target ->
                target.writeText("mp4")
                true
            },
            replicate = { source, target ->
                target.writeText(source.readText())
                true
            },
        )
    }

    @Then("the render plan has {int} entries")
    fun `the render plan has entries`(count: Int) {
        assertThat(renderPlan!!.size).isEqualTo(count)
    }

    @Then("the render plan ids are {string}")
    fun `the render plan ids are`(csv: String) {
        assertThat(renderPlan!!.ids).isEqualTo(csv.split(",").map { it.trim() })
    }

    @Then("the render result rendered is {int}")
    fun `the render result rendered is`(count: Int) {
        assertThat(renderResult!!.renderedCount).isEqualTo(count)
    }

    @Then("the render result skipped is {int}")
    fun `the render result skipped is`(count: Int) {
        assertThat(renderResult!!.skippedCount).isEqualTo(count)
    }

    @Given("a viral time-fit of beats {string} to target {int}")
    fun `a viral time-fit of beats to target`(beats: String, target: Int) {
        fittedDurations = ViralTimeFitter.fit(beats.split(",").map { it.trim().toDouble() }, target)
    }

    @Then("the fitted durations sum to {double}")
    fun `the fitted durations sum to`(expected: Double) {
        assertThat(fittedDurations.sum()).isCloseTo(expected, within(1e-6))
    }

    @Then("the fitted duration at index {int} is {double}")
    fun `the fitted duration at index is`(index: Int, expected: Double) {
        assertThat(fittedDurations[index]).isCloseTo(expected, within(0.01))
    }

    @Given("a viral duration gate with target {int} and tolerance {double}")
    fun `a viral duration gate with target and tolerance`(target: Int, tolerance: Double) {
        gateTarget = target
        gateTolerance = tolerance
    }

    @Then("the duration {double} is fitted")
    fun `the duration is fitted`(seconds: Double) {
        assertThat(ViralTimeFitter.isFitted(seconds, gateTarget, gateTolerance)).isTrue()
    }

    @Then("the duration {double} is not fitted")
    fun `the duration is not fitted`(seconds: Double) {
        assertThat(ViralTimeFitter.isFitted(seconds, gateTarget, gateTolerance)).isFalse()
    }
}
