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

    /**
     * Pedagogical scenario file/directory (CAP-SPD-3). When present, the
     * [PedagogicalScenarioLoader] resolves `metadata.json` + companion AsciiDoc
     * and renders the scenario section appended after the N0 channels.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val scenarioFile: ConfigurableFileCollection

    /** Rendered augmented context artefact. */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Provenance artefact (CAP-PROVENANCE): `build/capsule/context-provenance.json`.
     *
     * A serialized [ContextProvenance] snapshot describing which sources fed
     * the channels that survived the token budget — the anti-hallucination
     * audit trail of the augmented context.
     */
    @get:OutputFile
    abstract val provenanceFile: RegularFileProperty

    @TaskAction
    fun run() {
        val eager = eagerFiles.files
            .filter { it.exists() }
            .sortedBy { it.name }
            .joinToString("\n\n") { file ->
                "--- ${file.name} ---\n${file.readText().trim()}"
            }

        val budget = ChannelBudget(totalTokenBudget = tokenBudget.get())

        val docsSection = resolveDocsSection(budget)

        val scenarioSection = resolveScenarioSection(budget)

        val composite = CompositeContext(
            eagerSection = eager,
            ragSection = ragContent.orNull.orEmpty(),
            graphifySection = graphifyContent.orNull.orEmpty(),
            docsSection = docsSection,
            config = CompositeContextConfig(),
        )
        val tracker = ProvenanceTracker()
        trackSources(tracker)
        val context = CapsuleContextBuilder.build(composite, budget, scenarioSection, tracker)

        val output = outputFile.asFile.get()
        output.parentFile.mkdirs()
        output.writeText(context.rendered)

        val provenance = tracker.build()
        val provenanceOut = provenanceFile.asFile.get()
        provenanceOut.parentFile.mkdirs()
        provenanceOut.writeText(provenance.toJson())

        logger.lifecycle(
            "CAPSULE CONTEXT → ${context.nonEmptyCount} non-empty channels, " +
                "~${context.tokenEstimate} tokens → ${output.absolutePath}",
        )
        logger.lifecycle(
            "CAPSULE PROVENANCE → ${provenance.channels.size} channels, " +
                "${provenance.channels.sumOf { it.sources.size }} sources → ${provenanceOut.absolutePath}",
        )
        if (context.isEmpty && context.scenarioSection.isBlank()) {
            logger.warn("CAPSULE CONTEXT → no EAGER/RAG/Graphify/Docs/scenario content collected (empty augmented context)")
        }
    }

    /**
     * Tracks the per-channel sources into [tracker] (CAP-PROVENANCE US-2).
     *
     * The builder prunes the tracker to the channels that survived the token
     * budget via [ProvenanceTracker.retainOnly] — a channel truncated to zero
     * content is dropped from the provenance. Source measurement: raw chars
     * via `readText().length` and token estimate via the N0
     * [ContextChannel.estimateTokens] heuristic.
     */
    private fun trackSources(tracker: ProvenanceTracker) {
        val eagerSources = eagerFiles.files.filter { it.exists() }.sortedBy { it.name }.map { file ->
            ProvenanceSource(
                fileName = file.name,
                chars = file.readText().length,
                tokens = ContextChannel.estimateTokens(file.readText()),
            )
        }
        tracker.trackChannel("EAGER", eagerSources)

        val rag = ragContent.orNull.orEmpty()
        if (rag.isNotBlank()) {
            tracker.trackChannel(
                "RAG",
                listOf(
                    ProvenanceSource(
                        fileName = "rag-injected",
                        chars = rag.length,
                        tokens = ContextChannel.estimateTokens(rag),
                    ),
                ),
            )
        }

        val graphify = graphifyContent.orNull.orEmpty()
        if (graphify.isNotBlank()) {
            tracker.trackChannel(
                "GRAPHIFY",
                listOf(
                    ProvenanceSource(
                        fileName = "graphify-injected",
                        chars = graphify.length,
                        tokens = ContextChannel.estimateTokens(graphify),
                    ),
                ),
            )
        }

        val docsFilesResolved = docsFiles.files.filter { it.exists() }.sortedBy { it.name }
        val docsSources = if (docsFilesResolved.isNotEmpty()) {
            docsFilesResolved.map { file ->
                ProvenanceSource(
                    fileName = file.name,
                    chars = file.readText().length,
                    tokens = ContextChannel.estimateTokens(file.readText()),
                )
            }
        } else {
            val legacy = docsContent.orNull.orEmpty()
            if (legacy.isBlank()) emptyList()
            else listOf(
                ProvenanceSource(
                    fileName = "docs-injected",
                    chars = legacy.length,
                    tokens = ContextChannel.estimateTokens(legacy),
                ),
            )
        }
        tracker.trackChannel("DOCS", docsSources)

        val scenarioTarget = scenarioFile.files.firstOrNull()
        if (scenarioTarget != null && scenarioTarget.exists()) {
            val resolved = if (scenarioTarget.isDirectory) {
                scenarioTarget.listFiles()?.firstOrNull { it.extension.equals("adoc", ignoreCase = true) }
            } else {
                scenarioTarget
            }
            if (resolved != null && resolved.exists()) {
                tracker.trackChannel(
                    ContextProvenance.SCENARIO_CHANNEL,
                    listOf(
                        ProvenanceSource(
                            fileName = resolved.name,
                            chars = resolved.readText().length,
                            tokens = ContextChannel.estimateTokens(resolved.readText()),
                        ),
                    ),
                )
            }
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

    /**
     * Resolves the pedagogical scenario section content (CAP-SPD-3).
     *
     * The [scenarioFile] collection is fed by the wiring layer from the
     * 4-source config (ENV < props < YAML < CLI). When it contains a
     * directory, the [PedagogicalScenarioLoader] resolves `metadata.json` +
     * the first `.adoc` companion. When it contains a direct `.adoc` file,
     * the metadata is skipped. When empty, the scenario section is blank
     * (backward compatible no-op).
     *
     * Token budget: 5% of the total budget (the scenario is a lightweight
     * anchoring payload, not a corpus).
     */
    private fun resolveScenarioSection(budget: ChannelBudget): String {
        val files = scenarioFile.files.toList()
        if (files.isEmpty()) return ""
        val target = files.first()
        if (!target.exists()) return ""

        val scenarioBudget = (budget.totalTokenBudget * 0.05).toInt().coerceAtLeast(50)
        return if (target.isDirectory) {
            val metadata = target.listFiles()?.firstOrNull { it.name == "metadata.json" }
            val adoc = target.listFiles()?.firstOrNull { it.extension.equals("adoc", ignoreCase = true) }
            if (adoc == null) return ""
            PedagogicalScenarioLoader.load(metadata, adoc, scenarioBudget)
        } else {
            PedagogicalScenarioLoader.load(null, target, scenarioBudget)
        }
    }
}
