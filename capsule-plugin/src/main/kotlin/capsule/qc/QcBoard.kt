package capsule.qc

import capsule.feed.SlideSegment
import java.io.File

/**
 * One cell of a visual QC board (PRT-08): a label, an optional picture and a
 * caption (the spoken text, an annotation…).
 */
data class QcCell(
    val label: String,
    val image: File?,
    val caption: String,
)

/**
 * A visual QC board (PRT-08) — a titled grid of [QcCell]s rendered to a PNG a
 * human reviews before shipping a capsule. Ported from the review boards of
 * `prepa_electricite_complet`.
 */
data class QcBoard(
    val title: String,
    val cells: List<QcCell>,
)

/**
 * The "dit vs montré" review input (PRT-08): what the slide showed ([frame]) and
 * what the voice was meant to say ([spoken]).
 */
data class DitMontre(
    val slideIndex: Int,
    val title: String,
    val frame: File?,
    val spoken: String,
)

/**
 * Builds the "beats" QC board — the final state of every slide (PRT-08).
 *
 * Pure — no I/O. The slide pictures are passed in (the caller resolves the
 * capture PNGs); a missing picture renders as a placeholder.
 */
object BeatsBoardPlanner {

    fun plan(deckName: String, slides: List<SlideSegment>, imagesByIndex: Map<Int, File>): QcBoard =
        QcBoard(
            title = "Beats — $deckName",
            cells = slides.map { slide ->
                QcCell(
                    label = "${slide.index}. ${slide.title}",
                    image = imagesByIndex[slide.index],
                    caption = slide.speakerNote.replace(Regex("\\s+"), " ").trim(),
                )
            },
        )
}

/**
 * Builds the "dit vs montré" QC board — for each slide, the picture that was on
 * screen next to the narration that was spoken (PRT-08).
 *
 * Pure — no I/O. The frames are passed in (the caller extracts them); a missing
 * frame renders as a placeholder.
 */
object DitMontreBoardPlanner {

    fun plan(deckName: String, takes: List<DitMontre>): QcBoard =
        QcBoard(
            title = "Dit vs montré — $deckName",
            cells = takes.sortedBy { it.slideIndex }.map { take ->
                QcCell(
                    label = "${take.slideIndex}. ${take.title}",
                    image = take.frame,
                    caption = take.spoken.replace(Regex("\\s+"), " ").trim(),
                )
            },
        )
}
