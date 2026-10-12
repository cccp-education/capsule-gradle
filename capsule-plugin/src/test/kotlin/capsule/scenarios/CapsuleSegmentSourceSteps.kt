package capsule.scenarios

import capsule.adapter.revealjs.RevealJsScriptContract
import capsule.adapter.revealjs.RevealJsSpeakerNoteSource
import capsule.kernel.VideoScript
import capsule.kernel.VideoSegment
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Pure-domain BDD steps for `capsule_segment_source.feature`
 * (CAP-ENGINE-DECOUPLING US-3).
 *
 * Exercises [RevealJsSpeakerNoteSource] and [RevealJsScriptContract] directly.
 * Step texts are prefixed with "source"/"segment source"/"deck"/"reveal contract"
 * (S-088 — the glue is classpath-wide).
 */
class CapsuleSegmentSourceSteps {

    private val workDir: File = Files.createTempDirectory("capsule-segment-source").toFile()
    private var deckBody: String = ""
    private var deckName: String = "demo"
    private var segments: List<VideoSegment> = emptyList()
    private var script: VideoScript? = null
    private var readScript: VideoScript? = null

    @Given("a reveal AsciiDoc deck named {string} with narrated slides {string} and {string}")
    fun aRevealAsciiDocDeckNamedWithNarratedSlides(name: String, first: String, second: String) {
        deckName = name
        deckBody = """
            = $name

            == $first
            [NOTE.speaker]
            --
            $first narration.
            --

            == $second
            [NOTE.speaker]
            --
            $second narration.
            --
        """.trimIndent()
    }

    @Given("the reveal deck {string} also contains a silent slide {string}")
    fun theRevealDeckAlsoContainsASilentSlide(name: String, silent: String) {
        deckBody = deckBody.replaceFirst(
            "== ",
            "== $silent\nThis slide carries no speaker note.\n\n== ",
        )
    }

    @When("the reveal speaker-note source reads the deck file")
    fun theRevealSpeakerNoteSourceReadsTheDeckFile() {
        val file = File(workDir, "$deckName-deck.adoc").apply { writeText(deckBody) }
        segments = RevealJsSpeakerNoteSource.segments(file)
    }

    @Then("the segment source yields {int} segments")
    fun theSegmentSourceYieldsSegments(expected: Int) {
        assertEquals(expected, segments.size)
    }

    @Then("the first segment source title is {string}")
    fun theFirstSegmentSourceTitleIs(expected: String) {
        assertEquals(expected, segments.first().title)
    }

    @Given("a video script named {string} with {int} segments")
    fun aVideoScriptNamedWithSegments(name: String, count: Int) {
        script = VideoScript(
            name = name,
            segments = (1..count).map { VideoSegment(index = it, title = "S$it", narration = "n$it.") },
        )
    }

    @When("the script is written and read back by the reveal contract")
    fun theScriptIsWrittenAndReadBackByTheRevealContract() {
        val file = File(workDir, "${script!!.name}-script.txt")
        file.writeText(RevealJsScriptContract.write(script!!))
        readScript = RevealJsScriptContract.read(file)
    }

    @Then("the reveal read script name is {string}")
    fun theRevealReadScriptNameIs(expected: String) {
        assertEquals(expected, readScript?.name)
    }

    @Then("the reveal read script has {int} segments")
    fun theRevealReadScriptHasSegments(expected: Int) {
        assertEquals(expected, readScript?.segments?.size)
    }
}
