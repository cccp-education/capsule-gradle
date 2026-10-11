package capsule.scenarios

import capsule.feed.SlideSegment
import capsule.qc.BeatsBoardPlanner
import capsule.qc.QcBoard
import capsule.qc.QcBoardRenderer
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_qc_boards.feature` (PRT-08).
 *
 * Exercises [BeatsBoardPlanner] and [QcBoardRenderer] directly. Step texts are
 * prefixed with "QC board"/"QC deck" (S-088 — the glue is classpath-wide).
 */
class CapsuleQcBoardsSteps {

    private val slides = mutableListOf<SlideSegment>()
    private var board: QcBoard? = null
    private var rendered: File? = null

    @Given("a QC deck slide {int} titled {string} saying {string}")
    fun aQcDeckSlide(index: Int, title: String, note: String) {
        slides.add(SlideSegment(index = index, title = title, speakerNote = note))
    }

    @When("the beats board is planned")
    fun theBeatsBoardIsPlanned() {
        board = BeatsBoardPlanner.plan("demo", slides.toList(), emptyMap())
    }

    @When("the beats board is rendered")
    fun theBeatsBoardIsRendered() {
        val planned = BeatsBoardPlanner.plan("demo", slides.toList(), emptyMap())
        val dir = Files.createTempDirectory("capsule-qc").toFile()
        rendered = QcBoardRenderer.render(planned, File(dir, "beats.png"))
    }

    @Then("the QC board has {int} cells")
    fun theQcBoardHasCells(count: Int) {
        assertEquals(count, board!!.cells.size)
    }

    @Then("the QC board title is {string}")
    fun theQcBoardTitleIs(title: String) {
        assertEquals(title, board!!.title)
    }

    @Then("the QC board image is readable")
    fun theQcBoardImageIsReadable() {
        val file = rendered!!
        assertTrue(file.isFile, "expected the QC board PNG to be written")
        assertNotNull(ImageIO.read(file), "expected a readable PNG")
    }
}
