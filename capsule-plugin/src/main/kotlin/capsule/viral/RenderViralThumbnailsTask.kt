package capsule.viral

import capsule.CaptureResolver
import capsule.CaptureStrategy
import capsule.NoOpPlaywrightCapture
import capsule.PlaywrightCapture
import capsule.ScreenshotCaptureImpl
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
 * Gradle task: `renderViralThumbnails` (CAP-VIRAL-E2E US-2).
 *
 * Produces one portrait cover PNG per campaign variant (`<variant.id>-thumb.png`)
 * by capturing the cover HTML written by `generateViralCampaign`
 * ([ViralThumbnailPlanner] + [ViralThumbnailBuilder]) through the deterministic
 * FFmpeg screenshot socle (preview mode: PNG only, no video).
 *
 * Symmetric with [RenderViralVariantsTask]: the shared source is captured **once**
 * and replicated to every variant (economy of ink); an already-present
 * `<id>-thumb.png` is skipped. The manifest's [CampaignBundle.thumbnailFile]
 * references these files (wired by `generateViralCampaign`); the delivery gate
 * (`validateViralCampaign`, CAP-VIRAL-E2E US-1) then verifies they exist.
 *
 * Inputs:
 *   - [coverFile]     the portrait cover HTML (`viral-thumbnail.html`).
 *   - [languages]/[platforms]/[targetDurationSeconds] the batch matrix.
 *   - [renderedVideoDir] where `<variant.id>-thumb.png` is written.
 *
 * Usage:
 *   ./gradlew renderViralThumbnails -Pcapsule.viral.enabled=true
 */
@DisableCachingByDefault(because = "Viral thumbnail render — Playwright screenshot of the cover")
abstract class RenderViralThumbnailsTask : DefaultTask() {

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

    /** Directory (relative to the project) holding `<variant.id>-thumb.png`. */
    @get:Input
    abstract val renderedVideoDir: Property<String>

    /** FFmpeg executable path (or `noop` for the degraded test path). */
    @get:Input
    abstract val ffmpegPath: Property<String>

    /** When true, a failed cover capture fails the build. */
    @get:Input
    abstract val strict: Property<Boolean>

    /** Capture timeout (milliseconds). */
    @get:Input
    abstract val captureTimeoutMs: Property<Double>

    /** Capturable portrait cover HTML (US-2). */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val coverFile: RegularFileProperty

    /** Test seam — the capture engine (defaults to the FFmpeg screenshot socle in preview mode). */
    @get:Internal
    internal var captureFactory: (() -> PlaywrightCapture)? = null

    @TaskAction
    fun run() {
        if (!viralEnabled.get()) {
            logger.lifecycle("CAPSULE VIRAL THUMBNAILS → skipped (viralEnabled=false)")
            return
        }

        val cover = coverFile.orNull?.asFile
        if (cover == null || !cover.exists()) {
            failOrWarn(
                "CAPSULE VIRAL THUMBNAILS → no cover (run `generateViralCampaign` first; " +
                    "expected ${coverFile.orNull?.asFile?.path}).",
            )
            return
        }

        val resolvedPlatforms = platforms.get()
            .mapNotNull { name -> name.takeIf { it.isNotBlank() }?.let { ViralPlatform.fromString(it) } }
        val variants = ViralBatchPlanner.plan(languages.get(), resolvedPlatforms, targetDurationSeconds.get())
        if (variants.isEmpty()) {
            logger.lifecycle("CAPSULE VIRAL THUMBNAILS → skipped (empty variant matrix)")
            return
        }

        val dir = File(project.projectDir, renderedVideoDir.get())
        dir.mkdirs()

        val pending = variants.filter { !isRenderable(File(dir, ViralThumbnailBuilder.fileNameFor(it.id))) }
        if (pending.isEmpty()) {
            logger.lifecycle(
                "CAPSULE VIRAL THUMBNAILS → all ${variants.size} cover(s) present, skipping (economy of ink)",
            )
            return
        }

        val work = File(dir, "_thumb-work")
        work.deleteRecursively()
        work.mkdirs()
        val engine = resolveCapture()
        try {
            val source = captureCover(cover, work, engine)
            if (source == null) {
                failOrWarn("CAPSULE VIRAL THUMBNAILS → cover capture produced no PNG")
                return
            }
            val rendered = pending.count { replicate(source, File(dir, ViralThumbnailBuilder.fileNameFor(it.id))) }
            val failed = pending.size - rendered
            logger.lifecycle(
                "CAPSULE VIRAL THUMBNAILS → $rendered rendered, ${variants.size - pending.size} skipped, " +
                    "$failed failed → ${dir.absolutePath}",
            )
            if (failed > 0) failOrWarn("CAPSULE VIRAL THUMBNAILS → $failed variant(s) failed")
        } finally {
            engine.close()
            work.deleteRecursively()
        }
    }

    /** Captures the cover HTML once into [work]; returns the produced PNG, or null on failure. */
    private fun captureCover(cover: File, work: File, engine: PlaywrightCapture): File? =
        runCatching {
            engine.capture(
                deckHtmlPath = cover.absolutePath,
                outputDir = work,
                viewportWidth = ViralDeckBuilder.WIDTH,
                viewportHeight = ViralDeckBuilder.HEIGHT,
                slideDurations = listOf(COVER_SLIDE_SECONDS),
            )
            File(work, "slide-0.png").takeIf { isRenderable(it) }
        }.getOrElse { e ->
            logger.warn("CAPSULE VIRAL THUMBNAILS → cover capture failed: {}", e.message)
            null
        }

    /** Copies a produced cover to a variant's file (batch replication). */
    private fun replicate(source: File, target: File): Boolean =
        runCatching {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
            isRenderable(target)
        }.getOrDefault(false)

    private fun isRenderable(file: File): Boolean = file.exists() && file.length() > 0L

    /** Fails the build when [strict], otherwise warns (degraded, non-blocking). */
    private fun failOrWarn(message: String) {
        if (strict.get()) error(message) else logger.warn(message)
    }

    /** Resolves the capture engine — the cover always uses the FFmpeg screenshot socle (preview). */
    private fun resolveCapture(): PlaywrightCapture {
        captureFactory?.let { return it() }
        return CaptureResolver.resolve(
            strategy = CaptureStrategy.SCREENSHOT,
            strict = strict.get(),
            playwrightFactory = { NoOpPlaywrightCapture() },
            screenshotFactory = {
                ScreenshotCaptureImpl(
                    timeout = captureTimeoutMs.get(),
                    ffmpegPath = ffmpegPath.get(),
                    previewOnly = true,
                )
            },
            noOpCapture = NoOpPlaywrightCapture(),
        )
    }

    private companion object {
        /** Cover slide duration (seconds) — only a still PNG is kept, never encoded. */
        const val COVER_SLIDE_SECONDS: Double = 1.0
    }
}
