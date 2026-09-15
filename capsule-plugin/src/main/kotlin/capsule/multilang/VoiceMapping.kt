package capsule.multilang

/**
 * Piper TTS models + espeak voices per language code (ISO 639-1).
 *
 * Covers all [contracts.i18n.LanguageCatalog] languages (22). The 4 historical
 * mappings (fr/en/es/de) are preserved verbatim for backward compatibility;
 * the 7 first extension mappings (zh/hi/ar/bn/pt/ru/ur) and the 11 talaria.school
 * mappings (it/nl/el/tr/vi/th/id/ko/ja/sr/fa) use named Piper models + native
 * espeak codes. Piper model keys are validated against the rhasspy/piper-voices
 * registry (`voices.json`).
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
        "it" to "it_IT-paola-medium",
        "nl" to "nl_NL-pim-medium",
        "el" to "el_GR-rapunzelina-medium",
        "tr" to "tr_TR-dfki-medium",
        "vi" to "vi_VN-vais1000-medium",
        "th" to "th_TH-tsync2-medium",
        "id" to "id_ID-news_tts-medium",
        "ko" to "ko_KR-kss-medium",
        "ja" to "ja_JA-hi_fi_captain-medium",
        "sr" to "sr_RS-serbski_institut-medium",
        "fa" to "fa_IR-amir-medium",
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
        "it" to "it",
        "nl" to "nl",
        "el" to "el",
        "tr" to "tr",
        "vi" to "vi",
        "th" to "th",
        "id" to "id",
        "ko" to "ko",
        "ja" to "ja",
        "sr" to "sr",
        "fa" to "fa",
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