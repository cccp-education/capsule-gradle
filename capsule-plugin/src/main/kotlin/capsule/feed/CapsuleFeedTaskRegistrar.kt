package capsule.feed

import capsule.i18n.CapsuleMessages
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Thin Gradle adapter that registers the `extractSpeakerNotes` task.
 *
 * This task replaces the slider-side `generateCapsule` task previously owned
 * by `slider.capsule.CapsuleTaskRegistrar`. Slider now stops at producing the
 * HTML Reveal.js deck; capsule parses the AsciiDoc source directly and emits
 * the `*-script.txt` contract consumed by [CapsuleScriptReader] (video pipeline).
 *
 * Behavioural contract preserved bit-for-bit from the slider-side original:
 * - task name `extractSpeakerNotes`, group `capsule`
 * - `outputs.upToDateWhen { false }` so the task always re-runs
 * - for each `.adoc` file in `<projectDir>/slides/misc`:
 *   - parse speaker notes via [AsciidocSpeakerNoteParser.parse]
 *   - when the script is empty (no speaker notes), log a skip warning and
 *     continue to the next deck
 *   - otherwise write `<buildDir>/capsule/<deckName>-script.txt` via
 *     [CapsuleScriptWriter.write] and log the slide count
 */
object CapsuleFeedTaskRegistrar {

    /**
     * Registers the `extractSpeakerNotes` task on [project].
     */
    fun register(project: Project) {
        val adocDir = CapsuleAdocDir(project.projectDir)
        val scriptDir = CapsuleScriptDir(project.layout.buildDirectory.get().asFile)
        val lang = CapsuleMessages.resolveLanguage(project)

        project.tasks.register(
            CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES,
            ExtractSpeakerNotesTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.capsule", lang)
            task.description = CapsuleMessages.get("task.extractSpeakerNotes.description", lang)
            task.outputs.upToDateWhen { false }
            task.adocDir = adocDir
            task.scriptDir = scriptDir
        }
    }

    /**
     * Registers the `translateAndExtractSpeakerNotes` composite task on [project].
     *
     * This task depends on both `translateDeck` (slider-gradle) and
     * `extractSpeakerNotes` (capsule-gradle), wiring the translation pipeline
     * to the capsule feed pipeline so that a single Gradle invocation produces
     * capsule scripts for all translated decks. Replaces the slider-side
     * `translateAndGenerateCapsule` composite task.
     */
    fun registerTranslateAndExtractSpeakerNotes(project: Project) {
        val lang = CapsuleMessages.resolveLanguage(project)
        project.tasks.register(
            CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.capsule", lang)
            task.description = CapsuleMessages.get("task.translateAndExtractSpeakerNotes.description", lang)
            task.dependsOn(CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES)
            task.dependsOn("translateDeck")
        }
    }

    /**
     * Registers the `translateAndGenerateCapsuleVideos` composite task on [project].
     *
     * This task wires the full multi-language video pipeline: it depends on
     * `translateAndExtractSpeakerNotes` (slider `translateDeck` → capsule
     * `extractSpeakerNotes`) and on `generateCapsuleVideoAllLanguages`, so a
     * single Gradle invocation produces one localized capsule WebM per target
     * language from a single source deck.
     */
    fun registerTranslateAndGenerateCapsuleVideos(project: Project) {
        val lang = CapsuleMessages.resolveLanguage(project)
        project.tasks.register(
            CapsuleFeedTaskNames.TRANSLATE_AND_GENERATE_CAPSULE_VIDEOS,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.capsule", lang)
            task.description = CapsuleMessages.get("task.translateAndGenerateCapsuleVideos.description", lang)
            task.dependsOn(CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES)
            task.dependsOn("generateCapsuleVideoAllLanguages")
        }
    }

    /**
     * Registers the `generateCapsuleContentAndVideos` composite task on [project]
     * (CAP-ARCH-4).
     *
     * This task wires the LLM content generation pipeline (`generateCapsuleContent`)
     * to the existing multi-language video pipeline (`translateAndGenerateCapsuleVideos`)
     * without modifying either backend. A single Gradle invocation produces
     * enriched speaker notes then localized capsule WebMs.
     */
    fun registerGenerateCapsuleContentAndVideos(project: Project) {
        val lang = CapsuleMessages.resolveLanguage(project)
        project.tasks.register(
            CapsuleFeedTaskNames.GENERATE_CAPSULE_CONTENT_AND_VIDEOS,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.capsule", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleContentAndVideos.description", lang)
            task.dependsOn("generateCapsuleContent")
            task.dependsOn(CapsuleFeedTaskNames.TRANSLATE_AND_GENERATE_CAPSULE_VIDEOS)
        }
    }
}

/**
 * Gradle task that extracts speaker notes from AsciiDoc decks.
 *
 * Inputs and outputs are wired as properties by the registrar so the task
 * remains a thin Gradle adapter over the pure `capsule.feed` domain.
 */
@DisableCachingByDefault(because = "Filesystem-bound: reads AsciiDoc decks and produces *-script.txt artifacts")
open class ExtractSpeakerNotesTask : DefaultTask() {

    @get:org.gradle.api.tasks.Internal
    var adocDir: CapsuleAdocDir? = null
        internal set

    @get:org.gradle.api.tasks.Internal
    var scriptDir: CapsuleScriptDir? = null
        internal set

    @TaskAction
    fun execute() {
        val adoc = adocDir ?: error("adocDir must be wired by CapsuleFeedTaskRegistrar")
        val scriptOut = scriptDir ?: error("scriptDir must be wired by CapsuleFeedTaskRegistrar")
        adoc.adocFiles().forEach { adocFile ->
            val scriptModel = AsciidocSpeakerNoteParser.parse(
                adocContent = adocFile.readText(),
                deckName = adocFile.nameWithoutExtension,
            )
            if (scriptModel.isEmpty) {
                logger.warn("No speaker notes found in {}, skipping.", adocFile.name)
                return@forEach
            }
            val scriptFile = scriptOut.scriptFileFor(adocFile.nameWithoutExtension)
            scriptFile.writeText(CapsuleScriptWriter.write(scriptModel))
            logger.lifecycle(
                "Capsule script '{}' written ({} slides) -> {}",
                scriptFile.name,
                scriptModel.segments.size,
                scriptFile.absolutePath,
            )
        }
    }
}