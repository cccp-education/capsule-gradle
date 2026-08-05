package capsule.feed

/**
 * Parses an AsciiDoc deck source and extracts a [CapsuleScript] made of the
 * slides that carry a `[NOTE.speaker]` open-block. Slides without a speaker
 * note block are skipped (they cannot be narrated by capsule-gradle).
 *
 * Recognised slide heading: a level-2 AsciiDoc heading `== Title`.
 * Attribute lines preceding the heading (e.g. `[.slide,data-transition="zoom"]`)
 * are tolerated — only the `== ` line itself is used as the slide title.
 * Level-3+ headings (`===`, `====`...) are NOT treated as slides.
 *
 * Speaker note block delimiters:
 *   [NOTE.speaker]            or   [NOTE.speaker, ...attrs]
 *   --
 *   note body
 *   --
 */
object AsciidocSpeakerNoteParser {

    fun parse(adocContent: String, deckName: String): CapsuleScript {
        require(deckName.isNotBlank()) { "deckName must not be blank" }

        val lines = adocContent.lines()
        val rawSegments = mutableListOf<SlideSegment>()

        var currentTitle: String? = null
        var inNote = false
        var noteOpened = false
        val noteLines = mutableListOf<String>()

        for (line in lines) {
            when {
                isSlideHeading(line) -> {
                    flush(rawSegments, currentTitle, noteLines)
                    currentTitle = line.removePrefix("==").trim()
                    inNote = false
                    noteOpened = false
                    noteLines.clear()
                }
                isSpeakerNoteStart(line) -> {
                    inNote = true
                    noteOpened = false
                    noteLines.clear()
                }
                inNote && !noteOpened && line.trimStart().startsWith("--") -> {
                    noteOpened = true
                }
                inNote && noteOpened && line.trimStart().startsWith("--") -> {
                    flush(rawSegments, currentTitle, noteLines)
                    currentTitle = null
                    inNote = false
                    noteOpened = false
                    noteLines.clear()
                }
                inNote && noteOpened -> noteLines.add(line)
            }
        }
        flush(rawSegments, currentTitle, noteLines)

        val reindexed = rawSegments.mapIndexed { i, seg ->
            SlideSegment(index = i + 1, title = seg.title, speakerNote = seg.speakerNote)
        }
        return CapsuleScript(deckName = deckName, segments = reindexed)
    }

    private fun flush(
        sink: MutableList<SlideSegment>,
        title: String?,
        noteLines: List<String>,
    ) {
        if (title == null) return
        val note = noteLines.joinToString("\n").trim()
        if (note.isBlank()) return
        sink.add(SlideSegment(index = -1, title = title, speakerNote = note))
    }

    private fun isSlideHeading(line: String): Boolean =
        line.startsWith("== ") && !line.startsWith("=== ")

    private fun isSpeakerNoteStart(line: String): Boolean {
        val t = line.trimStart()
        return t.startsWith("[NOTE.speaker]") || t.startsWith("[NOTE.speaker,")
    }
}