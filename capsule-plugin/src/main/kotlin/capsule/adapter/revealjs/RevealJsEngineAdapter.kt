package capsule.adapter.revealjs

import capsule.CaptureResolver
import capsule.CaptureStrategy
import capsule.NoOpPlaywrightCapture
import capsule.PlaywrightCapture
import capsule.PlaywrightCaptureImpl
import capsule.ProcessRunner
import capsule.RemotionCaptureImpl
import capsule.RemotionPlanner
import capsule.ScreenshotCaptureImpl
import capsule.kernel.VideoEngine
import capsule.kernel.VideoRenderRequest
import capsule.kernel.VideoRenderResult
import capsule.viral.ViralCaptureStrategy
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Configuration snapshot the reveal.js engine adapter needs, decoupled from the
 * Gradle extension so the adapter stays testable and the kernel stays neutral
 * (CAP-ENGINE-DECOUPLING US-2).
 */
data class RevealJsCaptureSettings(
    val playwrightTimeout: Double,
    val defaultSlideDuration: Double,
    val configuredStrategy: CaptureStrategy,
    val viralEnabled: Boolean,
    val strict: Boolean,
    val ffmpegPath: String,
    val parallelThreads: Int,
    val previewOnly: Boolean,
    val chromiumPath: String,
    val remotionProjectDir: File,
    val remotionNodePath: String,
    val remotionConcurrency: Int,
    val remotionFps: Int,
    val manimSources: List<File>,
)

/**
 * Reveal.js video engine adapter — the concrete [VideoEngine] that renders a
 * reveal.js deck (CAP-ENGINE-DECOUPLING US-2).
 *
 * Extracted from `CapsuleVideoTask`: it owns the capture-engine resolution
 * (Playwright / Screenshot / Remotion via [CaptureResolver]), the parallel
 * per-slide capture using [RevealJsDeckHtml.singleSlideHtml], the whole-deck
 * capture, and the FFmpeg concatenation of per-slide clips. The task keeps only
 * the post-processing steps (audio mix, subtitle burn-in, audio post, format
 * conversion) which are engine-agnostic.
 *
 * A [captureProvider] can be supplied for tests to inject a fake
 * [PlaywrightCapture] (never a real browser); it is invoked once per engine.
 */
class RevealJsEngineAdapter(
    private val settings: RevealJsCaptureSettings,
    private val logger: Logger = Logging.getLogger(RevealJsEngineAdapter::class.java),
    private val captureProvider: (() -> PlaywrightCapture)? = null,
) : VideoEngine {

    private var cachedCapture: PlaywrightCapture? = null

    override fun name(): String = "revealjs"

    override fun isAvailable(): Boolean = true

    override fun close() {
        cachedCapture?.let { runCatching { it.close() } }
        cachedCapture = null
    }

    /** A fresh capture engine — one per parallel worker thread. */
    fun newCapture(segmentDurations: List<Double>): PlaywrightCapture =
        captureProvider?.invoke() ?: resolveFromSettings(segmentDurations)

    /** A single engine reused across a whole-source render. */
    fun resolveCapture(segmentDurations: List<Double>): PlaywrightCapture =
        captureProvider?.invoke() ?: (cachedCapture ?: resolveFromSettings(segmentDurations).also { cachedCapture = it })

    /**
     * Resolves the capture engine for the configured strategy, applying the
     * viral rule and the strict-mode guard. Falls back to noop when the engine
     * is unavailable and strict mode is off.
     */
    private fun resolveFromSettings(segmentDurations: List<Double>): PlaywrightCapture {
        val strategy = ViralCaptureStrategy.resolve(
            configured = settings.configuredStrategy,
            viralEnabled = settings.viralEnabled,
        )
        val resolved = CaptureResolver.resolve(
            strategy = strategy,
            strict = settings.strict,
            playwrightFactory = {
                PlaywrightCaptureImpl(
                    timeout = settings.playwrightTimeout,
                    defaultSlideDuration = settings.defaultSlideDuration,
                )
            },
            screenshotFactory = {
                ScreenshotCaptureImpl(
                    timeout = settings.playwrightTimeout,
                    ffmpegPath = settings.ffmpegPath,
                    encodeParallelism = settings.parallelThreads,
                    previewOnly = settings.previewOnly,
                )
            },
            noOpCapture = NoOpPlaywrightCapture(),
            enginePath = settings.chromiumPath,
            remotionFactory = {
                RemotionCaptureImpl(
                    projectDir = settings.remotionProjectDir,
                    nodeExecutablePath = settings.remotionNodePath,
                    concurrency = settings.remotionConcurrency,
                    fps = settings.remotionFps,
                    manimSources = settings.manimSources,
                )
            },
        )
        if (resolved.isAvailable()) {
            val totalSecs = segmentDurations.sum()
            logger.lifecycle(
                "Capture: {} strategy ({} slides, {}s total)",
                strategy.name.lowercase(),
                segmentDurations.size,
                String.format("%.1f", totalSecs),
            )
        } else {
            logger.warn("{} not available, falling back to noop capture", strategy.name.lowercase())
        }
        return resolved
    }

    override fun render(request: VideoRenderRequest): VideoRenderResult =
        if (request.parallel) {
            renderSlidesParallel(request)
        } else {
            renderWholeSource(request)
        }

    /**
     * Whole-source capture: the engine renders the deck in one pass; the task
     * post-processes the produced file (audio mix, subtitles, format).
     */
    private fun renderWholeSource(request: VideoRenderRequest): VideoRenderResult {
        val capture = resolveCapture(request.segmentDurations)
        try {
            capture.capture(
                deckHtmlPath = request.visual.file.absolutePath,
                outputDir = request.outputDir,
                viewportWidth = request.viewportWidth,
                viewportHeight = request.viewportHeight,
                slideDurations = request.segmentDurations,
            )
        } finally {
            capture.close()
            cachedCapture = null
        }

        val ext = capture.outputExtension()
        val produced = request.outputDir.listFiles { f -> f.name.endsWith(".$ext") }?.toList().orEmpty()
        val chosen = produced.firstOrNull { it.name == "${RemotionPlanner.FINAL_VIDEO_BASENAME}.$ext" }
            ?: produced.firstOrNull { !it.name.startsWith("slide-") }
            ?: produced.firstOrNull()
        return VideoRenderResult(produced = chosen)
    }

    /**
     * Parallel capture: one standalone slide HTML per segment, captured by a
     * per-worker engine, then concatenated into `<name>.<ext>`. A failing
     * segment is reported in [VideoRenderResult.failedSegments], never fatal.
     */
    private fun renderSlidesParallel(request: VideoRenderRequest): VideoRenderResult {
        val executor = Executors.newFixedThreadPool(settings.parallelThreads)
        val futures = mutableListOf<Future<File?>>()
        val failedSegments = mutableListOf<Int>()

        // One capture engine per worker thread, reused across that thread's
        // slides. Resolving per slide launched a browser for the availability
        // probe and a second one for the capture itself — two Chromium instances
        // per slide, which saturated the driver and made this mode unusable.
        val engines = ConcurrentLinkedQueue<PlaywrightCapture>()
        val threadEngine = ThreadLocal.withInitial {
            newCapture(request.segmentDurations).also { engines.add(it) }
        }

        val deckHtml = request.visual.file.readText()

        for ((idx, seg) in request.segments.withIndex()) {
            val slideDir = request.outputDir.resolve("slide-${String.format("%02d", seg.index)}")
            futures.add(executor.submit<File?> {
                val capture = threadEngine.get()
                try {
                    // Standalone HTML for this specific slide.
                    val singleSlideHtml = RevealJsDeckHtml.singleSlideHtml(deckHtml, idx)
                    val singleSlideFile = slideDir.resolve("slide.html")
                    slideDir.mkdirs()
                    singleSlideFile.writeText(singleSlideHtml)

                    capture.capture(
                        singleSlideFile.absolutePath,
                        slideDir,
                        request.viewportWidth,
                        request.viewportHeight,
                        listOf(request.segmentDurations[idx]),
                    )
                    // No capture engine writes a file called `slide.webm`:
                    // Playwright names its recording with a random id,
                    // ScreenshotCaptureImpl emits `capsule.webm`. Take whatever
                    // video landed in the slide directory.
                    val slideExt = capture.outputExtension()
                    val produced = slideDir.listFiles { f -> f.name.endsWith(".$slideExt") }?.toList().orEmpty()
                    val source = produced.firstOrNull {
                        it.name == "${RemotionPlanner.FINAL_VIDEO_BASENAME}.$slideExt"
                    }
                        ?: produced.firstOrNull { !it.name.startsWith("slide-") }
                        ?: produced.firstOrNull()
                    if (source != null && source.exists()) {
                        val target = request.outputDir.resolve("slide-${String.format("%02d", seg.index)}.$slideExt")
                        source.copyTo(target, overwrite = true)
                        target
                    } else null
                } catch (e: Exception) {
                    // Degraded mode: a failing slide is reported, never fatal.
                    logger.warn("  Slide {} capture failed: {}", seg.index, e.message)
                    synchronized(failedSegments) { failedSegments.add(seg.index) }
                    null
                }
            })
        }

        val slideVideos = try {
            futures.mapNotNull { future ->
                try {
                    future.get(request.timeoutMillis, TimeUnit.MILLISECONDS)
                } catch (e: TimeoutException) {
                    logger.error(
                        "Parallel capture timed out after {} ms — aborting. Shutting down executor.",
                        request.timeoutMillis,
                    )
                    throw e
                }
            }
        } finally {
            executor.shutdownNow()
            engines.forEach { engine ->
                runCatching { engine.close() }
                    .onFailure { logger.info("Capture engine close failed: {}", it.message) }
            }
        }

        if (failedSegments.isNotEmpty()) {
            logger.error(
                "{} slide(s) failed to capture: {} — resulting video may be incomplete",
                failedSegments.size,
                failedSegments.sorted().joinToString(", "),
            )
        }

        val produced = if (slideVideos.isNotEmpty()) {
            val finalVideo = request.outputDir.resolve("${request.name}.${slideVideos.first().extension}")
            concat(slideVideos, finalVideo, settings.ffmpegPath)
            finalVideo.takeIf { it.exists() }
        } else null

        return VideoRenderResult(produced = produced, failedSegments = failedSegments.toList())
    }

    companion object {

        /**
         * Concatenates per-slide video files into [outputFile] via the FFmpeg
         * concat demuxer (`-c copy`, no re-encode), then deletes the inputs when
         * the concat succeeds (Economy of Ink).
         */
        @JvmStatic
        fun concat(slideVideos: List<File>, outputFile: File, ffmpegPath: String = "ffmpeg"): Boolean {
            if (slideVideos.isEmpty()) return false
            val concatList = File(outputFile.parentFile, "concat-${System.currentTimeMillis()}.txt")
            concatList.writeText(slideVideos.joinToString("\n") { "file '${it.absolutePath.replace("'", "'\\''")}'" })
            val success = try {
                val result = ProcessRunner.run(
                    listOf(
                        ffmpegPath, "-y", "-f", "concat", "-safe", "0",
                        "-i", concatList.absolutePath, "-c", "copy", outputFile.absolutePath,
                    )
                )
                result.isSuccess && outputFile.exists()
            } catch (e: Exception) {
                false
            } finally {
                concatList.delete()
            }
            if (success) {
                slideVideos.forEach { it.delete() }
            }
            return success
        }
    }
}
