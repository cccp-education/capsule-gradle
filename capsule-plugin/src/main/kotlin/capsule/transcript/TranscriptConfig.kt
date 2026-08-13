package capsule.transcript

/**
 * Configuration section for the capsule transcript feature
 * (CAP-TRANSCRIPT US-1).
 *
 * When [enabled] is true, the `generateCapsuleTranscript` task
 * produces an AsciiDoc article from the capsule speaker notes + slide
 * titles. The [strategy] selects the generation mode: `TEMPLATE`
 * (deterministic assembly, no LLM) or `LLM` (enrichment via
 * `TranscriptLlmEnhancer`, requires `LlmBuildService`).
 *
 * All fields default to disabled/`TEMPLATE` to preserve backward
 * compatibility — existing configs without a `transcript` section
 * keep the no-transcript behavior.
 *
 * Resolution follows the 4-source precedence:
 * ENV (`CAPSULE_TRANSCRIPT_ENABLED`, `CAPSULE_TRANSCRIPT_STRATEGY`)
 * < gradle.properties (`capsule.transcript.enabled`,
 * `capsule.transcript.strategy`) < YAML (`transcript.enabled`,
 * `transcript.strategy`) < CLI (`-Pcapsule.transcript.*`).
 *
 * @param enabled  `true` to enable transcript generation
 *        (default `false` — backward compat, opt-in).
 * @param strategy the generation strategy (default [TranscriptStrategy.TEMPLATE]
 *        — deterministic, no LLM required).
 */
data class TranscriptConfig(
    val enabled: Boolean = false,
    val strategy: TranscriptStrategy = TranscriptStrategy.TEMPLATE
)

/**
 * Transcript generation strategy (CAP-TRANSCRIPT US-1).
 *
 * - [TEMPLATE] — assemble the AsciiDoc article directly from the
 *   capsule segments (slide titles + speaker notes). Deterministic,
 *   zero LLM, no `LlmBuildService` required. This is the default
 *   (backward compat — existing configs without a `transcript.strategy`
 *   keep the template behavior).
 * - [LLM] — enrich the template article via `TranscriptLlmEnhancer`
 *   (pedagogical transitions, engaging introductions, reformulations).
 *   Requires `LlmBuildService` (codebase LLM hub). Falls back to the
 *   template if the LLM returns blank/invalide (degraded mode).
 */
enum class TranscriptStrategy {
    TEMPLATE,
    LLM;

    companion object {
        /**
         * Case-insensitive parse. Falls back to [TEMPLATE] for
         * null/blank/unknown values (backward compat — existing
         * configs without `transcript.strategy` keep the template
         * behavior).
         */
        fun fromString(value: String?): TranscriptStrategy =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEMPLATE
    }
}