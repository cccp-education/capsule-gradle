package capsule.viral

import capsule.CapsuleExtension
import capsule.voice.PronunciationLexicon
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `renderViralSubtitles` (CAP-VIRAL-E2E US-3).
 *
 * Produces one karaoke caption file per campaign variant (`<variant.id>.ass`) from
 * the validated storyboard: [KaraokeWordPlanner] times the beat words (proportional
 * to their length within each beat window) and [AssKaraokeDocument] renders the
 * full ASS document (which [KaraokeSubtitleBuilder] feeds with `\k` dialogue).
 *
 * The manifest's [CampaignBundle.subtitleFile] references these files (wired by
 * `generateViralCampaign`); the delivery gate (`validateViralCampaign`) then
 * verifies they exist. Captions are pure text — no browser, no FFmpeg — so this
 * task is deterministic and cheap; an already-present file is skipped (economy of
 * ink).
 *
 * Inputs:
 *   - [storyboardFile]   the validated storyboard (beat intents = spoken text).
 *   - [languages]/[platforms]/[targetDurationSeconds] the batch matrix.
 *   - [renderedVideoDir] where `<variant.id>.ass` is written.
 *
 * Usage:
 *   ./gradlew renderViralSubtitles -Pcapsule.viral.enabled=true
 */
@DisableCachingByDefault(because = "Viral subtitles — pure text keyed on the storyboard + matrix")
abstract class RenderViralSubtitlesTask : DefaultTask() {

    /** Master switch — when false (default), the task is a no-op skip. */
    @get:Input
    abstract val viralEnabled: Property<Boolean>

    /** Target languages of the batch matrix. */
    @get:Input
    abstract val languages: ListProperty<String>

    /** Target platforms of the batch matrix (names). */
    @get:Input
    abstract val platforms: ListProperty<String>

    /** Short-form duration in [15, 60]. */
    @get:Input
    abstract val targetDurationSeconds: Property<Int>

    /** Directory (relative to the project) holding `<variant.id>.ass`. */
    @get:Input
    abstract val renderedVideoDir: Property<String>

    /** Validated storyboard AsciiDoc — the beat intents are the spoken text. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val storyboardFile: RegularFileProperty

    /**
     * Optional ASR word-timings JSON (whisper `segments[].words[]`, CAP-QC-VOICE
     * US-5). When present, the karaoke uses the measured words; otherwise it times
     * words proportionally.
     */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val wordTimingsFile: RegularFileProperty

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @TaskAction
    fun run() {
        if (!viralEnabled.get()) {
            logger.lifecycle("CAPSULE VIRAL SUBTITLES → skipped (viralEnabled=false)")
            return
        }

        val storyboard = storyboardFile.orNull?.asFile
        if (storyboard == null || !storyboard.exists()) {
            logger.warn(
                "CAPSULE VIRAL SUBTITLES → no storyboard (run `generateViralCampaign` first; " +
                    "expected ${storyboardFile.orNull?.asFile?.path}).",
            )
            return
        }
        val parsed = ViralStoryboardParser.parse(storyboard.readText())
        if (parsed == null) {
            logger.warn("CAPSULE VIRAL SUBTITLES → storyboard '${storyboard.path}' is not a validable storyboard.")
            return
        }

        val resolvedPlatforms = platforms.get()
            .mapNotNull { name -> name.takeIf { it.isNotBlank() }?.let { ViralPlatform.fromString(it) } }
        val variants = ViralBatchPlanner.plan(languages.get(), resolvedPlatforms, targetDurationSeconds.get())
        if (variants.isEmpty()) {
            logger.lifecycle("CAPSULE VIRAL SUBTITLES → skipped (empty variant matrix)")
            return
        }

        val timings = wordTimingsFile.orNull?.asFile
            ?.takeIf { it.isFile && it.length() > 0L }
            ?.let { WordTimingsJson.parse(it.readText()) }
            .orEmpty()
        // PRT-01 — dit/lu: when the voice speaks the pronunciation form, the caption
        // shows the display form (reversals map spoken words back to their symbols).
        val reversals = if (capsuleExtension.ttsPronunciationEnabled.getOrElse(false)) {
            PronunciationLexicon.reversals()
        } else {
            emptyMap()
        }
        val document = AssKaraokeDocument.build(KaraokeWordPlanner.plan(parsed.beats, timings, reversals))
        val dir = File(project.projectDir, renderedVideoDir.get())
        dir.mkdirs()

        var written = 0
        var skipped = 0
        variants.forEach { variant ->
            val target = File(dir, KaraokeSubtitleBuilder.fileNameFor(variant.id))
            if (target.exists() && target.length() > 0L) {
                skipped++
            } else {
                target.parentFile?.mkdirs()
                target.writeText(document)
                written++
            }
        }
        logger.lifecycle(
            "CAPSULE VIRAL SUBTITLES → $written written, $skipped skipped → ${dir.absolutePath}",
        )
    }
}
