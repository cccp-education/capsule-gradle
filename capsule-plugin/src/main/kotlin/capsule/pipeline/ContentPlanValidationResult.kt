package capsule.pipeline

/**
 * Outcome of validating a content plan JSON blob proposed by the LLM.
 * Sealed hierarchy: either [Valid] (the JSON is well-formed and complete)
 * or [Invalid] with a human-readable error message.
 *
 * Pure value object — no Gradle, no LLM, no I/O. Pattern aligned on
 * `slider.pipeline.ValidationResult` (sealed for exhaustive `when` matching).
 */
sealed interface ContentPlanValidationResult {

    /**
     * The JSON is a well-formed content plan with all required fields.
     *
     * @param json the raw JSON string that was validated.
     */
    data class Valid(val json: String) : ContentPlanValidationResult {
        init {
            require(json.isNotBlank()) { "ContentPlanValidationResult.Valid.json must not be blank" }
        }
    }

    /**
     * The JSON is missing, malformed, or incomplete.
     *
     * @param error human-readable explanation of what failed.
     */
    data class Invalid(val error: String) : ContentPlanValidationResult {
        init {
            require(error.isNotBlank()) { "ContentPlanValidationResult.Invalid.error must not be blank" }
        }
    }
}
