package capsule.scenarios

import capsule.viral.StoryboardBeat
import capsule.viral.ViralPlatform
import capsule.viral.ViralStoryboard
import capsule.viral.ViralStoryboardNormalizer
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

/**
 * Cucumber steps for the viral storyboard normalization.
 *
 * Steps are prefixed "storyboard to normalize"/"normalized" to avoid collisions on
 * the shared `capsule.scenarios` glue (bug S-088). Pure domain.
 */
class CapsuleStoryboardNormalizeSteps {

    private var storyboard: ViralStoryboard = defaultStoryboard()
    private var normalized: ViralStoryboard = defaultStoryboard()
    private var secondPass: ViralStoryboard = defaultStoryboard()

    private fun defaultStoryboard() = ViralStoryboard(
        message = "Maîtriser opencode",
        angle = "L'agent écrit, Gradle assemble",
        beats = listOf(StoryboardBeat("hook", "Et si ?", 3.0)),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 30,
    )

    @Given("a storyboard to normalize with beats")
    fun `a storyboard to normalize with beats`(table: DataTable) {
        val beats = table.asMaps().map { row ->
            StoryboardBeat(
                role = row["role"]!!,
                intent = "Intent ${row["role"]}",
                durationSeconds = 5.0,
                enteringTransition = row["transition"].orEmpty(),
            )
        }
        storyboard = defaultStoryboard().copy(beats = beats)
    }

    @When("the storyboard is normalized")
    fun `the storyboard is normalized`() {
        normalized = ViralStoryboardNormalizer.normalize(storyboard)
        secondPass = ViralStoryboardNormalizer.normalize(normalized)
    }

    @When("the storyboard is normalized twice")
    fun `the storyboard is normalized twice`() {
        normalized = ViralStoryboardNormalizer.normalize(storyboard)
        secondPass = ViralStoryboardNormalizer.normalize(normalized)
    }

    @Then("the normalized beat {int} transition is {string}")
    fun `the normalized beat transition is`(index: Int, transition: String) {
        assertThat(normalized.beats[index].enteringTransition).isEqualTo(transition)
    }

    @Then("the two normalizations are equal")
    fun `the two normalizations are equal`() {
        assertThat(secondPass).isEqualTo(normalized)
    }
}
