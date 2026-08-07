package capsule.feed

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

        project.tasks.register(
            CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES,
            ExtractSpeakerNotesTask::class.java,
        ) { task ->
            task.group = CapsuleFeedTaskNames.GROUP
            task.description = CapsuleFeedTaskNames.DESCRIPTION
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
        project.tasks.register(
            CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES,
        ) { task ->
            task.group = CapsuleFeedTaskNames.GROUP
            task.description = CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_DESCRIPTION
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
        project.tasks.register(
            CapsuleFeedTaskNames.TRANSLATE_AND_GENERATE_CAPSULE_VIDEOS,
        ) { task ->
            task.group = CapsuleFeedTaskNames.GROUP
            task.description = CapsuleFeedTaskNames.TRANSLATE_AND_GENERATE_VIDEOS_DESCRIPTION
            task.dependsOn(CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES)
            task.dependsOn("generateCapsuleVideoAllLanguages")
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