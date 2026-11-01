package capsule.scenarios

import capsule.viral.AssKaraokeDocument
import capsule.viral.KaraokeLine
import capsule.viral.KaraokeWordPlanner
import capsule.viral.StoryboardBeat
import capsule.viral.TimedWord
import capsule.viral.WordTimingsJson
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
    private var beats: List<StoryboardBeat> = emptyList()
    private var timings: List<TimedWord> = emptyList()

    @Given("karaoke beats")
    fun karaokeBeats(table: DataTable) {
        beats =
            table.asMaps().map { row ->
                StoryboardBeat(
                    role = "development",
                    intent = row["intent"]!!.trim(),
                    durationSeconds = row["seconds"]!!.trim().toDouble(),
                )
            }
    }

    @Given("karaoke word timings")
    fun karaokeWordTimings(table: DataTable) {
        timings =
            table.asMaps().map { row ->
                TimedWord(
                    text = WordTimingsJson.normalize(row["word"]!!),
                    start = row["start"]!!.trim().toDouble(),
                    end = row["end"]!!.trim().toDouble(),
                )
            }
    }

    @When("the karaoke timeline is planned with timings")
    fun theKaraokeTimelineIsPlannedWithTimings() {
        lines = KaraokeWordPlanner.plan(beats, timings)
    }

    @Then("the karaoke first line first word starts at {double}")
    fun theKaraokeFirstLineFirstWordStartsAt(value: Double) {
        assertThat(
            lines
                .first()
                .words
                .first()
                .startTime,
        ).isCloseTo(value, within(1e-9))
    }

    @Then("the karaoke first line last word ends at {double}")
    fun theKaraokeFirstLineLastWordEndsAt(value: Double) {
        assertThat(
            lines
                .first()
                .words
                .last()
                .endTime,
        ).isCloseTo(value, within(1e-9))
    }

    @Given("a karaoke timeline for beats")
    fun aKaraokeTimelineForBeats(table: DataTable) {
        val beats =
            table.asMaps().map { row ->
                StoryboardBeat(
                    role = "development",
                    intent = row["intent"]!!.trim(),
                    durationSeconds = row["seconds"]!!.trim().toDouble(),
                )
            }
        lines = KaraokeWordPlanner.plan(beats)
    }

    @Then("the karaoke timeline has {int} lines")
    fun theKaraokeTimelineHasLines(count: Int) {
        assertThat(lines).hasSize(count)
    }

    @Then("the karaoke timeline spans {double} to {double}")
    fun theKaraokeTimelineSpans(
        start: Double,
        end: Double,
    ) {
        assertThat(lines.first().startTime).isCloseTo(start, within(1e-9))
        assertThat(lines.last().endTime).isCloseTo(end, within(1e-9))
    }

    @Then("the karaoke first line has {int} words")
    fun theKaraokeFirstLineHasWords(count: Int) {
        assertThat(lines.first().words).hasSize(count)
    }

    @Then("the karaoke first line words are {string}")
    fun theKaraokeFirstLineWordsAre(csv: String) {
        assertThat(lines.first().words.map { it.text }).isEqualTo(csv.split(",").map { it.trim() })
    }

    @When("the karaoke ASS document is built")
    fun theKaraokeAssDocumentIsBuilt() {
        document = AssKaraokeDocument.build(lines)
    }

    @Then("the karaoke document is an ASS v4.00+ document")
    fun theKaraokeDocumentIsAnAssDocument() {
        assertThat(document).startsWith("[Script Info]")
        assertThat(document).contains("ScriptType: v4.00+")
        assertThat(document).contains("[Events]")
    }

    @Then("the karaoke document has {int} dialogues")
    fun theKaraokeDocumentHasDialogues(count: Int) {
        val found = Regex("^Dialogue: ", RegexOption.MULTILINE).findAll(document!!).count()
        assertThat(found).isEqualTo(count)
    }

    @Then("the karaoke document contains a karaoke override")
    fun theKaraokeDocumentContainsAKaraokeOverride() {
        assertThat(document).contains("{\\k")
    }
}
