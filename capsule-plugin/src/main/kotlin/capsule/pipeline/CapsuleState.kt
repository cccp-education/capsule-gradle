package capsule.pipeline

/**
 * Immutable state of the capsule content generation pipeline, flowing through
 * the nodes of [CapsulePipelineGraph]. Each node produces a new [CapsuleState]
 * via [copy], following the koog state pattern (data class + immutable
 * progression, pattern `slider.pipeline.DeckState` SLD-8.3).
 *
 * Pure value object — no Gradle, no LLM, no koog, no I/O. The fields model the
 * full lifecycle from the initial deck + augmented context to the enriched
 * speaker notes / TTS script (or failure).
 *
 * Invariants — cross-field consistency validated in [init]:
 * - [contextValid] `true` requires [validationError] `null` and stage >= CONTEXT_VALIDATED.
 * - [stage] CONTEXT_PROPOSED requires a non-blank [contentPlanJson].
 * - [speakerNotesAdoc] non-blank requires stage >= CONTENT_GENERATED.
 * - [error] non-null requires stage FAILED.
 *
 * @param deckName         the deck name without extension (non-blank).
 * @param language         ISO 639-1 target language code (non-blank).
 * @param sourceAdoc       the original AsciiDoc deck content to enrich (non-blank).
 * @param augmentedContext the CAP-ARCH-2 rendered augmented context (may be blank).
 * @param contentPlanJson  content plan JSON proposed by the LLM; blank until
 *                         the `propose-context` node runs.
 * @param contextValid     `true` only after [ContentPlanValidator] passes.
 * @param validationError  error message from [ContentPlanValidator], or null.
 * @param speakerNotesAdoc enriched speaker notes AsciiDoc; blank until
 *                         `generate-speaker-notes` runs.
 * @param ttsScript        TTS script text derived from [speakerNotesAdoc]; blank
 *                         until `generate-speaker-notes` runs.
 * @param error            pipeline error message; null unless [stage] is FAILED.
 * @param stage            current [CapsuleStage] — defaults to [CapsuleStage.INITIALIZED].
 */
data class CapsuleState(
    val deckName: String,
    val language: String,
    val sourceAdoc: String,
    val augmentedContext: String = "",
    val contentPlanJson: String = "",
    val contextValid: Boolean = false,
    val validationError: String? = null,
    val speakerNotesAdoc: String = "",
    val ttsScript: String = "",
    val error: String? = null,
    val stage: CapsuleStage = CapsuleStage.INITIALIZED,
) {
    init {
        require(deckName.isNotBlank()) { "CapsuleState.deckName must not be blank" }
        require(language.isNotBlank()) { "CapsuleState.language must not be blank" }
        require(sourceAdoc.isNotBlank()) { "CapsuleState.sourceAdoc must not be blank" }

        // contextValid true requires no validationError.
        require(!(contextValid && validationError != null)) {
            "CapsuleState.validationError must be null when contextValid is true"
        }

        // stage INITIALIZED cannot have contextValid true.
        require(!(contextValid && stage == CapsuleStage.INITIALIZED)) {
            "CapsuleState.contextValid cannot be true when stage is INITIALIZED"
        }

        // stage CONTEXT_PROPOSED requires a non-blank contentPlanJson.
        require(!(stage == CapsuleStage.CONTEXT_PROPOSED && contentPlanJson.isBlank())) {
            "CapsuleState.contentPlanJson must not be blank when stage is CONTEXT_PROPOSED"
        }

        // speakerNotesAdoc non-blank requires stage CONTENT_GENERATED or FAILED.
        require(!(speakerNotesAdoc.isNotBlank() &&
                stage != CapsuleStage.CONTENT_GENERATED && stage != CapsuleStage.FAILED)) {
            "CapsuleState.speakerNotesAdoc must be blank when stage is $stage"
        }

        // error non-null requires stage FAILED.
        require(!(error != null && stage != CapsuleStage.FAILED)) {
            "CapsuleState.error must be null when stage is $stage"
        }
    }
}
