package capsule.viral

import capsule.CaptureResolver
import capsule.CaptureStrategy
import capsule.CapsuleManager
import capsule.MediaProbeUtil
import capsule.NoOpPlaywrightCapture
import capsule.PlaywrightCapture
import capsule.ScreenshotCaptureImpl
import capsule.VideoFormatConverter
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
 * Gradle task: `renderViralVariants` (CAP-SHORT US-1b).
 *
 * Closes the domain → deliverable gap: it iterates the campaign's [ViralVariant]s
 * (language × platform) and renders one short-form MP4 per variant by capturing
 * the **deck** produced by US-1a (`viral-deck.html`) through the deterministic
 * FFmpeg screenchot socle ([ScreenshotCaptureImpl] — Playwright only takes static
 * PNGs; FFmpeg animates and concatenates), then transcoding to MP4.
 *
 * All decisions live in the pure [ViralVariantRenderer]; this task is a thin
 * adapter. Economy of ink: a variant whose `<id>.mp4` already exists (non-empty)
 * is skipped.
 *
 * Inputs:
 *   - [deckFile]      the capturable portrait deck (US-1a, `viral-deck.html`).
 *   - [storyboardFile] the validated storyboard (beat durations drive the capture).
 *   - [languages]/[platforms]/[targetDurationSeconds] the batch matrix.
 *   - [renderedVideoDir] where `<variant.id>.mp4` is written.
 *
 * Usage:
 *   ./gradlew renderViralVariants -Pcapsule.viral.enabled=true \
 *     -Pcapsule.viral.storyboardFile=storyboard.adoc
 */
@DisableCachingByDefault(because = "Viral render — Playwright + FFmpeg capture, may call the LLM upstream")
abstract class RenderViralVariantsTask : DefaultTask() {

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

    /** Directory (relative to the project) holding `<variant.id>.mp4`. */
    @get:Input
    abstract val renderedVideoDir: Property<String>

    /** FFmpeg executable path (or `noop` for the degraded test path). */
    @get:Input
    abstract val ffmpegPath: Property<String>

    /** When true, an unavailable engine / a failed variant fails the build. */
    @get:Input
    abstract val strict: Property<Boolean>

    /** Capture timeout (milliseconds). */
    @get:Input
    abstract val captureTimeoutMs: Property<Double>

    /** Tolerance (seconds) of the produced duration against the target short-form duration. */
    @get:Input
    abstract val toleranceSecs: Property<Double>

    /** Validated storyboard AsciiDoc — the beat durations drive the capture. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val storyboardFile: RegularFileProperty

    /** Capturable portrait deck (US-1a). */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val deckFile: RegularFileProperty

    /** Test seam — the capture engine (defaults to the FFmpeg screenshot socle). */
    @get:Internal
    internal var captureFactory: (() -> PlaywrightCapture)? = null

    /** Test seam — the format converter (defaults to FFmpeg/NoOp resolution). */
    @get:Internal
    internal var formatConverter: VideoFormatConverter? = null

    @TaskAction
    fun run() {
        if (!viralEnabled.get()) {
            logger.lifecycle("CAPSULE VIRAL RENDER → skipped (viralEnabled=false)")
            return
        }

        val deck = deckFile.orNull?.asFile
        if (deck == null || !deck.exists()) {
            error(
                "CAPSULE VIRAL RENDER → no capturable deck (run `generateViralCampaign` first; " +
                    "expected ${deckFile.orNull?.asFile?.path}).",
            )
        }
        val storyboard = storyboardFile.orNull?.asFile
        if (storyboard == null || !storyboard.exists()) {
            error("CAPSULE VIRAL RENDER → no storyboard (set -Pcapsule.viral.storyboardFile=<path>).")
        }
        val parsed = ViralStoryboardParser.parse(storyboard.readText())
            ?: error("CAPSULE VIRAL RENDER → storyboard '${storyboard.path}' is not a validable storyboard.")

        val rawDurations = parsed.beats.map { it.durationSeconds }
        val target = targetDurationSeconds.get()
        val durations = ViralTimeFitter.fit(rawDurations, target)
        logger.lifecycle(
            "CAPSULE VIRAL RENDER → time-fit: Σ${"%.2f".format(rawDurations.sum())}s " +
                "→ ${"%.2f".format(durations.sum())}s (target ${target}s, ±${toleranceSecs.get()}s)",
        )
        val resolvedPlatforms = platforms.get()
            .mapNotNull { name -> name.takeIf { it.isNotBlank() }?.let { ViralPlatform.fromString(it) } }
        val variants = ViralBatchPlanner.plan(languages.get(), resolvedPlatforms, targetDurationSeconds.get())
        if (variants.isEmpty()) {
            logger.lifecycle("CAPSULE VIRAL RENDER → skipped (empty variant matrix)")
            return
        }

        val videoDir = File(project.projectDir, renderedVideoDir.get())
        videoDir.mkdirs()
        val plan = ViralVariantRenderer.plan(variants, videoDir)

        val engine = resolveCapture()
        val converter = resolveConverter()
        try {
            val result = ViralVariantRenderer.render(
                plan = plan,
                isRenderable = { it.exists() && it.length() > 0L },
                render = { entry ->
                    renderVariant(entry, deck, durations, target, toleranceSecs.get(), engine, converter)
                },
            )
            logger.lifecycle(
                "CAPSULE VIRAL RENDER → ${result.renderedCount} rendered, " +
                    "${result.skippedCount} skipped, ${result.failedCount} failed → ${videoDir.absolutePath}",
            )
            if (result.failedCount > 0) {
                val message = "CAPSULE VIRAL RENDER → ${result.failedCount} variant(s) failed: " +
                    result.failed.joinToString(", ")
                if (strict.get()) error(message) else logger.warn(message)
            }
        } finally {
            engine.close()
        }
    }

    /** Captures the deck, transcodes to `entry.videoFile`, then gates the duration (US-2). */
    private fun renderVariant(
        entry: ViralVariantRenderEntry,
        deck: File,
        durations: List<Double>,
        targetSeconds: Int,
        toleranceSecs: Double,
        engine: PlaywrightCapture,
        converter: VideoFormatConverter,
    ): Boolean {
        val work = File(entry.videoFile.parentFile, "${entry.variant.id}-work")
        work.deleteRecursively()
        work.mkdirs()
        return try {
            engine.capture(
                deckHtmlPath = deck.absolutePath,
                outputDir = work,
                viewportWidth = ViralDeckBuilder.WIDTH,
                viewportHeight = ViralDeckBuilder.HEIGHT,
                slideDurations = durations,
            )
            val webm = File(work, "capsule.webm")
            val converted = converter.convertToMp4(webm, entry.videoFile)
            val produced = converted && entry.videoFile.exists() && entry.videoFile.length() > 0L
            if (!produced) {
                false
            } else {
                val seconds = MediaProbeUtil.probeDuration(entry.videoFile)
                val fitted = ViralTimeFitter.isFitted(seconds, targetSeconds, toleranceSecs)
                if (!fitted) {
                    logger.warn(
                        "CAPSULE VIRAL RENDER → variant '{}' produced {}s, outside target {}s ±{}s",
                        entry.variant.id, seconds, targetSeconds, toleranceSecs,
                    )
                }
                fitted
            }
        } catch (e: Exception) {
            logger.warn("CAPSULE VIRAL RENDER → variant '{}' failed: {}", entry.variant.id, e.message)
            false
        } finally {
            work.deleteRecursively()
        }
    }

    /** Resolves the capture engine — viral always uses the FFmpeg screenshot socle. */
    private fun resolveCapture(): PlaywrightCapture {
        captureFactory?.let { return it() }
        return CaptureResolver.resolve(
            strategy = CaptureStrategy.SCREENSHOT,
            strict = strict.get(),
            playwrightFactory = { NoOpPlaywrightCapture() },
            screenshotFactory = { ScreenshotCaptureImpl(timeout = captureTimeoutMs.get(), ffmpegPath = ffmpegPath.get()) },
            noOpCapture = NoOpPlaywrightCapture(),
        )
    }

    /** Resolves the MP4 converter (FFmpeg, or NoOp when unavailable / ffmpegPath=noop). */
    private fun resolveConverter(): VideoFormatConverter =
        formatConverter ?: CapsuleManager.resolveFormatConverter(ffmpegPath.get(), strict.get())
}
