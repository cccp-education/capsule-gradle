package capsule.qc

import capsule.feed.SlideSegment
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/**
 * TDD unit tests for the PRT-08 QC boards: [BeatsBoardPlanner],
 * [DitMontreBoardPlanner] (pure) and [QcBoardRenderer] (Java2D → PNG).
 */
class QcBoardRendererTest {

    @field:TempDir
    lateinit var workDir: File

    private fun slide(index: Int, title: String, note: String) =
        SlideSegment(index = index, title = title, narration = note)

    @Test
    fun `the beats planner builds one cell per slide`() {
        val board = BeatsBoardPlanner.plan(
            "demo",
            listOf(slide(1, "Intro", "bonjour"), slide(2, "Plan", "le programme")),
            emptyMap(),
        )

        assertEquals("Beats — demo", board.title)
        assertEquals(2, board.cells.size)
        assertEquals("1. Intro", board.cells[0].label)
        assertEquals("bonjour", board.cells[0].caption)
    }

    @Test
    fun `the dit-vs-montre planner orders takes by slide index`() {
        val board = DitMontreBoardPlanner.plan(
            "demo",
            listOf(
                DitMontre(2, "Plan", null, "le programme"),
                DitMontre(1, "Intro", null, "bonjour"),
            ),
        )

        assertEquals(listOf(1, 2), board.cells.map { it.label.substringBefore('.').toInt() })
    }

    @Test
    fun `the renderer writes a readable png`() {
        val board = BeatsBoardPlanner.plan("demo", listOf(slide(1, "Intro", "bonjour")), emptyMap())
        val output = File(workDir, "qc/beats.png")

        val rendered = QcBoardRenderer.render(board, output)

        assertTrue(rendered.isFile, "expected the board PNG to be written")
        val image = ImageIO.read(rendered)
        assertNotNull(image, "expected a readable PNG")
        assertTrue(image.width > 0 && image.height > 0)
    }

    @Test
    fun `a missing picture degrades to a placeholder without failing`() {
        val board = QcBoard(
            title = "dit vs montre",
            cells = listOf(QcCell(label = "1. Intro", image = File(workDir, "absent.png"), caption = "bonjour")),
        )
        val output = File(workDir, "qc/dit.png")

        QcBoardRenderer.render(board, output)

        assertTrue(output.isFile)
    }

    @Test
    fun `an empty board still renders`() {
        val output = File(workDir, "qc/empty.png")
        QcBoardRenderer.render(QcBoard("empty", emptyList()), output)
        assertTrue(output.isFile)
    }
}
