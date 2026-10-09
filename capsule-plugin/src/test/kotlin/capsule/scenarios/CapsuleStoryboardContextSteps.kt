package capsule.scenarios

import capsule.viral.NarrativeThread
import capsule.viral.Qqoqcp
import capsule.viral.StoryboardBeat
import capsule.viral.StoryboardValidationResult
import capsule.viral.VisualIdentity
import capsule.viral.ViralPlatform
import capsule.viral.ViralStoryboard
import capsule.viral.ViralStoryboardBuilder
import capsule.viral.ViralStoryboardParser
import capsule.viral.ViralStoryboardValidator
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps for the storyboard context (CAP-CONTEXT US-2/US-3).
 *
 * Steps are prefixed "contextual storyboard" / "contextual verdict" to avoid
 * collisions on the shared `capsule.scenarios` glue (bug S-088).
 */
class CapsuleStoryboardContextSteps {

    private var storyboard: ViralStoryboard? = null
    private var verdict: StoryboardValidationResult? = null
    private var document: String? = null
    private var parsed: ViralStoryboard? = null

    private fun qqoqcp() = Qqoqcp(
        audience = "Développeur solo",
        subject = "Créer un site perso",
        context = "Dépôt local via bakery + opencode",
        moment = "Au démarrage d'un projet",
        modality = "Scaffold → rédaction → bake → publish",
        purpose = "Mettre un site perso en ligne",
    )

    private fun visualIdentity() = VisualIdentity(
        brand = "cheroliv.com",
        format = "9:16 (1080x1920)",
        tokens = linkedMapOf("--accent-color" to "#0d6efd", "--footer-bg" to "#7952b3"),
        typography = "Inter",
    )

    private fun narrativeThread() = NarrativeThread(
        thesis = "Un site perso, ça se génère",
        recurringMotif = "le terminal qui produit le site",
        arc = "accroche → démonstration → résolution → CTA",
    )

    private fun contextual(transitions: List<String>): ViralStoryboard = ViralStoryboard(
        message = "Créer son site perso avec bakery + opencode",
        angle = "L'agent écrit, Gradle bake",
        beats = listOf(
            StoryboardBeat("hook", "Un site perso, sans HTML", 3.0, enteringTransition = transitions[0]),
            StoryboardBeat("development", "Scaffold bakery", 10.0, enteringTransition = transitions[1]),
            StoryboardBeat("cta", "bakery-gradle + opencode", 7.0, enteringTransition = transitions[2]),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 20,
        qqoqcp = qqoqcp(),
        visualIdentity = visualIdentity(),
        narrativeThread = narrativeThread(),
    )

    @Given("a fully contextual viral storyboard")
    fun `a fully contextual viral storyboard`() {
        storyboard = contextual(listOf("— (ouverture)", "cut", "fade"))
    }

    @Given("a context-less viral storyboard")
    fun `a context-less viral storyboard`() {
        storyboard = contextual(listOf("— (ouverture)", "cut", "fade"))
            .copy(qqoqcp = null, visualIdentity = null, narrativeThread = null)
    }

    @Given("a viral storyboard carrying only the QQOQCP axis")
    fun `a viral storyboard carrying only the qqoqcp axis`() {
        storyboard = contextual(listOf("— (ouverture)", "cut", "fade"))
            .copy(visualIdentity = null, narrativeThread = null)
    }

    @Given("a fully contextual viral storyboard with a missing second transition")
    fun `a fully contextual viral storyboard with a missing second transition`() {
        storyboard = contextual(listOf("— (ouverture)", "", "fade"))
    }

    @When("the contextual storyboard is rendered")
    fun `the contextual storyboard is rendered`() {
        document = ViralStoryboardBuilder.build(storyboard!!)
    }

    @When("the contextual storyboard is rendered then parsed back")
    fun `the contextual storyboard is rendered then parsed back`() {
        document = ViralStoryboardBuilder.build(storyboard!!)
        parsed = ViralStoryboardParser.parse(document!!)
    }

    @When("the contextual storyboard is validated")
    fun `the contextual storyboard is validated`() {
        verdict = ViralStoryboardValidator.validate(storyboard!!)
    }

    @Then("the parsed contextual storyboard equals the original")
    fun `the parsed contextual storyboard equals the original`() {
        assertThat(parsed).isEqualTo(storyboard)
    }

    @Then("the contextual storyboard document contains {string}")
    fun `the contextual storyboard document contains`(needle: String) {
        assertThat(document).contains(needle)
    }

    @Then("the contextual storyboard document does not contain {string}")
    fun `the contextual storyboard document does not contain`(needle: String) {
        assertThat(document).doesNotContain(needle)
    }

    @Then("the contextual storyboard verdict is valid")
    fun `the contextual storyboard verdict is valid`() {
        assertThat(verdict).isEqualTo(StoryboardValidationResult.Valid)
    }

    @Then("the contextual storyboard verdict is invalid")
    fun `the contextual storyboard verdict is invalid`() {
        assertThat(verdict).isInstanceOf(StoryboardValidationResult.Invalid::class.java)
    }

    @Then("the contextual verdict reason mentions {string}")
    fun `the contextual verdict reason mentions`(needle: String) {
        val invalid = verdict as StoryboardValidationResult.Invalid
        assertThat(invalid.reasons.any { it.contains(needle) })
            .`as`("expected a reason mentioning '$needle' among ${invalid.reasons}")
            .isTrue()
    }
}
