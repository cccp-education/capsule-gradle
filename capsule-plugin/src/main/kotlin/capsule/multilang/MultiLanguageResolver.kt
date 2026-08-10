package capsule.multilang

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage

/**
 * Resolved language bundle for the capsule multi-language pipeline.
 *
 * Holds the [SupportedLanguage] N0 contract (code, name, nativeName, rtl,
 * localeTag) alongside the resolved Piper model + espeak voice for TTS.
 */
data class ResolvedLanguage(
    val language: SupportedLanguage,
    val piperModel: String,
    val espeakVoice: String,
)

/**
 * Stateless resolver bridging [LanguageCatalog] (i18n-contracts N0) with the
 * [VoiceMapping] Piper/espeak table. Replaces the deleted `Language` enum +
 * `Language.fromCode()` hardcoding.
 *
 * Pattern: direct consumption of `LanguageCatalog`/`SupportedLanguage` N0
 * contracts (same as plantuml-gradle `PlantumlTranslationServiceAdapter` and
 * slider-gradle `DeckTranslator`). Log-message i18n is handled by
 * [capsule.i18n.CapsuleMessages] (cross-borough convention with
 * `SliderMessages` and `PlantumlMessages`).
 */
object MultiLanguageResolver {

    fun resolve(code: String): ResolvedLanguage? {
        val language = LanguageCatalog.findByCode(code) ?: return null
        val piperModel = VoiceMapping.piperModel(code) ?: return null
        val espeakVoice = VoiceMapping.espeakVoice(code) ?: return null
        return ResolvedLanguage(language, piperModel, espeakVoice)
    }

    fun resolveByPiperModel(model: String): SupportedLanguage? =
        VoiceMapping.codeByPiperModel(model)?.let { LanguageCatalog.findByCode(it) }

    fun resolveByEspeakVoice(voice: String): SupportedLanguage? =
        VoiceMapping.codeByEspeakVoice(voice)?.let { LanguageCatalog.findByCode(it) }

    fun piperModel(code: String): String? = VoiceMapping.piperModel(code)

    fun espeakVoice(code: String): String? = VoiceMapping.espeakVoice(code)

    val supportedCodes: Set<String> get() = LanguageCatalog.supportedCodes()
}