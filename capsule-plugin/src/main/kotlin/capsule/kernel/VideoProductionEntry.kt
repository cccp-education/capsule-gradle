package capsule.kernel

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import java.io.File

/**
 * Single video production unit for one target language
 * (CAP-ENGINE-DECOUPLING US-1).
 *
 * Replaces the former `capsule.multilang.CapsuleVideoEntry`: the renderer-leaking
 * `deckFile: File` becomes the neutral [VisualSource] abstraction, so a second
 * engine (HyperFrames) can feed the same production pipeline.
 *
 * Invariants enforced at construction:
 * - [visual] file must exist on disk (fail-fast — an engine cannot render a
 *   missing visual source).
 * - [scriptFile] must exist on disk (fail-fast — TTS cannot read a missing
 *   script).
 * - [language] must be one of the [LanguageCatalog] codes (the production never
 *   accepts a language outside the N0 contract).
 *
 * @property visual      engine-neutral visual source (not necessarily a deck).
 * @property scriptFile  narration script file.
 * @property language    target [SupportedLanguage] (N0 contract).
 * @property outputVideo destination video file (not required to exist yet).
 */
data class VideoProductionEntry(
    val visual: VisualSource,
    val scriptFile: File,
    val language: SupportedLanguage,
    val outputVideo: File,
) {
    init {
        require(visual.file.exists()) {
            "visual source not found: ${visual.file.absolutePath}"
        }
        require(scriptFile.exists()) {
            "script file not found: ${scriptFile.absolutePath}"
        }
        require(LanguageCatalog.supportedCodes().contains(language.code)) {
            "language code '${language.code}' is not in LanguageCatalog"
        }
    }
}
