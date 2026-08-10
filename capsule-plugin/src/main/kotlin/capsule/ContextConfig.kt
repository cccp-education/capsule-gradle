package capsule

/**
 * Configuration section for the augmented context channels
 * (CAP-DOCCONTEXT-1 + CAP-SPD-3).
 *
 * The [docsGlobs] list drives the `DocContextLoader` (US-2) to resolve
 * documentary corpus files (AsciiDoc, Markdown, text) from the consumer
 * project via Ant-style globs (e.g. `docs/afnor/**/*.adoc`). The concatenated
 * content feeds the Docs channel of `CompositeContext` (CAP-ARCH-2), giving
 * the LLM the real training material instead of an empty prompt.
 *
 * The [scenarioFile] path drives the `PedagogicalScenarioLoader`
 * (CAP-SPD-3) to resolve a pedagogical scenario (K-2 `metadata.json` +
 * companion AsciiDoc). The rendered section feeds the capsule-local
 * `scenarioSection` of `CapsuleContext` (CAP-SPD-2), anchoring the LLM
 * speaker notes in the session objectives, duration and prerequisites.
 *
 * Resolution follows the 4-source precedence:
 * ENV (`CAPSULE_CONTEXT_DOCS_GLOBS`, `CAPSULE_CONTEXT_SCENARIO_FILE`) <
 * gradle.properties (`capsule.context.docsGlobs`, `capsule.context.scenarioFile`)
 * < YAML (`context.docsGlobs`, `context.scenarioFile`) < CLI
 * (`-Pcapsule.context.*`). List values are comma-separated.
 *
 * @param docsGlobs    Ant-style glob patterns to resolve documentary files.
 *                    Defaults to an empty list (no Docs corpus — backward compatible).
 * @param scenarioFile path to a pedagogical scenario directory (containing
 *                    `metadata.json` + `.adoc`) or a direct `.adoc` file.
 *                    Defaults to null (no scenario — backward compatible).
 */
data class ContextConfig(
    val docsGlobs: List<String> = emptyList(),
    val scenarioFile: String? = null,
)