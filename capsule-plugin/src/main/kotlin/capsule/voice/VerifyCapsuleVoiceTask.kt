package capsule.voice

import capsule.CapsuleExtension
import capsule.CapsuleManager
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
 * Gradle task: `verifyCapsuleVoice` (CAP-QC-VOICE US-2).
 *
 * Transcribes every `slide-NN.mp3` of the capsule through the ASR port and
 * checks it against the text that was meant to be spoken. Writes a JSON report
 * ([VoiceReportJson]) and fails the build when a take breaches a threshold.
 *
 * Behaviour:
 * - when [enabled] is false (default), the task is a no-op;
 * - when no audio take is found, it logs and skips;
 * - when the ASR binary is unavailable, it degrades (report + warning) and
 *   fails **only** under [strict];
 * - otherwise a threshold breach fails the build with the failing slides.
 *
 * The ASR is **never** a prerequisite of `check`: a missing engine degrades.
 * Test seams [recognizerOverride] / [envelopeOverride] / [scriptDirOverride]
 * let the orchestration be exercised without a real ASR or ffmpeg.
 */
@DisableCachingByDefault(because = "Runs the external ASR engine; the verdict is a function of the audio content")
abstract class VerifyCapsuleVoiceTask : DefaultTask() {

    /** Master switch — when false, the task is a no-op (opt-in, backward compat). */
    @get:Input
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /** Word error rate ceiling accepted before a take fails. */
    @get:Input
    val maxWordErrorRate: Property<Double> = project.objects.property(Double::class.java).convention(0.15)

    /** Background headroom floor (dB) before a take fails. */
    @get:Input
    val minSnrDb: Property<Double> = project.objects.property(Double::class.java).convention(66.0)

    /** Extra transcribed words signalling a hallucination. */
    @get:Input
    val surplusThreshold: Property<Int> = project.objects.property(Int::class.java).convention(3)

    /** The ASR binary (`whisper`, `faster-whisper`…). */
    @get:Input
    val asrExecutablePath: Property<String> = project.objects.property(String::class.java).convention("whisper")

    /** ASR model name (blank = CLI default). */
    @get:Input
    val asrModel: Property<String> = project.objects.property(String::class.java).convention("")

    /** ASR language hint (ISO-639-1). */
    @get:Input
    val language: Property<String> = project.objects.property(String::class.java).convention("fr")

    /** When true, an ASR-less (degraded) run fails the build too. */
    @get:Input
    val strict: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /** ffmpeg executable used to extract the RMS envelope for the SNR check. */
    @get:Input
    val ffmpegPath: Property<String> = project.objects.property(String::class.java).convention("ffmpeg")

    /** The per-slide TTS audio files (`slide-NN.mp3`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val audioFiles: ConfigurableFileCollection

    /** The JSON voice-quality report. */
    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    /** Test seam: when set, replaces the CLI ASR adapter. */
    @get:Internal
    internal var recognizerOverride: SpeechRecognizer? = null

    /** Test seam: when set, replaces the ffmpeg RMS-envelope extractor. */
    @get:Internal
    internal var envelopeOverride: ((File) -> DoubleArray?)? = null

    /** Test seam: when set, overrides the resolved script directory. */
    @get:Internal
    internal var scriptDirOverride: File? = null

    @TaskAction
    fun run() {
        if (!enabled.get()) {
            logger.lifecycle("CAPSULE VOICE → skipped (voice.verifyEnabled=false)")
            return
        }

        val takes = collectTakes()
        if (takes.isEmpty()) {
            logger.lifecycle("CAPSULE VOICE → no audio take found. Run 'generateCapsule' first.")
            return
        }

        val thresholds = VoiceThresholds(
            maxWordErrorRate = maxWordErrorRate.get(),
            minSnrDb = minSnrDb.get(),
            surplusThreshold = surplusThreshold.get(),
        )

        val override = recognizerOverride
        val asrAvailable = override != null || CliSpeechRecognizer.isAvailable(asrExecutablePath.get())
        val recognizer = override ?: CliSpeechRecognizer(asrExecutablePath.get(), asrModel.get())
        val envelope = envelopeOverride ?: { file: File -> AudioEnvelopeExtractor.extract(file, ffmpegPath.get()) }

        val report = if (asrAvailable) {
            VoiceVerifier.verify(takes, recognizer, thresholds, language.get(), envelope)
        } else {
            VoiceVerifier.degraded(takes, thresholds)
        }

        writeReport(report)

        if (report.degraded) {
            logger.warn(
                "CAPSULE VOICE → degraded: ASR engine '{}' is not available. Report: {}",
                asrExecutablePath.get(),
                reportFile.asFile.get().absolutePath,
            )
            if (strict.get()) {
                throw GradleException(
                    "Voice verification is degraded (ASR '${asrExecutablePath.get()}' unavailable) and strictMode is enabled."
                )
            }
            return
        }

        logger.lifecycle(
            "CAPSULE VOICE → {} take(s) verified, {} failure(s). Report: {}",
            report.takes.size,
            report.failures.size,
            reportFile.asFile.get().absolutePath,
        )

        if (report.hasFailure) {
            report.failures.forEach { failure ->
                val verdict = failure.verdict as VoiceVerdict.Fail
                logger.error(
                    "CAPSULE VOICE → FAIL {} slide {} [{}]: {}",
                    failure.deckName,
                    failure.slideIndex,
                    verdict.reasons.joinToString(","),
                    failure.audioFile,
                )
            }
            throw GradleException(
                "Voice verification failed: ${report.failures.size} take(s) above threshold " +
                    "(maxWordErrorRate=${thresholds.maxWordErrorRate}, minSnrDb=${thresholds.minSnrDb}, " +
                    "surplusThreshold=${thresholds.surplusThreshold})."
            )
        }
    }

    /** Resolves the takes from the capsule scripts and the produced MP3s. */
    private fun collectTakes(): List<VoiceTake> {
        val scriptDir = scriptDirOverride ?: CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleManager.readScriptFiles(scriptDir)
        if (scripts.isEmpty()) return emptyList()

        val outputRoot = project.layout.buildDirectory.dir(capsuleExtension.outputDir.get()).get().asFile
        val takes = mutableListOf<VoiceTake>()
        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)
            val deckDir = File(outputRoot, parsed.deckName)
            for (segment in parsed.segments) {
                val audio = File(deckDir, "slide-%02d.mp3".format(segment.index))
                if (audio.isFile && audio.length() > 0) {
                    takes += VoiceTake(parsed.deckName, segment.index, segment.speakerNote, audio)
                }
            }
        }
        return takes
    }

    private fun writeReport(report: VoiceReport) {
        val file = reportFile.asFile.get()
        file.parentFile?.mkdirs()
        file.writeText(VoiceReportJson.toJson(report))
    }
}
