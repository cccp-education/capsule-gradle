package capsule

import org.gradle.api.logging.Logging
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap

/**
 * Renders multiple Manim slides in parallel using a thread pool.
 *
 * When a deck contains multiple slides with type MANIM, rendering them
 * sequentially (one by one) is slow. This interface abstracts the
 * parallel rendering strategy.
 *
 * Design:
 * - ManimParallelRendererImpl: uses Executors.newFixedThreadPool for parallel render
 * - NoOpManimParallelRenderer: sequential fallback (iterates slides one by one)
 * - Factory: CapsuleManager.resolveManimParallelRenderer(parallelism)
 *
 * TDD Baby Step — EPIC CAP-20: Manim parallel render.
 */
interface ManimParallelRenderer {
    /**
     * Renders all MANIM slides in parallel (or sequentially for NoOp).
     *
     * @param slides The list of SlideSegments to render (typically only MANIM slides)
     * @param manimEngine The ManimEngine to use for rendering
     * @param scriptsDir Directory containing .py Manim scripts
     * @param outputDir Directory to write rendered MP4 files
     * @return Map of slide index → rendered MP4 File. Slides that fail to render are omitted.
     */
    fun renderAll(
        slides: List<SlideSegment>,
        manimEngine: ManimEngine,
        scriptsDir: File,
        outputDir: File
    ): Map<Int, File>

    /** Returns true if the renderer is available */
    fun isAvailable(): Boolean

    /** Returns the renderer name for logging */
    fun name(): String
}

/**
 * Sequential fallback renderer — iterates slides one by one.
 * Used when parallelism is 1 or when the thread pool is not desired.
 */
class NoOpManimParallelRenderer : ManimParallelRenderer {

    override fun isAvailable(): Boolean = true

    override fun name(): String = "noop-parallel-renderer"

    override fun renderAll(
        slides: List<SlideSegment>,
        manimEngine: ManimEngine,
        scriptsDir: File,
        outputDir: File
    ): Map<Int, File> {
        val results = mutableMapOf<Int, File>()
        for (seg in slides) {
            val sceneName = seg.manimScene ?: continue
            val scriptPath = scriptsDir.resolve("$sceneName.py")
            if (!scriptPath.exists()) continue

            val sceneOutputDir = File(outputDir, sceneName)
            sceneOutputDir.mkdirs()

            try {
                val rendered = manimEngine.render(sceneName, scriptPath, sceneOutputDir)
                results[seg.index] = rendered
            } catch (_: ManimException) {
                // Skip slides with ManimException (e.g., binary not available)
            }
        }
        return results
    }
}

/**
 * Parallel Manim renderer using a fixed thread pool.
 *
 * Each slide is submitted to the pool as a callable.
 * Slides that fail (ManimException) are skipped gracefully.
 * The thread pool is created and destroyed per renderAll() call.
 *
 * @param parallelism Number of threads in the pool (default: 4)
 */
class ManimParallelRendererImpl(
    val parallelism: Int = 4
) : ManimParallelRenderer {

    private val logger = Logging.getLogger(ManimParallelRendererImpl::class.java)

    override fun isAvailable(): Boolean = true

    override fun name(): String = "manim-parallel-renderer"

    override fun renderAll(
        slides: List<SlideSegment>,
        manimEngine: ManimEngine,
        scriptsDir: File,
        outputDir: File
    ): Map<Int, File> {
        if (slides.isEmpty()) return emptyMap()

        val results = ConcurrentHashMap<Int, File>()
        val effectiveParallelism = parallelism.coerceAtMost(slides.size)
        val executor = Executors.newFixedThreadPool(effectiveParallelism)

        try {
            val futures = slides.map { seg ->
                executor.submit(Callable {
                    val sceneName = seg.manimScene ?: return@Callable null
                    val scriptPath = scriptsDir.resolve("$sceneName.py")
                    if (!scriptPath.exists()) return@Callable null

                    val sceneOutputDir = File(outputDir, sceneName)
                    sceneOutputDir.mkdirs()

                    try {
                        val rendered = manimEngine.render(sceneName, scriptPath, sceneOutputDir)
                        results[seg.index] = rendered
                        logger.lifecycle("    Manim parallel render: slide {} → {} (scene: {})", seg.index, rendered.name, sceneName)
                    } catch (e: ManimException) {
                        logger.warn("    Manim parallel render failed for scene '{}': {}", sceneName, e.message)
                    }
                    null
                })
            }

            // Wait for all tasks to complete
            futures.forEach { it.get() }
        } finally {
            executor.shutdown()
        }

        return results.toSortedMap()
    }
}