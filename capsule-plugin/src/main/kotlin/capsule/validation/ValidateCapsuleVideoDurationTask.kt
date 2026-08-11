package capsule.validation

import capsule.MediaProbeUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `validateCapsuleVideoDuration` (CAP-CR3-1 US-2).
 *
 * Probes the final capsule video duration via `MediaProbeUtil` and
 * compares it against the sum of the per-slide TTS audio durations. A
 * delta exceeding the configured tolerance fails the build — this is
 * the *video solidity contract* (`check` green ≠ exploitable video).
 *
 * Inputs are declared for up-to-date checking (economy of ink): the
 * task is skipped if neither the video nor the audio files changed.
 *
 * Behavior:
 * - When [durationEnabled] is false (default), the task is a no-op.
 * - When the video file does not exist, the task logs a warning and
 *   skips (non-bloquant — the video may not be generated in NoOp mode).
 * - When the audio files collection is empty, the task logs a warning
 *   and skips.
 * - Otherwise, the validator is invoked. On [ValidationResult.Invalid],
 *   a `GradleException` is thrown with the actionable message.
 *
 * Usage:
 *   ./gradlew validateCapsuleVideoDuration
 *   ./gradlew validateCapsuleVideoDuration -Pcapsule.validation.durationEnabled=true
 *   ./gradlew validateCapsuleVideoDuration -Pcapsule.validation.toleranceSecs=1.5
 */
@DisableCachingByDefault(because = "Probes external ffprobe, not a pure function of inputs")
abstract class ValidateCapsuleVideoDurationTask : DefaultTask() {

    /** The final capsule video file (WebM or MP4) to probe. Internal — existence is checked at execution time. */
    @get:Internal
    abstract val videoFile: RegularFileProperty

    /** The per-slide TTS audio files (`slide-XX.mp3`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val audioFiles: ConfigurableFileCollection

    /** Tolerance threshold in seconds. A delta <= tolerance is valid. */
    @get:Input
    abstract val toleranceSecs: Property<Double>

    /** Master switch — when false, the task is a no-op. */
    @get:Input
    abstract val durationEnabled: Property<Boolean>

    @TaskAction
    fun run() {
        if (!durationEnabled.get()) {
            logger.lifecycle("CAPSULE VALIDATION → skipped (durationEnabled=false)")
            return
        }

        val video = videoFile.asFile.get()
        if (!video.exists()) {
            logger.warn("CAPSULE VALIDATION → video file not found: {}. Run 'generateCapsuleVideo' first.", video.absolutePath)
            return
        }

        val audio = audioFiles.files.toList()
        if (audio.isEmpty()) {
            logger.warn("CAPSULE VALIDATION → no audio files provided. Skipping duration validation.")
            return
        }

        val videoDuration = MediaProbeUtil.probeDuration(video)
        val audioDurations = audio.map { MediaProbeUtil.probeDuration(it) }

        val result = VideoDurationValidator.validate(
            videoDurationSecs = videoDuration,
            audioDurationsSecs = audioDurations,
            toleranceSecs = toleranceSecs.get()
        )

        when (result) {
            is ValidationResult.Valid -> logger.lifecycle(
                "CAPSULE VALIDATION → OK (video={}s, audio={}s, delta={}s, tolerance={}s)",
                String.format("%.2f", videoDuration),
                String.format("%.2f", audioDurations.sum()),
                String.format("%.2f", result.delta),
                String.format("%.2f", toleranceSecs.get())
            )
            is ValidationResult.Invalid -> {
                logger.error("CAPSULE VALIDATION → FAIL ({})", result.message())
                throw org.gradle.api.GradleException(result.message())
            }
        }
    }
}