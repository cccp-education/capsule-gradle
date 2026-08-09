package capsule.pipeline

import capsule.ai.CapsuleLlmService.aiProvider
import capsule.ai.CapsuleLlmService.resolveModel
import codebase.koog.llm.service.LlmBuildService
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task: `generateCapsuleContent`
 *
 * Orchestrates the koog [CapsulePipelineGraph] (propose-context →
 * validate-context → generate-speaker-notes) in a single invocation — the
 * CAP-ARCH-3 pipeline entry point. It enriches the speaker notes of a deck
 * (with the CAP-ARCH-2 augmented context) and derives the TTS script.
 *
 * Inputs:
 *   - [deckFile]             source AsciiDoc deck (`-Pdeck.file`, or the first
 *                            `.adoc` in `slides/misc`)
 *   - [augmentedContextFile] `build/capsule/augmented-context.txt` produced by
 *                            `collectCapsuleAugmentedContext` (optional)
 *   - [language]             ISO 639-1 code, default `fr`
 *   - [llmService]           the codebase [LlmBuildService] bridge (Gradle DI)
 *
 * Outputs (declared → economy of ink via UP-TO-DATE):
 *   - [speakerNotesOutput] `build/capsule/<deckName>-speaker-notes.adoc`
 *   - [ttsScriptOutput]    `build/capsule/<deckName>-script.txt`
 *
 * Mock-LLM test path: `-Pollama.baseUrl=<url>` (see [CapsuleLlmService]).
 *
 * Usage:
 *   ./gradlew generateCapsuleContent -Pdeck.file=slides/misc/my-deck.adoc
 */
@DisableCachingByDefault(because = "LLM content generation")
abstract class GenerateCapsuleContentTask : DefaultTask() {

    /** Source AsciiDoc deck to enrich. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val deckFile: RegularFileProperty

    /** Rendered augmented context artefact from `collectCapsuleAugmentedContext`. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val augmentedContextFile: RegularFileProperty

    /** ISO 639-1 target language code. */
    @get:Input
    abstract val language: Property<String>

    /** Enriched speaker notes AsciiDoc output. */
    @get:OutputFile
    abstract val speakerNotesOutput: RegularFileProperty

    /** Derived TTS script output. */
    @get:OutputFile
    abstract val ttsScriptOutput: RegularFileProperty

    @get:ServiceReference
    abstract val llmService: Property<LlmBuildService>

    @TaskAction
    fun run() {
        val deck = deckFile.get().asFile
        check(deck.exists()) { "Deck file does not exist: ${deck.path} (set -Pdeck.file=<path> or add a .adoc in slides/misc)" }
        val deckName = deck.nameWithoutExtension
        val sourceAdoc = deck.readText()
        val augmentedContext = when {
            !augmentedContextFile.isPresent -> ""
            augmentedContextFile.get().asFile.exists() -> augmentedContextFile.get().asFile.readText()
            else -> error(
                "Augmented context file does not exist: ${augmentedContextFile.get().asFile.path}. " +
                    "Run collectCapsuleAugmentedContext first or unset the property.",
            )
        }

        val provider = project.aiProvider
        val model = project.resolveModel(provider, llmService)
        val graph = CapsulePipelineGraph(
            promptBuilder = DefaultCapsulePromptBuilder(),
            llm = ChatModelCapsuleLlm(model),
        )

        val initial = CapsuleState(
            deckName = deckName,
            language = language.get(),
            sourceAdoc = sourceAdoc,
            augmentedContext = augmentedContext,
            contentPlanJson = "",
        )

        val result = graph.execute(initial)
        if (result.stage == CapsuleStage.FAILED) {
            error("generateCapsuleContent failed at stage ${result.stage}: ${result.error}")
        }

        val speakerNotes = speakerNotesOutput.get().asFile
        speakerNotes.parentFile.mkdirs()
        speakerNotes.writeText(result.speakerNotesAdoc)

        val ttsScript = ttsScriptOutput.get().asFile
        ttsScript.parentFile.mkdirs()
        ttsScript.writeText(result.ttsScript)

        logger.lifecycle(
            "CAPSULE PIPELINE → ${result.deckName} speaker notes (${result.speakerNotesAdoc.length} chars) + " +
                "TTS script (${result.ttsScript.length} chars)",
        )
    }
}
