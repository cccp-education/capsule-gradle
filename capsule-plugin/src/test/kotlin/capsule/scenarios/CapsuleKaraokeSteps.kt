package capsule.scenarios

import capsule.viral.AssKaraokeDocument
import capsule.viral.KaraokeLine
import capsule.viral.KaraokeWordPlanner
import capsule.viral.StoryboardBeat
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within

/**
 * Cucumber steps for the viral karaoke captions (CAP-VIRAL-E2E US-3).
 *
 * Steps are prefixed "karaoke" to avoid collisions on the shared
 * `capsule.scenarios` glue (bug S-088). Pure domain — planner + ASS document.
 */
class CapsuleKaraokeSteps {

    private var lines: List<KaraokeLine> = emptyList()
    private var document: String? = null

    @Given("a karaoke timeline for beats")
    fun `a karaoke timeline for beats`(table: DataTable) {
        val beats = table.asMaps().map { row ->
            StoryboardBeat(
                role = "development",
                intent = row["intent"]!!.trim(),
                durationSeconds = row["seconds"]!!.trim().toDouble(),
            )
        }
        lines = KaraokeWordPlanner.plan(beats)
    }

    @Then("the karaoke timeline has {int} lines")
    fun `the karaoke timeline has lines`(count: Int) {
        assertThat(lines).hasSize(count)
    }

    @Then("the karaoke timeline spans {double} to {double}")
    fun `the karaoke timeline spans`(start: Double, end: Double) {
        assertThat(lines.first().startTime).isCloseTo(start, within(1e-9))
        assertThat(lines.last().endTime).isCloseTo(end, within(1e-9))
    }

    @Then("the karaoke first line has {int} words")
    fun `the karaoke first line has words`(count: Int) {
        assertThat(lines.first().words).hasSize(count)
    }

    @Then("the karaoke first line words are {string}")
    fun `the karaoke first line words are`(csv: String) {
        assertThat(lines.first().words.map { it.text }).isEqualTo(csv.split(",").map { it.trim() })
    }

    @When("the karaoke ASS document is built")
    fun `the karaoke ASS document is built`() {
        document = AssKaraokeDocument.build(lines)
    }

    @Then("the karaoke document is an ASS v4.00+ document")
    fun `the karaoke document is an ASS document`() {
        assertThat(document).startsWith("[Script Info]")
        assertThat(document).contains("ScriptType: v4.00+")
        assertThat(document).contains("[Events]")
    }

    @Then("the karaoke document has {int} dialogues")
    fun `the karaoke document has dialogues`(count: Int) {
        val found = Regex("^Dialogue: ", RegexOption.MULTILINE).findAll(document!!).count()
        assertThat(found).isEqualTo(count)
    }

    @Then("the karaoke document contains a karaoke override")
    fun `the karaoke document contains a karaoke override`() {
        assertThat(document).contains("{\\k")
    }
}
