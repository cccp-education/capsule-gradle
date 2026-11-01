package capsule.scenarios

import capsule.viral.StoryboardBeat
import capsule.viral.ThumbnailPlan
import capsule.viral.ViralPlatform
import capsule.viral.ViralStoryboard
import capsule.viral.ViralThumbnailBuilder
import capsule.viral.ViralThumbnailPlanner
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps for the viral campaign cover (CAP-VIRAL-E2E US-2).
 *
 * Steps are prefixed "viral cover"/"viral thumbnail" to avoid collisions on the
 * shared `capsule.scenarios` glue (bug S-088). Pure domain — planner + builder.
 */
class CapsuleViralThumbnailSteps {
    private var storyboard: ViralStoryboard = defaultStoryboard()
    private var plan: ThumbnailPlan? = null
    private var cover: String? = null

    private fun defaultStoryboard() =
        ViralStoryboard(
            message = "Devenir développeur professionnel",
            angle = "Le métier en 30 secondes",
            beats =
                listOf(
                    StoryboardBeat("hook", "Et si 30 secondes suffisaient ?", 3.0),
                    StoryboardBeat("cta", "Découvrez Content", 5.0),
                ),
            platform = ViralPlatform.TIKTOK,
            language = "fr",
            targetDurationSeconds = 30,
        )

    @Given("a viral thumbnail for a storyboard with a hook beat {string}")
    fun aViralThumbnailForAStoryboardWithAHookBeat(hook: String) {
        storyboard =
            defaultStoryboard().copy(
                beats =
                    listOf(
                        StoryboardBeat("hook", hook, 3.0),
                        StoryboardBeat("cta", "Découvrez Content", 5.0),
                    ),
            )
    }

    @Given("a viral thumbnail for a storyboard with no hook beat")
    fun aViralThumbnailForAStoryboardWithNoHookBeat() {
        storyboard = defaultStoryboard().copy(beats = listOf(StoryboardBeat("cta", "Découvrez Content", 5.0)))
    }

    @Given("a viral thumbnail whose hook carries html {string}")
    fun aViralThumbnailWhoseHookCarriesHtml(html: String) {
        storyboard =
            defaultStoryboard().copy(
                beats =
                    listOf(
                        StoryboardBeat("hook", html, 3.0),
                        StoryboardBeat("cta", "Découvrez Content", 5.0),
                    ),
            )
    }

    @When("the viral cover is built")
    fun theViralCoverIsBuilt() {
        plan = ViralThumbnailPlanner.plan(storyboard)
        cover = ViralThumbnailBuilder.build(plan!!)
    }

    @Then("the viral cover is a portrait {int}x{int} document")
    fun theViralCoverIsAPortraitDocument(
        width: Int,
        height: Int,
    ) {
        assertThat(cover).startsWith("<!DOCTYPE html>")
        assertThat(cover).contains("width=$width, height=$height")
        assertThat(plan!!.isPortrait).isTrue()
    }

    @Then("the viral cover carries {string}")
    fun theViralCoverCarries(text: String) {
        assertThat(cover).contains(text)
    }

    @Then("the viral cover escapes html")
    fun theViralCoverEscapesHtml() {
        assertThat(cover).contains("&lt;b&gt;")
        assertThat(cover).doesNotContain("<b>")
    }

    @Then("the viral cover file name for {string} is {string}")
    fun theViralCoverFileName(
        variantId: String,
        expected: String,
    ) {
        assertThat(ViralThumbnailBuilder.fileNameFor(variantId)).isEqualTo(expected)
    }
}
