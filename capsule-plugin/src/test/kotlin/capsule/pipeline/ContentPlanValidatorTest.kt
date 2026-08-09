package capsule.pipeline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [ContentPlanValidator] — the pure validation service of the
 * CAP-ARCH-3 content plan JSON.
 *
 * Baby-step TDD (RED → GREEN): pattern `slider.pipeline.DeckContextValidatorTest`.
 * No Gradle, no LLM, no I/O — only Jackson + the N0 `LanguageCatalog`.
 */
class ContentPlanValidatorTest {

    private val validPlan = """
        {
          "deckName": "kotlin-coroutines",
          "languageCode": "fr",
          "slideCount": 24,
          "speakerNotesFile": "kotlin-coroutines-speaker-notes.adoc",
          "ttsScriptFile": "kotlin-coroutines-script.txt"
        }
    """.trimIndent()

    @Test
    fun `valid content plan passes`() {
        val result = ContentPlanValidator.validate(validPlan)
        assertIs<ContentPlanValidationResult.Valid>(result)
        assertEquals(validPlan, result.json)
    }

    @Test
    fun `blank input fails`() {
        val result = ContentPlanValidator.validate("   ")
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("blank"))
    }

    @Test
    fun `malformed JSON fails`() {
        val result = ContentPlanValidator.validate("""{"deckName": broken""")
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("malformed"))
    }

    @Test
    fun `non-object JSON fails`() {
        val result = ContentPlanValidator.validate("""["deckName"]""")
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("object"))
    }

    @Test
    fun `missing deckName fails`() {
        val json = validPlan.replace(""""deckName": "kotlin-coroutines",""", "")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("deckName"))
    }

    @Test
    fun `blank deckName fails`() {
        val json = validPlan.replace("kotlin-coroutines", "   ")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("deckName"))
    }

    @Test
    fun `missing languageCode fails`() {
        val json = validPlan.replace(""""languageCode": "fr",""", "")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("languageCode"))
    }

    @Test
    fun `unsupported languageCode fails`() {
        val json = validPlan.replace("fr", "xx")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("not a supported language code"))
    }

    @Test
    fun `missing slideCount fails`() {
        val json = validPlan.replace(""""slideCount": 24,""", "")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("slideCount"))
    }

    @Test
    fun `non-positive slideCount fails`() {
        val json = validPlan.replace("24", "0")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("positive integer"))
    }

    @Test
    fun `missing speakerNotesFile fails`() {
        val json = validPlan.replace(""""speakerNotesFile": "kotlin-coroutines-speaker-notes.adoc",""", "")
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("speakerNotesFile"))
    }

    @Test
    fun `missing ttsScriptFile fails`() {
        val json = """
            {
              "deckName": "kotlin-coroutines",
              "languageCode": "fr",
              "slideCount": 24,
              "speakerNotesFile": "kotlin-coroutines-speaker-notes.adoc"
            }
        """.trimIndent()
        val result = ContentPlanValidator.validate(json)
        assertIs<ContentPlanValidationResult.Invalid>(result)
        assertTrue(result.error.contains("ttsScriptFile"))
    }
}
