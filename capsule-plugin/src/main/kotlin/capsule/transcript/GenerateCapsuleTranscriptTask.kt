package capsule.transcript

import capsule.ai.CapsuleLlmService.aiProvider
import capsule.ai.CapsuleLlmService.resolveModel
import capsule.feed.AsciidocSpeakerNoteParser
import codebase.koog.llm.service.LlmBuildService
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task: `generateCapsuleTranscript`
 *
 * Produces an AsciiDoc transcript article from the capsule speaker notes
 * (CAP-TRANSCRIPT US-4). The article is publishable by document-gradle
 * (WRITE + PUBLISH multi-format) or bakery-gradle (static site), doubling
 * the MVP0 deliverable: capsule video + revision article.
 *
 * Inputs:
 *   - [speakerNotesFile] the `build/capsule/<deckName>-speaker-notes.adoc`
 *     produced by `generateCapsuleContent` (dependsOn)
 *   - [language]        ISO 639-1 code, default `fr`
 *   - [strategy]        `TEMPLATE` (deterministic, no LLM) or `LLM`
 *                        (enrichment via [TranscriptLlmEnhancer])
 *   - [llmService]      the codebase [LlmBuildService] bridge (Gradle DI,
 *                        optional — only required when strategy is `LLM`)
 *
 * Output:
 *   - [transcriptOutput] `build/capsule/<deckName>-transcript.adoc`
 *
 * Economy of ink: the task is skipped (no-op) when [transcriptOutput] already
 * exists and is non-blank — a valid transcript is never overwritten by a
 * re-computation of the same input.
 *
 * Usage:
 *   ./gradlew generateCapsuleTranscript -Pcapsule.transcript.enabled=true \
 *     -Pcapsule.transcript.strategy=template
 */
@DisableCachingByDefault(because = "Transcript generation (LLM enrichment)")
abstract class GenerateCapsuleTranscriptTask : DefaultTask() {

    /** Source speaker notes AsciiDoc produced by `generateCapsuleContent`. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val speakerNotesFile: RegularFileProperty

    /** ISO 639-1 target language code. */
    @get:Input
    abstract val language: Property<String>

    /** Generation strategy: `TEMPLATE` (deterministic) or `LLM` (enrichment). */
    @get:Input
    abstract val strategy: Property<TranscriptStrategy>

    /** Transcript AsciiDoc article output. */
    @get:OutputFile
    abstract val transcriptOutput: RegularFileProperty

    @get:ServiceReference
    @get:Optional
    abstract val llmService: Property<LlmBuildService>

    @TaskAction
    fun run() {
        val output = transcriptOutput.get().asFile

        // Economy of ink — skip if the transcript already exists and is valid.
        if (output.exists() && output.readText().isNotBlank()) {
            logger.lifecycle("CAPSULE TRANSCRIPT → ${output.name} already exists, skipping (economy of ink)")
            return
        }

        val notesFile = speakerNotesFile.get().asFile
        check(notesFile.exists()) {
            "Speaker notes file does not exist: ${notesFile.path}. Run generateCapsuleContent first."
        }
        val deckName = notesFile.nameWithoutExtension.removeSuffix("-speaker-notes")
        val notesContent = notesFile.readText()

        val script = AsciidocSpeakerNoteParser.parse(notesContent, deckName)
        val plan = TranscriptPlan(
            deckName = deckName,
            segments = script.segments,
            language = language.get(),
            outputPath = output.absolutePath,
        )

        val template = TranscriptBuilder.build(plan)

        val article = when (strategy.get()) {
            TranscriptStrategy.TEMPLATE -> template
            TranscriptStrategy.LLM -> {
                val provider = project.aiProvider
                val model = project.resolveModel(provider, llmService)
                val enhancer = ChatModelTranscriptEnhancer(model)
                enhancer.enhance(template, plan)
            }
        }

        output.parentFile.mkdirs()
        output.writeText(article)

        logger.lifecycle(
            "CAPSULE TRANSCRIPT → ${output.name} (${article.length} chars, strategy=${strategy.get()})"
        )
    }
}