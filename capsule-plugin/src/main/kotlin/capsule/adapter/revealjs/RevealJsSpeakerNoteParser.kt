package capsule.adapter.revealjs

import video.kernel.notes.SpeakerNoteBlockParser
import video.kernel.segment.VideoScript
import video.kernel.segment.VideoSegment

/**
 * Reveal.js speaker-note parser — the AsciiDoc-deck adapter of the shared
 * grammar contract (CAP-ENGINE-DECOUPLING US-3, VK-SPEAKER-NOTES US-1b).
 *
 * The **grammar** lives in the kernel ([SpeakerNoteBlockParser]): a
 * `[NOTE.speaker]` marker followed by a `--` block, attached to the preceding
 * level-2 heading. This adapter only maps the neutral blocks onto the
 * capsule video model:
 * - one [VideoSegment] per note block, re-indexed `1..n` in document order;
 * - [VideoSegment.title] = section heading, [VideoSegment.narration] = note body.
 *
 * Segments without a note are not segments for capsule (they cannot be
 * narrated); a second note under the same heading is dropped (first wins —
 * legacy parity, enforced by the kernel parser).
 *
 * Pure — no I/O, no Gradle, no Playwright.
 */
object RevealJsSpeakerNoteParser {
    /** Level-2 heading (`== Title`) starts a reveal.js slide. */
    private const val SLIDE_HEADING_LEVEL: Int = 2

    fun parse(
        adocContent: String,
        deckName: String,
    ): VideoScript {
        require(deckName.isNotBlank()) { "deckName must not be blank" }

        val segments =
            SpeakerNoteBlockParser
                .parse(adocContent, headingLevel = SLIDE_HEADING_LEVEL)
                .mapIndexed { index, block ->
                    VideoSegment(
                        index = index + 1,
                        title = block.title,
                        narration = block.text,
                    )
                }
        return VideoScript(name = deckName, segments = segments)
    }
}
