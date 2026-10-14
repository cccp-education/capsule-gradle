package capsule.voice

import capsule.CapsuleExtension
import capsule.CapsuleFactory
import capsule.feed.CapsuleScriptReader
import capsule.feed.deckName
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `selectBestVoiceTake` (CAP-QC-VOICE US-3).
 *
 * When a slide was synthesised more than once (`slide-NN.mp3`,
 * `slide-NN-<take>.mp3`, …), transcribes every candidate and keeps the best one
 * per slide, recording the choice in `voice-selection.json` — the capsule
 * counterpart of `prepa_electricite_complet/compare_prises.py`.
 *
 * Behaviour:
 * - when [enabled] is false (default), the task is a no-op;
 * - when no slide has a candidate, it logs and skips;
 * - when the ASR is unavailable it degrades (empty selection + warning) and
 *   fails **only** under [strict];
 * - otherwise it writes the selection file. Selection is **non-destructive**:
 *   it never overwrites an audio file.
 *
 * Test seams [recognizerOverride] / [envelopeOverride] / [scriptDirOverride]
 * let the selection be exercised without a real ASR or ffmpeg.
 */
@DisableCachingByDefault(because = "Runs the external ASR engine; the verdict is a function of the audio content")
abstract class SelectBestVoiceTakeTask : DefaultTask() {

    /** Master switch — when false, the task is a no-op (opt-in, backward compat). */
    @get:Input
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Input
    val maxWordErrorRate: Property<Double> = project.objects.property(Double::class.java).convention(0.15)

    @get:Input
    val minSnrDb: Property<Double> = project.objects.property(Double::class.java).convention(66.0)

    @get:Input
    val surplusThreshold: Property<Int> = project.objects.property(Int::class.java).convention(3)

    @get:Input
    val asrExecutablePath: Property<String> = project.objects.property(String::class.java).convention("whisper")

    @get:Input
    val asrModel: Property<String> = project.objects.property(String::class.java).convention("")

    @get:Input
    val language: Property<String> = project.objects.property(String::class.java).convention("fr")

    @get:Input
    val strict: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Input
    val ffmpegPath: Property<String> = project.objects.property(String::class.java).convention("ffmpeg")

    /** Every candidate audio file (`slide-NN.mp3` and `slide-NN-<take>.mp3`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val audioFiles: ConfigurableFileCollection

    /** The JSON selection report. */
    @get:OutputFile
    abstract val selectionFile: RegularFileProperty

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @get:Internal
    internal var recognizerOverride: SpeechRecognizer? = null

    @get:Internal
    internal var envelopeOverride: ((File) -> DoubleArray?)? = null

    @get:Internal
    internal var scriptDirOverride: File? = null

    @TaskAction
    fun run() {
        if (!enabled.get()) {
            logger.lifecycle("CAPSULE VOICE SELECT → skipped (voice.selectBestTake=false)")
            return
        }

        val candidates = collectCandidateTakes()
        if (candidates.isEmpty()) {
            logger.lifecycle("CAPSULE VOICE SELECT → no candidate take found. Run 'generateCapsule' first.")
            return
        }

        val thresholds = VoiceThresholds(maxWordErrorRate.get(), minSnrDb.get(), surplusThreshold.get())
        val override = recognizerOverride
        val asrAvailable = override != null || CliSpeechRecognizer.isAvailable(asrExecutablePath.get())

        if (!asrAvailable) {
            writeSelection(emptyList())
            logger.warn(
                "CAPSULE VOICE SELECT → degraded: ASR engine '{}' is not available. Selection: {}",
                asrExecutablePath.get(),
                selectionFile.asFile.get().absolutePath,
            )
            if (strict.get()) {
                throw GradleException("Voice take selection is degraded (ASR '${asrExecutablePath.get()}' unavailable) and strictMode is enabled.")
            }
            return
        }

        val recognizer = override ?: CliSpeechRecognizer(asrExecutablePath.get(), asrModel.get())
        val envelope = envelopeOverride ?: { file: File -> AudioEnvelopeExtractor.extract(file, ffmpegPath.get()) }
        val report = VoiceVerifier.verify(candidates, recognizer, thresholds, language.get(), envelope)
        val selections = TakeSelector.selectBySlide(report.takes)
        writeSelection(selections)

        logger.lifecycle(
            "CAPSULE VOICE SELECT → {} slide(s), {} candidate(s) compared. Selection: {}",
            selections.size,
            candidates.size,
            selectionFile.asFile.get().absolutePath,
        )
    }

    /** Resolves the candidate takes (`slide-NN*.mp3`) for every slide. */
    private fun collectCandidateTakes(): List<VoiceTake> {
        val scriptDir = scriptDirOverride ?: CapsuleFactory.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleFactory.readScriptFiles(scriptDir)
        if (scripts.isEmpty()) return emptyList()

        val outputRoot = project.layout.buildDirectory.dir(capsuleExtension.outputDir.get()).get().asFile
        val takes = mutableListOf<VoiceTake>()
        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)
            val deckDir = File(outputRoot, parsed.deckName)
            for (segment in parsed.segments) {
                val prefix = "slide-%02d".format(segment.index)
                deckDir.listFiles { f -> f.isFile && f.name.endsWith(".mp3") && f.name.startsWith(prefix) }
                    ?.sortedBy { it.name }
                    ?.forEach { candidate ->
                        if (candidate.length() > 0) {
                            takes += VoiceTake(parsed.deckName, segment.index, segment.speakerNote, candidate)
                        }
                    }
            }
        }
        return takes
    }

    private fun writeSelection(selections: List<VoiceSelection>) {
        val file = selectionFile.asFile.get()
        file.parentFile?.mkdirs()
        file.writeText(VoiceSelectionJson.toJson(selections))
    }
}
