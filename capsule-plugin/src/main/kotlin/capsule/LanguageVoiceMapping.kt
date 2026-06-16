package capsule

enum class Language(val code: String) {
    FR("fr"),
    EN("en"),
    ES("es"),
    DE("de");

    companion object {
        fun fromCode(code: String): Language? =
            entries.find { it.code.equals(code, ignoreCase = true) }
    }
}

object VoiceMapping {
    private val piperModelByLanguage = mapOf(
        Language.FR to "fr_FR-siwis-medium",
        Language.EN to "en_US-lessac-medium",
        Language.ES to "es_ES-carlfm-x_low",
        Language.DE to "de_DE-thorsten-medium"
    )

    private val espeakVoiceByLanguage = mapOf(
        Language.FR to "fr",
        Language.EN to "en",
        Language.ES to "es",
        Language.DE to "de"
    )

    private val languageByPiperModel = piperModelByLanguage.entries.associate { (k, v) -> v to k }
    private val languageByEspeakVoice = espeakVoiceByLanguage.entries.associate { (k, v) -> v to k }

    fun piperModel(language: Language): String = piperModelByLanguage[language]!!

    fun espeakVoice(language: Language): String = espeakVoiceByLanguage[language]!!

    fun resolveLanguage(piperModel: String): Language? = languageByPiperModel[piperModel]

    fun resolveLanguageFromEspeak(espeakVoice: String): Language? = languageByEspeakVoice[espeakVoice]
}
