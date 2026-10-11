package capsule.scenarios

import capsule.viral.CampaignBundle
import capsule.viral.CampaignBundleSerializer
import capsule.viral.MarketCopy
import capsule.viral.ViralCampaignGate
import capsule.viral.ViralCampaignGateResult
import capsule.viral.ViralPlatform
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps for the viral campaign delivery gate (CAP-VIRAL-E2E US-1).
 *
 * Steps are prefixed "campaign gate"/"campaign manifest" to avoid collisions on
 * the shared `capsule.scenarios` glue (bug S-088). Pure domain — no filesystem,
 * the existence predicate is simulated.
 */
class CapsuleViralGateSteps {

    private var videoPath: String = ""
    private var videoExists: Boolean = true
    private var thumbnailPath: String = ""
    private var thumbnailExists: Boolean = false
    private var bundles: List<CampaignBundle> = emptyList()
    private var verdict: ViralCampaignGateResult? = null
    private var parsed: List<CampaignBundle> = emptyList()

    private fun sampleBundle() = CampaignBundle(
        language = "fr",
        platform = ViralPlatform.TIKTOK,
        videoFile = videoPath,
        thumbnailFile = thumbnailPath,
        metadata = MarketCopy(
            language = "fr",
            platform = ViralPlatform.TIKTOK,
            hook = "h",
            title = "t",
            caption = "c",
        ),
    )

    @Given("a campaign gate manifest with a present video {string}")
    fun `a campaign gate manifest with a present video`(path: String) {
        videoPath = path
        videoExists = true
        thumbnailPath = ""
        thumbnailExists = false
        bundles = listOf(sampleBundle())
    }

    @Given("a campaign gate manifest with a missing video {string}")
    fun `a campaign gate manifest with a missing video`(path: String) {
        videoPath = path
        videoExists = false
        bundles = listOf(sampleBundle())
    }

    @Given("a missing thumbnail {string}")
    fun `a missing thumbnail`(path: String) {
        thumbnailPath = path
        thumbnailExists = false
        bundles = listOf(sampleBundle())
    }

    @When("the campaign gate is evaluated")
    fun `the campaign gate is evaluated`() {
        verdict = ViralCampaignGate.verdict(bundles) { path ->
            (path == videoPath && videoExists) || (path == thumbnailPath && thumbnailExists)
        }
    }

    @Then("the campaign gate verdict is valid")
    fun `the campaign gate verdict is valid`() {
        assertThat(verdict).isEqualTo(ViralCampaignGateResult.Valid)
    }

    @Then("the campaign gate verdict is invalid")
    fun `the campaign gate verdict is invalid`() {
        assertThat(verdict).isInstanceOf(ViralCampaignGateResult.Invalid::class.java)
    }

    @Then("the campaign gate reports {string}")
    fun `the campaign gate reports`(descriptor: String) {
        val invalid = verdict as ViralCampaignGateResult.Invalid
        assertThat(invalid.missing).contains(descriptor)
    }

    @When("the campaign manifest round-trips")
    fun `the campaign manifest round-trips`() {
        val json = CampaignBundleSerializer.toManifestJson(bundles)
        parsed = CampaignBundleSerializer.parseManifest(json)
    }

    @Then("the campaign gate bundles are preserved")
    fun `the campaign gate bundles are preserved`() {
        assertThat(parsed).isEqualTo(bundles)
    }
}
