package capsule.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import contracts.i18n.LanguageCatalog

/**
 * Pure domain service that validates a content plan JSON blob proposed by the
 * LLM `propose-context` node — before the `generate-speaker-notes` node runs.
 *
 * The content plan declares the deck metadata the generation node needs:
 *   - `deckName` — the deck name (without extension).
 *   - `languageCode` — ISO 639-1 code, must belong to [LanguageCatalog].
 *   - `slideCount` — expected number of slides (positive integer).
 *   - `speakerNotesFile` — output AsciiDoc speaker notes file.
 *   - `ttsScriptFile` — output TTS script file.
 *
 * Checks (in order):
 *  1. Input is non-blank.
 *  2. Input is valid JSON.
 *  3. `deckName` is present and non-blank.
 *  4. `languageCode` is present and belongs to [LanguageCatalog.supportedCodes].
 *  5. `slideCount` is present and a positive integer.
 *  6. `speakerNotesFile` is present and non-blank.
 *  7. `ttsScriptFile` is present and non-blank.
 *
 * Returns a [ContentPlanValidationResult] — [ContentPlanValidationResult.Valid]
 * when all checks pass, [ContentPlanValidationResult.Invalid] with a
 * human-readable error otherwise.
 *
 * Pure — no Gradle, no LLM, no I/O. Uses Jackson (JSON parser) and the shared
 * N0 contract [LanguageCatalog]. Pattern aligned on
 * `slider.pipeline.DeckContextValidator` (object pur, returns sealed result).
 */
object ContentPlanValidator {

    private val jsonMapper: ObjectMapper = ObjectMapper()

    /**
     * Validates the given [json] content plan blob.
     *
     * @param json the raw content plan JSON string proposed by the LLM.
     * @return [ContentPlanValidationResult.Valid] when the JSON is well-formed
     *         and complete, [ContentPlanValidationResult.Invalid] with an
     *         explanation otherwise.
     */
    fun validate(json: String): ContentPlanValidationResult {
        if (json.isBlank()) {
            return ContentPlanValidationResult.Invalid("Content plan JSON is blank")
        }

        val root: JsonNode = try {
            jsonMapper.readTree(json)
        } catch (e: Exception) {
            return ContentPlanValidationResult.Invalid("Content plan JSON is malformed: ${e.message}")
        }

        if (root.isMissingNode || !root.isObject) {
            return ContentPlanValidationResult.Invalid("Content plan JSON must be a JSON object")
        }

        root.textField("deckName")?.let { return it }

        val languageCode = root.get("languageCode")
        if (languageCode == null || languageCode.asText().isBlank()) {
            return ContentPlanValidationResult.Invalid("ContentPlan.languageCode is missing or blank")
        }
        if (languageCode.asText() !in LanguageCatalog.supportedCodes()) {
            return ContentPlanValidationResult.Invalid(
                "ContentPlan.languageCode '${languageCode.asText()}' is not a supported language code",
            )
        }

        val slideCount = root.get("slideCount")
        if (slideCount == null || !slideCount.isNumber || slideCount.asInt() <= 0) {
            return ContentPlanValidationResult.Invalid("ContentPlan.slideCount must be a positive integer")
        }

        root.textField("speakerNotesFile")?.let { return it }
        root.textField("ttsScriptFile")?.let { return it }

        return ContentPlanValidationResult.Valid(json)
    }

    /**
     * Returns an [ContentPlanValidationResult.Invalid] when the field at
     * [name] is missing, null, or blank — otherwise null.
     */
    private fun JsonNode.textField(name: String): ContentPlanValidationResult.Invalid? {
        val node = this.get(name)
        if (node == null || node.isNull) {
            return ContentPlanValidationResult.Invalid("ContentPlan.$name is missing")
        }
        if (node.asText().isBlank()) {
            return ContentPlanValidationResult.Invalid("ContentPlan.$name is blank")
        }
        return null
    }
}
