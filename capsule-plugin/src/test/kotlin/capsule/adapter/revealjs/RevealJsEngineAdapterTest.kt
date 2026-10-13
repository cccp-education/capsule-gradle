package capsule.adapter.revealjs

import capsule.CaptureStrategy
import capsule.PlaywrightCapture
import capsule.kernel.FileVisualSource
import capsule.kernel.VideoRenderRequest
import capsule.kernel.segment.VideoSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * TDD — reveal.js engine adapter ([capsule.kernel.VideoEngine] implementation)
 * (video-kernel US-1, 0.0.2).
 *
 * Uses a fake [PlaywrightCapture] (never a real browser); FFmpeg is pointed at a
 * non-existent path so the concat step degrades without touching the system.
 */
class RevealJsEngineAdapterTest {

    @TempDir
    lateinit var tempDir: Path

    private val segments = listOf(
        VideoSegment(index = 1, title = "One", narration = "one."),
        VideoSegment(index = 2, title = "Two", narration = "two."),
    )

    private fun settings() = RevealJsCaptureSettings(
        playwrightTimeout = 1_000.0,
        defaultSlideDuration = 1.0,
        configuredStrategy = CaptureStrategy.SCREENSHOT,
        viralEnabled = false,
        strict = false,
        ffmpegPath = "/nonexistent/ffmpeg",
        parallelThreads = 1,
        previewOnly = false,
        chromiumPath = "",
        remotionProjectDir = tempDir.resolve("remotion").toFile(),
        remotionNodePath = "node",
        remotionConcurrency = 1,
        remotionFps = 30,
        manimSources = emptyList(),
    )

    private fun plan(parallel: Boolean) = RevealJsRenderPlan(
        segments = segments,
        segmentDurations = listOf(1.0, 1.0),
        viewportWidth = 1920,
        viewportHeight = 1080,
        parallel = parallel,
        captureTimeoutMillis = 5_000,
    )

    private fun deckFile(): File =
        tempDir.resolve("deck.html").toFile().apply {
            writeText(
                """
                <html><head></head><body><div class="reveal"><div class="slides">
                <section data-capsule-slide="1"><h1>One</h1></section>
                <section data-capsule-slide="2"><h1>Two</h1></section>
                </div></div></body></html>
                """.trimIndent()
            )
        }

    private fun request(parallelOutputDir: String) = VideoRenderRequest(
        name = "demo",
        visual = FileVisualSource(deckFile()),
        outputDir = tempDir.resolve(parallelOutputDir).toFile(),
        timeoutMillis = 5_000,
    )

    private open class FakeCapture(
        private val outputName: String = "capsule.webm",
        private val failWhenPathContains: String? = null,
    ) : PlaywrightCapture {
        override fun isAvailable(): Boolean = true
        override fun name(): String = "fake-capture"
        override fun outputExtension(): String = "webm"
        override fun close() {}
        override fun capture(
            deckHtmlPath: String,
            outputDir: File,
            viewportWidth: Int,
            viewportHeight: Int,
            slideDurations: List<Double>,
        ) {
            if (failWhenPathContains != null && deckHtmlPath.contains(failWhenPathContains)) {
                throw RuntimeException("boom on $deckHtmlPath")
            }
            outputDir.mkdirs()
            File(outputDir, outputName).writeText("dummy")
        }
    }

    @Test
    fun `newCapture and resolveCapture use the injected provider`() {
        val adapter = RevealJsEngineAdapter(settings(), plan(false), captureProvider = { FakeCapture() })

        assertThat(adapter.newCapture().name()).isEqualTo("fake-capture")
        assertThat(adapter.resolveCapture().name()).isEqualTo("fake-capture")
    }

    @Test
    fun `render whole source returns the produced capsule video`() {
        val adapter = RevealJsEngineAdapter(settings(), plan(false), captureProvider = { FakeCapture() })

        val result = adapter.render(request("out-whole"))

        assertThat(result.produced?.name).isEqualTo("capsule.webm")
        assertThat(result.failedUnits).isEmpty()
    }

    @Test
    fun `render parallel reports a failing unit without aborting the render`() {
        val adapter = RevealJsEngineAdapter(
            settings(),
            plan(true),
            captureProvider = { FakeCapture(failWhenPathContains = "slide-02") },
        )

        val result = adapter.render(request("out-parallel"))

        assertThat(result.failedUnits).containsExactly(2)
    }

    @Test
    fun `concat with no input returns false`() {
        assertThat(
            RevealJsEngineAdapter.concat(emptyList(), tempDir.resolve("out.webm").toFile())
        ).isFalse()
    }

    @Test
    fun `adapter name and availability are stable`() {
        val adapter = RevealJsEngineAdapter(settings(), plan(false), captureProvider = { FakeCapture() })

        assertThat(adapter.name()).isEqualTo("revealjs")
        assertThat(adapter.isAvailable()).isTrue()
    }
}
