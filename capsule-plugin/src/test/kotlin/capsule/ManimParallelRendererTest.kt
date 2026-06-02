package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * TDD Baby Step — ManimParallelRenderer unit tests.
 *
 * EPIC CAP-20: Manim parallel render.
 * Tests written BEFORE production code (baby-step TDD).
 *
 * Design:
 * - Interface ManimParallelRenderer with renderAll(slides, manimEngine, scriptsDir, outputDir)
 * - NoOpManimParallelRenderer: sequential fallback, iterates slides one by one
 * - ManimParallelRendererImpl: parallel render using thread pool
 * - Factory: CapsuleManager.resolveManimParallelRenderer()
 */
class ManimParallelRendererTest {

    // ─── NoOpManimParallelRenderer ──────────────────────────────────────

    @Test
    fun `noop renderer renders all manim slides sequentially`() {
        val renderer = NoOpManimParallelRenderer()
        val tmpDir = File.createTempFile("manim-parallel-noop", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val slides = listOf(
            SlideSegment(1, "Scene1", "Note 1", type = SlideType.MANIM, manimScene = "Scene1"),
            SlideSegment(2, "Scene2", "Note 2", type = SlideType.MANIM, manimScene = "Scene2")
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()

        // Create placeholder .py scripts
        File(scriptsDir, "Scene1.py").writeText("# Scene1")
        File(scriptsDir, "Scene2.py").writeText("# Scene2")

        val outputDir = tmpDir.resolve("manim-output")
        outputDir.mkdirs()

        val results = renderer.renderAll(slides, engine, scriptsDir, outputDir)

        assertEquals(2, results.size, "NoOp renderer should return results for all manim slides")
        assertTrue(results.keys.contains(1), "Results should contain slide index 1")
        assertTrue(results.keys.contains(2), "Results should contain slide index 2")
        assertTrue(results[1]!!.exists(), "Rendered file for slide 1 should exist")
        assertTrue(results[2]!!.exists(), "Rendered file for slide 2 should exist")
    }

    @Test
    fun `noop renderer returns empty map for empty slides`() {
        val renderer = NoOpManimParallelRenderer()
        val tmpDir = File.createTempFile("manim-parallel-empty", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val outputDir = tmpDir.resolve("output")

        val results = renderer.renderAll(emptyList(), engine, tmpDir, outputDir)
        assertTrue(results.isEmpty(), "NoOp renderer should return empty map for empty slides")
    }

    @Test
    fun `noop renderer is available and named noop-parallel-renderer`() {
        val renderer = NoOpManimParallelRenderer()
        assertTrue(renderer.isAvailable())
        assertEquals("noop-parallel-renderer", renderer.name())
    }

    @Test
    fun `noop renderer skips slides without manimScene`() {
        val renderer = NoOpManimParallelRenderer()
        val tmpDir = File.createTempFile("manim-parallel-noscene", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val slides = listOf(
            SlideSegment(1, "NoScene", "Note", type = SlideType.MANIM, manimScene = null)
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()

        val outputDir = tmpDir.resolve("output")
        outputDir.mkdirs()

        val results = renderer.renderAll(slides, engine, scriptsDir, outputDir)
        // Slides without manimScene should be skipped (no sceneName to render)
        assertTrue(results.isEmpty(), "Slides without manimScene should be skipped")
    }

    @Test
    fun `noop renderer skips slides where script file does not exist`() {
        val renderer = NoOpManimParallelRenderer()
        val tmpDir = File.createTempFile("manim-parallel-noscript", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val slides = listOf(
            SlideSegment(1, "Missing", "Note", type = SlideType.MANIM, manimScene = "MissingScene")
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()
        // No .py file created for MissingScene

        val outputDir = tmpDir.resolve("output")

        val results = renderer.renderAll(slides, engine, scriptsDir, outputDir)
        // Script not found → slide is skipped
        assertTrue(results.isEmpty(), "Slides with missing script should be skipped")
    }

    // ─── ManimParallelRendererImpl (without real manim) ─────────────────

    @Test
    fun `parallel renderer impl reports available`() {
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        assertTrue(renderer.isAvailable(), "Parallel renderer should always be available")
    }

    @Test
    fun `parallel renderer impl name is manim-parallel-renderer`() {
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        assertEquals("manim-parallel-renderer", renderer.name())
    }

    @Test
    fun `parallel renderer renders all manim slides with NoOp engine`() {
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        val tmpDir = File.createTempFile("manim-parallel-impl", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val slides = listOf(
            SlideSegment(1, "Scene1", "Note 1", type = SlideType.MANIM, manimScene = "Scene1"),
            SlideSegment(2, "Scene2", "Note 2", type = SlideType.MANIM, manimScene = "Scene2"),
            SlideSegment(3, "Scene3", "Note 3", type = SlideType.MANIM, manimScene = "Scene3")
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()
        File(scriptsDir, "Scene1.py").writeText("# Scene1")
        File(scriptsDir, "Scene2.py").writeText("# Scene2")
        File(scriptsDir, "Scene3.py").writeText("# Scene3")

        val outputDir = tmpDir.resolve("manim-output")
        outputDir.mkdirs()

        val results = renderer.renderAll(slides, engine, scriptsDir, outputDir)

        assertEquals(3, results.size, "Should render all 3 manim slides")
        assertTrue(results[1]!!.exists(), "Scene1 MP4 should exist")
        assertTrue(results[2]!!.exists(), "Scene2 MP4 should exist")
        assertTrue(results[3]!!.exists(), "Scene3 MP4 should exist")
    }

    @Test
    fun `parallel renderer returns empty for empty slides`() {
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        val tmpDir = File.createTempFile("manim-parallel-empty2", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()

        val results = renderer.renderAll(emptyList(), engine, tmpDir, tmpDir)
        assertTrue(results.isEmpty(), "Should return empty map for empty slides")
    }

    @Test
    fun `parallel renderer skips slides without manimScene`() {
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        val tmpDir = File.createTempFile("manim-parallel-noscene2", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val engine = NoOpManimEngine()
        val slides = listOf(
            SlideSegment(1, "NoScene", "Note", type = SlideType.MANIM, manimScene = null),
            SlideSegment(2, "WithScene", "Note", type = SlideType.MANIM, manimScene = "WithScene")
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()
        File(scriptsDir, "WithScene.py").writeText("# WithScene")

        val outputDir = tmpDir.resolve("output")

        val results = renderer.renderAll(slides, engine, scriptsDir, outputDir)
        assertEquals(1, results.size, "Should only render the slide with manimScene")
        assertTrue(results.containsKey(2), "Should have result for slide 2")
        assertFalse(results.containsKey(1), "Should not have result for slide without manimScene")
    }

    @Test
    fun `parallel renderer default parallelism is 4`() {
        val renderer = ManimParallelRendererImpl()
        assertEquals(4, renderer.parallelism, "Default parallelism should be 4")
    }

    @Test
    fun `parallel renderer handles ManimException gracefully`() {
        // When a render fails, the other slides should still complete
        val renderer = ManimParallelRendererImpl(parallelism = 2)
        val tmpDir = File.createTempFile("manim-parallel-exc", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        // Use a real ManimEngineImpl that will fail (no manim binary)
        val config = ManimConfig(executablePath = "/nonexistent/path/manim")
        val failingEngine = ManimEngineImpl(config)

        val slides = listOf(
            SlideSegment(1, "Fail", "Note", type = SlideType.MANIM, manimScene = "FailScene")
        )
        val scriptsDir = tmpDir.resolve("manim-scripts")
        scriptsDir.mkdirs()
        File(scriptsDir, "FailScene.py").writeText("# FailScene")

        val outputDir = tmpDir.resolve("output")

        // The parallel renderer should catch ManimException and skip the failed slide
        val results = renderer.renderAll(slides, failingEngine, scriptsDir, outputDir)
        // Since the engine is not available, it will throw ManimException
        // The renderer should handle this gracefully (not crash)
        assertTrue(results.isEmpty(), "Failed renders should be skipped, not crash")
    }

    // ─── Factory ───────────────────────────────────────────────────────

    @Test
    fun `resolveManimParallelRenderer returns NoOp when parallelism is 1`() {
        val renderer = CapsuleManager.resolveManimParallelRenderer(parallelism = 1)
        assertTrue(renderer is NoOpManimParallelRenderer, "parallelism=1 should return NoOp (sequential)")
    }

    @Test
    fun `resolveManimParallelRenderer returns parallel renderer when parallelism greater than 1`() {
        val renderer = CapsuleManager.resolveManimParallelRenderer(parallelism = 4)
        assertTrue(renderer is ManimParallelRendererImpl, "parallelism>1 should return parallel renderer")
    }
}