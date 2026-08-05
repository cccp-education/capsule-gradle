package capsule.scenarios

import capsule.feed.CapsuleScript
import capsule.feed.CapsuleScriptReader
import capsule.feed.CapsuleScriptWriter
import capsule.feed.SlideSegment
import capsule.feed.SlideType
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions
import java.io.File

class ConsolidatedCapsuleModelSteps {

    private var script: CapsuleScript? = null
    private var writtenFile: File? = null
    private var readScript: CapsuleScript? = null
    private var rawFile: File? = null

    @Given("a capsule script with deck name {string} and segments")
    fun aCapsuleScriptWithDeckNameAndSegments(deckName: String, table: DataTable) {
        val rows = table.asMaps()
        val segments = rows.mapIndexed { i, row ->
            val index = (row["index"]?.toIntOrNull() ?: (i + 1))
            val title = row["title"] ?: error("Missing title at row $i")
            val speakerNote = row["speakerNote"] ?: error("Missing speakerNote at row $i")
            val typeRaw = row["type"]?.trim()?.uppercase()
            val type = if (typeRaw == "MANIM") SlideType.MANIM else SlideType.HTML
            val manimScene = row["manimScene"]?.takeIf { it.isNotBlank() }
            SlideSegment(
                index = index,
                title = title,
                speakerNote = speakerNote,
                type = type,
                manimScene = manimScene,
            )
        }
        script = CapsuleScript(deckName = deckName, segments = segments)
    }

    @Given("a raw script file {string} with content")
    fun aRawScriptFileWithContent(fileName: String, content: String) {
        val temp = File.createTempFile(fileName.substringBeforeLast('.'), ".txt")
        temp.writeText(content.trimIndent())
        rawFile = temp
    }

    @When("the script is written to a file and read back")
    fun theScriptIsWrittenToAFileAndReadBack() {
        val source = script ?: error("No source script set")
        val temp = File.createTempFile("round-trip", "-script.txt")
        temp.writeText(CapsuleScriptWriter.write(source))
        writtenFile = temp
        readScript = CapsuleScriptReader.read(temp)
    }

    @When("the file is read as a capsule script")
    fun theFileIsReadAsACapsuleScript() {
        val file = rawFile ?: error("No raw script file set")
        readScript = CapsuleScriptReader.read(file)
    }

    @Then("the read script deck name should be {string}")
    fun theReadScriptDeckNameShouldBe(expected: String) {
        Assertions.assertEquals(expected, readScript?.deckName)
    }

    @And("the read script should contain {int} segment(s)")
    fun theReadScriptShouldContainSegments(count: Int) {
        Assertions.assertEquals(count, readScript?.segments?.size)
    }

    @And("the segment {int} should have title {string}")
    fun theSegmentShouldHaveTitle(index: Int, title: String) {
        val seg = findSegment(index)
        Assertions.assertEquals(title, seg.title)
    }

    @And("the segment {int} should have speakerNote {string}")
    fun theSegmentShouldHaveSpeakerNote(index: Int, note: String) {
        val seg = findSegment(index)
        Assertions.assertEquals(note, seg.speakerNote)
    }

    @And("the segment {int} should have type {string}")
    fun theSegmentShouldHaveType(index: Int, typeRaw: String) {
        val seg = findSegment(index)
        val expected = if (typeRaw.uppercase() == "MANIM") SlideType.MANIM else SlideType.HTML
        Assertions.assertEquals(expected, seg.type)
    }

    @And("the segment {int} should have manimScene {string}")
    fun theSegmentShouldHaveManimScene(index: Int, sceneRaw: String) {
        val seg = findSegment(index)
        val expected: String? = sceneRaw.takeIf { it != "null" && it.isNotBlank() }
        Assertions.assertEquals(expected, seg.manimScene)
    }

    private fun findSegment(index: Int): SlideSegment {
        val segments = readScript?.segments ?: error("No read script")
        return segments.firstOrNull { it.index == index }
            ?: segments.getOrNull(index - 1)
            ?: error("Segment $index not found")
    }
}