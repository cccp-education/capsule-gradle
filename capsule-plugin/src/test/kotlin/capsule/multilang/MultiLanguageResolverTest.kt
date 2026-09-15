package capsule.multilang

import contracts.i18n.SupportedLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class MultiLanguageResolverTest {

    @Test
    fun `resolve returns non-null for all supported language codes`() {
        val codes = contracts.i18n.LanguageCatalog.supportedCodes()
        codes.forEach { code ->
            assertNotNull(MultiLanguageResolver.resolve(code), "Expected non-null resolution for code: $code")
        }
    }

    @Test
    fun `resolve returns null for unknown code`() {
        assertNull(MultiLanguageResolver.resolve("xx"))
        assertNull(MultiLanguageResolver.resolve("unknown"))
    }

    @Test
    fun `resolve returns null for empty code`() {
        assertNull(MultiLanguageResolver.resolve(""))
    }

    @Test
    fun `resolve returns ResolvedLanguage with correct SupportedLanguage code for fr`() {
        val resolved = MultiLanguageResolver.resolve("fr")
        assertNotNull(resolved)
        assertEquals("fr", resolved!!.language.code)
    }

    @Test
    fun `resolve returns ResolvedLanguage with correct SupportedLanguage code for en`() {
        val resolved = MultiLanguageResolver.resolve("en")
        assertNotNull(resolved)
        assertEquals("en", resolved!!.language.code)
    }

    @Test
    fun `resolve returns correct Piper model for fr`() {
        val resolved = MultiLanguageResolver.resolve("fr")
        assertNotNull(resolved)
        assertEquals("fr_FR-siwis-medium", resolved!!.piperModel)
    }

    @Test
    fun `resolve returns correct Piper model for en`() {
        val resolved = MultiLanguageResolver.resolve("en")
        assertNotNull(resolved)
        assertEquals("en_US-lessac-medium", resolved!!.piperModel)
    }

    @Test
    fun `resolve returns correct Piper model for zh`() {
        val resolved = MultiLanguageResolver.resolve("zh")
        assertNotNull(resolved)
        assertEquals("zh_CN-huayan-medium", resolved!!.piperModel)
    }

    @Test
    fun `resolve returns correct espeak voice for fr`() {
        val resolved = MultiLanguageResolver.resolve("fr")
        assertNotNull(resolved)
        assertEquals("fr", resolved!!.espeakVoice)
    }

    @Test
    fun `resolve returns correct espeak voice for zh`() {
        val resolved = MultiLanguageResolver.resolve("zh")
        assertNotNull(resolved)
        assertEquals("zh", resolved!!.espeakVoice)
    }

    @Test
    fun `resolve preserves RTL flag for Arabic`() {
        val resolved = MultiLanguageResolver.resolve("ar")
        assertNotNull(resolved)
        assertTrue(resolved!!.language.rtl, "Arabic should be RTL")
    }

    @Test
    fun `resolve preserves RTL flag for Urdu`() {
        val resolved = MultiLanguageResolver.resolve("ur")
        assertNotNull(resolved)
        assertTrue(resolved!!.language.rtl, "Urdu should be RTL")
    }

    @Test
    fun `resolve preserves LTR flag for French`() {
        val resolved = MultiLanguageResolver.resolve("fr")
        assertNotNull(resolved)
        assertFalse(resolved!!.language.rtl, "French should be LTR")
    }

    @Test
    fun `resolveByPiperModel returns SupportedLanguage for known model`() {
        val lang = MultiLanguageResolver.resolveByPiperModel("fr_FR-siwis-medium")
        assertNotNull(lang)
        assertEquals("fr", lang!!.code)
    }

    @Test
    fun `resolveByPiperModel returns null for unknown model`() {
        assertNull(MultiLanguageResolver.resolveByPiperModel("unknown-model"))
        assertNull(MultiLanguageResolver.resolveByPiperModel(""))
    }

    @Test
    fun `resolveByEspeakVoice returns SupportedLanguage for known voice`() {
        val lang = MultiLanguageResolver.resolveByEspeakVoice("fr")
        assertNotNull(lang)
        assertEquals("fr", lang!!.code)
    }

    @Test
    fun `resolveByEspeakVoice returns null for unknown voice`() {
        assertNull(MultiLanguageResolver.resolveByEspeakVoice("xx"))
        assertNull(MultiLanguageResolver.resolveByEspeakVoice(""))
    }

    @Test
    fun `piperModel returns model string for all supported codes`() {
        val expected = mapOf(
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
        expected.forEach { (code, model) ->
            assertEquals(model, MultiLanguageResolver.piperModel(code), "Wrong Piper model for code: $code")
        }
    }

    @Test
    fun `espeakVoice returns voice string for all supported codes`() {
        val codes = contracts.i18n.LanguageCatalog.supportedCodes()
        codes.forEach { code ->
            assertEquals(code, MultiLanguageResolver.espeakVoice(code), "Wrong espeak voice for code: $code")
        }
    }

    @Test
    fun `piperModel returns null for unsupported code`() {
        assertNull(MultiLanguageResolver.piperModel("xx"))
        assertNull(MultiLanguageResolver.piperModel(""))
    }

    @Test
    fun `espeakVoice returns null for unsupported code`() {
        assertNull(MultiLanguageResolver.espeakVoice("xx"))
        assertNull(MultiLanguageResolver.espeakVoice(""))
    }

    @Test
    fun `supportedCodes covers all LanguageCatalog codes`() {
        val codes = MultiLanguageResolver.supportedCodes
        assertEquals(22, codes.size)
        assertEquals(contracts.i18n.LanguageCatalog.supportedCodes(), codes)
    }
}