package capsule

/**
 * Configuration section for the augmented context Docs channel
 * (CAP-DOCCONTEXT-1).
 *
 * The [docsGlobs] list drives the `DocContextLoader` (US-2) to resolve
 * documentary corpus files (AsciiDoc, Markdown, text) from the consumer
 * project via Ant-style globs (e.g. `docs/afnor/**/*.adoc`). The concatenated
 * content feeds the Docs channel of `CompositeContext` (CAP-ARCH-2), giving
 * the LLM the real training material instead of an empty prompt.
 *
 * Resolution follows the 4-source precedence:
 * ENV (`CAPSULE_CONTEXT_DOCS_GLOBS`) < gradle.properties
 * (`capsule.context.docsGlobs`) < YAML (`context.docsGlobs`) < CLI
 * (`-Pcapsule.context.docsGlobs`). List values are comma-separated.
 *
 * @param docsGlobs Ant-style glob patterns to resolve documentary files.
 *        Defaults to an empty list (no Docs corpus — backward compatible).
 */
data class ContextConfig(
    val docsGlobs: List<String> = emptyList()
)