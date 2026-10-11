package capsule.qc

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Renders a [QcBoard] to a PNG grid via Java2D (PRT-08).
 *
 * The board is a diagnostic artefact a human reviews — never content shown to a
 * learner (PRT-11's "no fourth rendering path" concerns *content* rendering,
 * not QC tooling). The renderer is deterministic: same board → same pixels.
 *
 * Missing pictures degrade to a labelled placeholder; the renderer never fails
 * on a missing image.
 */
object QcBoardRenderer {

    private const val PADDING = 16
    private const val HEADER = 56
    private const val CAPTION_LINES = 2
    private const val CAPTION_HEIGHT = 40

    /**
     * Renders [board] into [outputFile] as a PNG.
     *
     * @param columns number of cells per row (>= 1).
     * @param cellWidth  cell pixel width (>= 1).
     * @param cellHeight cell pixel height (>= 1).
     * @return [outputFile] (created, parent directories ensured).
     */
    fun render(
        board: QcBoard,
        outputFile: File,
        columns: Int = 3,
        cellWidth: Int = 480,
        cellHeight: Int = 300,
    ): File {
        require(columns >= 1) { "columns must be >= 1, got $columns" }
        require(cellWidth >= 1 && cellHeight >= 1) { "cell size must be positive" }

        val rows = if (board.cells.isEmpty()) 1 else (board.cells.size + columns - 1) / columns
        val width = PADDING + columns * (cellWidth + PADDING)
        val rowHeight = cellHeight + CAPTION_HEIGHT
        val height = HEADER + PADDING + rows * (rowHeight + PADDING)

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.color = Color.WHITE
            g.fillRect(0, 0, width, height)

            g.color = Color(0x1A, 0x1A, 0x1A)
            g.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
            g.drawString(board.title, PADDING, 38)

            val labelFont = Font(Font.SANS_SERIF, Font.BOLD, 14)
            val captionFont = Font(Font.SANS_SERIF, Font.PLAIN, 12)

            board.cells.forEachIndexed { index, cell ->
                val column = index % columns
                val row = index / columns
                val x = PADDING + column * (cellWidth + PADDING)
                val y = HEADER + row * (rowHeight + PADDING)

                g.color = Color(0xEE, 0xEE, 0xEE)
                g.fillRect(x, y, cellWidth, cellHeight)

                val picture = cell.image?.takeIf { it.isFile }?.let { runCatching { ImageIO.read(it) }.getOrNull() }
                if (picture != null) {
                    g.drawImage(picture, x, y, cellWidth, cellHeight, null)
                } else {
                    g.color = Color(0xBB, 0xBB, 0xBB)
                    g.font = labelFont
                    g.drawString("(no picture)", x + 10, y + 24)
                }

                g.color = Color(0x1A, 0x1A, 0x1A)
                g.font = labelFont
                g.drawString(clip(cell.label, 60), x + 6, y + cellHeight + 18)

                g.color = Color(0x55, 0x55, 0x55)
                g.font = captionFont
                wrap(cell.caption, 72).take(CAPTION_LINES).forEachIndexed { lineIndex, line ->
                    g.drawString(line, x + 6, y + cellHeight + 32 + lineIndex * 14)
                }
            }
        } finally {
            g.dispose()
        }

        outputFile.parentFile?.mkdirs()
        ImageIO.write(image, "png", outputFile)
        return outputFile
    }

    private fun clip(text: String, max: Int): String =
        if (text.length <= max) text else text.take(max - 1) + "…"

    private fun wrap(text: String, max: Int): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > max) {
                lines.add(current.toString())
                current.clear()
                if (lines.size >= CAPTION_LINES) return lines
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty() && lines.size < CAPTION_LINES) lines.add(current.toString())
        return lines
    }
}
