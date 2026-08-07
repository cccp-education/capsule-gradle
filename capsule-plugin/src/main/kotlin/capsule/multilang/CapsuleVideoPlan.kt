package capsule.multilang

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import java.io.File

/**
 * Single capsule video generation unit for one target language.
 *
 * Pairs a translated Reveal.js deck (`<deckName>_<lang>-deck.html` produced by
 * `slider.translateDeck`) with its speaker-notes script
 * (`<deckName>_<lang>-script.txt` produced by `extractSpeakerNotes`) and the
 * output WebM path the video pipeline must write
 * (`<outputDir>/<deckName>_<lang>.webm`).
 *
 * Invariants enforced at construction:
 * - [deckFile] must exist on disk (fail-fast — the pipeline cannot capture a
 *   missing deck).
 * - [scriptFile] must exist on disk (fail-fast — TTS cannot read a missing
 *   script).
 * - [language] must be one of the 10 [LanguageCatalog] codes (the plan never
 *   accepts a language outside the N0 contract).
 *
 * @property deckFile      translated Reveal.js deck HTML file.
 * @property scriptFile    translated speaker-notes script `.txt` file.
 * @property language      target [SupportedLanguage] (N0 contract).
 * @property outputVideo   destination WebM file (not required to exist yet).
 */
data class CapsuleVideoEntry(
    val deckFile: File,
    val scriptFile: File,
    val language: SupportedLanguage,
    val outputVideo: File,
) {
    init {
        require(deckFile.exists()) {
            "deck file not found: ${deckFile.absolutePath}"
        }
        require(scriptFile.exists()) {
            "script file not found: ${scriptFile.absolutePath}"
        }
        require(LanguageCatalog.supportedCodes().contains(language.code)) {
            "language code '${language.code}' is not in LanguageCatalog"
        }
    }
}

/**
 * Immutable plan of capsule video generations across one or more target
 * languages. Built by [CapsuleVideoPlanner] from the translated deck + script
 * artifacts produced upstream by the slider pipeline.
 *
 * Invariant: at least one [CapsuleVideoEntry] (an empty plan has no work to do
 * and signals a misconfiguration upstream — fail-fast rather than silently
 * producing nothing).
 *
 * @property entries non-empty list of per-language video generation units.
 */
data class CapsuleVideoPlan(
    val entries: List<CapsuleVideoEntry>,
) {
    init {
        require(entries.isNotEmpty()) {
            "CapsuleVideoPlan requires at least one entry"
        }
    }

    /** Number of per-language video generations in this plan. */
    fun size(): Int = entries.size

    /** `true` only when the plan holds zero entries (never reachable — the
     *  invariant throws before construction completes). */
    fun isEmpty(): Boolean = entries.isEmpty()

    /** Target languages covered by this plan, in entry order. */
    fun languages(): List<SupportedLanguage> = entries.map { it.language }
}