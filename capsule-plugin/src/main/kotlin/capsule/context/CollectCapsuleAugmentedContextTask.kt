package capsule.context

import contracts.context.ChannelBudget
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.context.ContextChannel
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task: `collectCapsuleAugmentedContext`
 *
 * Collects the augmented context that feeds capsule content generation
 * (CAP-ARCH-2): the EAGER governance files of the consumer project
 * (`INDEX.adoc`, `PROMPT_REPRISE.adoc`, `AGENT.adoc`) plus the optional
 * RAG / Graphify / Docs channels injected via Gradle properties. The raw
 * sections are assembled into a [contracts.context.CompositeContext], budgeted
 * and rendered through [CapsuleContextBuilder], then written to the output
 * artefact `build/capsule/augmented-context.txt`.
 *
 * The `capsule.pipeline` (CAP-ARCH-3) will consume this artefact — never the
 * raw contract. Task inputs/outputs are declared Gradle-native, so the build
 * is UP-TO-DATE when nothing changed (law of economy of ink — AGENT.adoc).
 *
 * RAG / Graphify channels are injected via `-Pcontext.*` properties
 * (mockable in tests, codebase pgvector integration is out of scope here):
 *   - `-Pcontext.ragContent=...`      RAG pgvector section
 *   - `-Pcontext.graphifyContent=...` Graphify relations section
 *   - `-Pcontext.tokenBudget=...`     total token budget (default 8000)
 *
 * Docs channel (CAP-DOCCONTEXT): two sources feed `docsSection`:
 *   - `-Pcontext.docsContent=...`    raw string (legacy, rétrocompat)
 *   - `-Pcapsule.context.docsGlobs=...`  comma-separated globs resolved by the
 *     wiring layer into [docsFiles] (CAP-DOCCONTEXT-3). Globs take precedence
 *     over the raw string when non-empty.
 */
@DisableCachingByDefault(because = "Augmented context collection — governance files, non-cacheable")
abstract class CollectCapsuleAugmentedContextTask : DefaultTask() {

    /** EAGER governance files of the consumer project (missing files skipped). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val eagerFiles: ConfigurableFileCollection

    /** Documentary corpus files resolved from `context.docsGlobs` (CAP-DOCCONTEXT-3). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsFiles: ConfigurableFileCollection

    /** Glob patterns that triggered [docsFiles] resolution (for input tracking). */
    @get:Input
    abstract val docsGlobs: ListProperty<String>

    /** RAG pgvector section content (optional). */
    @get:Input
    @get:Optional
    abstract val ragContent: Property<String>

    /** Graphify relations section content (optional). */
    @get:Input
    @get:Optional
    abstract val graphifyContent: Property<String>

    /** Codex/documentary section content (optional, legacy CLI string). */
    @get:Input
    @get:Optional
    abstract val docsContent: Property<String>

    /** Total token budget applied across the channels. */
    @get:Input
    abstract val tokenBudget: Property<Int>

    /** Rendered augmented context artefact. */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val eager = eagerFiles.files
            .sortedBy { it.name }
            .joinToString("\n\n") { file ->
                "--- ${file.name} ---\n${file.readText().trim()}"
            }

        val budget = ChannelBudget(totalTokenBudget = tokenBudget.get())

        val docsSection = resolveDocsSection(budget)

        val composite = CompositeContext(
            eagerSection = eager,
            ragSection = ragContent.orNull.orEmpty(),
            graphifySection = graphifyContent.orNull.orEmpty(),
            docsSection = docsSection,
            config = CompositeContextConfig(),
        )
        val context = CapsuleContextBuilder.build(composite, budget)

        val output = outputFile.asFile.get()
        output.parentFile.mkdirs()
        output.writeText(context.rendered)

        logger.lifecycle(
            "CAPSULE CONTEXT → ${context.nonEmptyCount} non-empty channels, " +
                "~${context.tokenEstimate} tokens → ${output.absolutePath}",
        )
        if (context.isEmpty) {
            logger.warn("CAPSULE CONTEXT → no EAGER/RAG/Graphify/Docs content collected (empty augmented context)")
        }
    }

    /**
     * Resolves the Docs section content (CAP-DOCCONTEXT-3).
     *
     * Precedence: globs (resolved into [docsFiles]) > legacy CLI string
     * ([docsContent]). When globs are configured and files are resolved,
     * [DocContextLoader] concatenates + truncates them. Otherwise the raw CLI
     * string is used as-is (backward compatible with CAP-ARCH-2).
     */
    private fun resolveDocsSection(budget: ChannelBudget): String {
        val globFiles = docsFiles.files.toList()
        if (globFiles.isNotEmpty()) {
            return DocContextLoader.load(globFiles, budget)
        }
        return docsContent.orNull.orEmpty()
    }
}
