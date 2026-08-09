package capsule.pipeline

import capsule.feed.AsciidocSpeakerNoteParser
import capsule.feed.CapsuleScriptWriter

/**
 * Pure derivation of the TTS script text from enriched speaker notes AsciiDoc.
 *
 * Reuses the `capsule.feed` domain round-trip: [AsciidocSpeakerNoteParser]
 * extracts the `[NOTE.speaker]` blocks from the generated speaker notes, and
 * [CapsuleScriptWriter] serializes them into the canonical `-script.txt`
 * contract consumed by the existing TTS backend (AGENT.adoc: generateCapsule).
 *
 * Pure — no Gradle, no LLM, no I/O. An adoc without any speaker-note block
 * yields a blank script (the parser drops slides that cannot be narrated).
 */
object TtsScriptDeriver {

    /**
     * Derives the TTS script text for [deckName] from the enriched
     * [speakerNotesAdoc]. Returns a blank string when no `[NOTE.speaker]`
     * block is present in the generated content.
     */
    fun derive(speakerNotesAdoc: String, deckName: String): String {
        require(deckName.isNotBlank()) { "deckName must not be blank" }
        val script = AsciidocSpeakerNoteParser.parse(speakerNotesAdoc, deckName)
        if (script.isEmpty) return ""
        return CapsuleScriptWriter.write(script)
    }
}
