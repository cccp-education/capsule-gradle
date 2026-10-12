package capsule.kernel

import java.io.File

/**
 * Engine-neutral port that renders a [VisualSource] into a video
 * (CAP-ENGINE-DECOUPLING US-2).
 *
 * The kernel defines *what* an engine does — turn visuals + narration segments
 * into a produced video — never *how*. A concrete engine (Reveal.js capture via
 * Playwright today, HyperFrames later) is an adapter that lives outside the
 * kernel and never leaks its renderer vocabulary (slide/deck/reveal/Playwright)
 * into this contract.
 *
 * An engine is a stateful resource (a browser, a process pool): callers must
 * invoke [close] once the production is done. A failure to render is reported
 * through [VideoRenderResult.failedSegments] rather than thrown, so a degraded
 * single segment never aborts the whole production.
 */
interface VideoEngine {

    /** Human-readable engine name (diagnostics only). */
    fun name(): String

    /** `true` when the engine can actually render (binaries/browser present). */
    fun isAvailable(): Boolean

    /**
     * Renders the production described by [request].
     *
     * @return the produced video file (or `null` when nothing was produced) and
     *         the indices of the segments whose capture failed (never fatal).
     */
    fun render(request: VideoRenderRequest): VideoRenderResult

    /** Releases the engine's resources (browser, executor, …). Idempotent. */
    fun close()
}

/**
 * A single render instruction — the neutral unit of work handed to a
 * [VideoEngine].
 *
 * @property name            production name (without extension), used to name
 *                           the produced video (`<name>.<ext>`).
 * @property visual          the engine-neutral visual source to render.
 * @property segments        ordered narration segments (drives the output order).
 * @property slideDurations  per-segment duration in seconds, aligned with
 *                           [segments] by index.
 * @property outputDir       directory the engine writes produced files into.
 * @property viewportWidth   capture viewport width in pixels.
 * @property viewportHeight  capture viewport height in pixels.
 * @property parallel        `true` to render each segment independently (a
 *                           standalone visual per segment) and concatenate;
 *                           `false` to render the whole source in one pass.
 * @property timeoutMillis   per-segment capture timeout in milliseconds.
 */
data class VideoRenderRequest(
    val name: String,
    val visual: VisualSource,
    val segments: List<VideoSegment>,
    val slideDurations: List<Double>,
    val outputDir: File,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val parallel: Boolean,
    val timeoutMillis: Long,
)

/**
 * Outcome of a [VideoEngine] render.
 *
 * @property produced        the produced video file, or `null` when the engine
 *                           wrote nothing.
 * @property failedSegments  indices of segments whose individual capture failed
 *                           (parallel mode); empty on a clean render.
 */
data class VideoRenderResult(
    val produced: File?,
    val failedSegments: List<Int> = emptyList(),
)
