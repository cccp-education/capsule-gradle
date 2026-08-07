package capsule.multilang

/**
 * Piper TTS models + espeak voices per language code (ISO 639-1).
 *
 * Covers the 10 [contracts.i18n.LanguageCatalog] languages. The 4 historical
 * mappings (fr/en/es/de) are preserved verbatim for backward compatibility;
 * the 7 new mappings (zh/hi/ar/bn/pt/ru/ur) use named Piper models + native
 * espeak codes.
 */
object VoiceMapping {

    private val piperModelByCode: Map<String, String> = mapOf(
        "fr" to "fr_FR-siwis-medium",
        "en" to "en_US-lessac-medium",
        "es" to "es_ES-carlfm-x_low",
        "de" to "de_DE-thorsten-medium",
        "zh" to "zh_CN-huayan-medium",
        "hi" to "hi_IN-cmu-medium",
        "ar" to "ar_JO-kareem-medium",
        "bn" to "bn_IN-mms-medium",
        "pt" to "pt_BR-francisca-medium",
        "ru" to "ru_RU-irina-medium",
        "ur" to "ur_PK-gul-medium",
    )

    private val espeakVoiceByCode: Map<String, String> = mapOf(
        "fr" to "fr",
        "en" to "en",
        "es" to "es",
        "de" to "de",
        "zh" to "zh",
        "hi" to "hi",
        "ar" to "ar",
        "bn" to "bn",
        "pt" to "pt",
        "ru" to "ru",
        "ur" to "ur",
    )

    private val codeByPiperModel: Map<String, String> =
        piperModelByCode.entries.associate { (code, model) -> model to code }

    private val codeByEspeakVoice: Map<String, String> =
        espeakVoiceByCode.entries.associate { (code, voice) -> voice to code }

    fun piperModel(code: String): String? = piperModelByCode[code]

    fun espeakVoice(code: String): String? = espeakVoiceByCode[code]

    fun codeByPiperModel(model: String): String? = codeByPiperModel[model]

    fun codeByEspeakVoice(voice: String): String? = codeByEspeakVoice[voice]

    fun supportedCodes(): Set<String> = piperModelByCode.keys
}