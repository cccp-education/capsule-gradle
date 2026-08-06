package capsule

import capsule.feed.CapsuleScript
import capsule.feed.SlideSegment
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * TDD baby-step — CR-2 Thread safety + timeout for captureSlideParallel.
 *
 * EPIC CR-2: captureSlideParallel must (1) bound Future.get with a timeout and
 * (2) shut down the executor even when a slide capture throws or blocks.
 *
 * Tests written BEFORE production code (RED), then production code is adjusted
 * until all tests pass (GREEN).
 *
 * Scope: unit-level, no real Playwright/FFmpeg. The capture factory is mocked.
 */
class CaptureSlideParallelTest {

    @TempDir
    lateinit var tempDir: File

    private fun newTask(): CapsuleVideoTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")
        ext.viewportWidth.set(1408)
        ext.viewportHeight.set(792)
        ext.slideDurationSeconds.set(5.0)
        ext.playwrightTimeout.set(120_000.0)
        val task = project.tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java).get()
        task.capsuleExtension = ext
        return task
    }

    private fun fakeDeckHtml(slides: Int): File {
        val deckFile = File(tempDir, "deck.html")
        deckFile.writeText(
            """
            <html><head><style>body{margin:0}</style></head><body>
            <div class="reveal"><div class="slides">
            ${(1..slides).joinToString("\n") { i -> """<section data-capsule-slide="$i"><h2>Slide $i</h2></section>""" }}
            </div></div>
            <script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
            <script>Reveal.initialize();</script>
            </body></html>
            """.trimIndent()
        )
        return deckFile
    }

    // ─── CR-2.1 — Future.get bounded by timeout ─────────────────────────

    @Test
    fun `captureSlideParallel times out when a slide capture blocks beyond timeout`() {
        val task = newTask()
        val script = CapsuleScript(
            deckName = "timeout-deck",
            segments = (1..2).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }
        fakeDeckHtml(2)

        val blockingCapture = object : PlaywrightCapture {
            override fun capture(
                deckHtmlPath: String, outputDir: File, viewportWidth: Int,
                viewportHeight: Int, slideDurations: List<Double>
            ) {
                // Block indefinitely — must trigger a timeout, not hang the build.
                Thread.sleep(Long.MAX_VALUE)
            }
            override fun isAvailable() = true
            override fun name() = "blocking"
            override fun close() {}
        }

        val start = System.currentTimeMillis()
        try {
            task.captureSlideParallel(
                deckHtmlPath = File(tempDir, "deck.html").absolutePath,
                outputDir = outputDir,
                viewportWidth = 1408,
                viewportHeight = 792,
                parsed = script,
                audioDir = audioDir,
                captureFactory = { blockingCapture },
                captureTimeoutMillis = 1_000
            )
            fail("Expected a timeout to surface from captureSlideParallel")
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - start
            assertTrue(
                elapsed < 60_000,
                "Must abort within the configured timeout (was ${elapsed}ms), not hang indefinitely"
            )
        }
    }

    // ─── CR-2.2 — executor is shut down even when a capture throws ───────

    @Test
    fun `captureSlideParallel shuts down the executor when a capture throws`() {
        val task = newTask()
        val script = CapsuleScript(
            deckName = "throwing-deck",
            segments = (1..3).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }
        fakeDeckHtml(3)

        val closed = AtomicBoolean(false)
        val throwingCapture = object : PlaywrightCapture {
            override fun capture(
                deckHtmlPath: String, outputDir: File, viewportWidth: Int,
                viewportHeight: Int, slideDurations: List<Double>
            ) {
                throw RuntimeException("capture boom")
            }
            override fun isAvailable() = true
            override fun name() = "throwing"
            override fun close() { closed.set(true) }
        }

        try {
            task.captureSlideParallel(
                deckHtmlPath = File(tempDir, "deck.html").absolutePath,
                outputDir = outputDir,
                viewportWidth = 1408,
                viewportHeight = 792,
                parsed = script,
                audioDir = audioDir,
                captureFactory = { throwingCapture },
                captureTimeoutMillis = 1_000
            )
        } catch (e: Exception) {
            // Expected: the exception surfaces (or failedSlides handles it in later steps).
        }

        // Give the executor a moment to terminate after shutdownNow.
        Thread.sleep(200)
        assertTrue(closed.get(), "close() should still be called on throwing captures (per-call finally)")
    }

    @Test
    fun `captureSlideParallel interrupts in-flight captures via shutdownNow`() {
        val task = newTask()
        val script = CapsuleScript(
            deckName = "interrupt-deck",
            segments = (1..2).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }
        fakeDeckHtml(2)

        val interrupted = AtomicBoolean(false)
        val interruptedLatch = CountDownLatch(2)
        val loopingCapture = object : PlaywrightCapture {
            override fun capture(
                deckHtmlPath: String, outputDir: File, viewportWidth: Int,
                viewportHeight: Int, slideDurations: List<Double>
            ) {
                // Loop until interrupted by shutdownNow (would otherwise run forever).
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        Thread.sleep(10)
                    } catch (e: InterruptedException) {
                        interrupted.set(true)
                        interruptedLatch.countDown()
                        Thread.currentThread().interrupt()
                        return
                    }
                }
            }
            override fun isAvailable() = true
            override fun name() = "looping"
            override fun close() {}
        }

        try {
            task.captureSlideParallel(
                deckHtmlPath = File(tempDir, "deck.html").absolutePath,
                outputDir = outputDir,
                viewportWidth = 1408,
                viewportHeight = 792,
                parsed = script,
                audioDir = audioDir,
                captureFactory = { loopingCapture },
                captureTimeoutMillis = 500
            )
        } catch (e: Exception) {
            // Expected: timeout surfaces once the in-flight tasks never complete.
        }

        // Wait for both in-flight captures to observe the interruption from shutdownNow.
        interruptedLatch.await(5, TimeUnit.SECONDS)
        assertTrue(
            interrupted.get(),
            "In-flight captures must be interrupted by shutdownNow once the timeout aborts the capture"
        )
    }

    // ─── CR-2.4 — failed slides are accumulated, not fatal ──────────────

    @Test
    fun `captureSlideParallel accumulates failed slides and continues with the rest`() {
        val task = newTask()
        val script = CapsuleScript(
            deckName = "mixed-deck",
            segments = (1..4).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }
        fakeDeckHtml(4)

        // Slides 1 and 3 throw during capture; slides 2 and 4 succeed.
        val factory: () -> PlaywrightCapture = {
            object : PlaywrightCapture {
                override fun capture(
                    deckHtmlPath: String, outputDir: File, viewportWidth: Int,
                    viewportHeight: Int, slideDurations: List<Double>
                ) {
                    if (outputDir.name == "slide-01" || outputDir.name == "slide-03") {
                        throw RuntimeException("boom on ${outputDir.name}")
                    }
                    outputDir.mkdirs()
                    outputDir.resolve("slide.webm").writeText("fake")
                }
                override fun isAvailable() = true
                override fun name() = "mixed"
                override fun close() {}
            }
        }

        val failed = task.captureSlideParallel(
            deckHtmlPath = File(tempDir, "deck.html").absolutePath,
            outputDir = outputDir,
            viewportWidth = 1408,
            viewportHeight = 792,
            parsed = script,
            audioDir = audioDir,
            captureFactory = factory,
            captureTimeoutMillis = 10_000
        )

        assertEquals(2, failed, "Two slides (1 and 3) should be reported as failed")
        assertTrue(outputDir.resolve("slide-02.webm").exists(), "Slide 2 capture should still be produced")
        assertTrue(outputDir.resolve("slide-04.webm").exists(), "Slide 4 capture should still be produced")
    }
}