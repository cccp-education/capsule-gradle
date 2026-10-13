package capsule.feed

import capsule.adapter.revealjs.RevealJsSpeakerNoteParser

/**
 * Parses an AsciiDoc deck source and extracts a [CapsuleScript] made of the
 * slides that carry a `[NOTE.speaker]` open-block.
 *
 * **Legacy compat surface** (CAP-ENGINE-DECOUPLING US-3): the reveal.js parsing
 * now lives in [RevealJsSpeakerNoteParser]; this object delegates so existing
 * call-sites keep working. Prefer the adapter (or the neutral
 * [capsule.kernel.segment.SegmentSource] port) in new code.
 */
object AsciidocSpeakerNoteParser {

    fun parse(adocContent: String, deckName: String): CapsuleScript =
        RevealJsSpeakerNoteParser.parse(adocContent, deckName)
}
