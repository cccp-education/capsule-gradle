package capsule.adapter.revealjs

import capsule.CaptureStrategy
import capsule.PlaywrightCapture
import capsule.kernel.FileVisualSource
import capsule.kernel.VideoRenderRequest
import capsule.kernel.VideoSegment
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * TDD — reveal.js engine adapter ([VideoEngine] implementation)
 * (CAP-ENGINE-DECOUPLING US-2).
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
        val adapter = RevealJsEngineAdapter(settings(), captureProvider = { FakeCapture() })

        assertThat(adapter.newCapture(emptyList()).name()).isEqualTo("fake-capture")
        assertThat(adapter.resolveCapture(emptyList()).name()).isEqualTo("fake-capture")
    }

    @Test
    fun `render whole source returns the produced capsule video`() {
        val outputDir = tempDir.resolve("out").toFile()
        val adapter = RevealJsEngineAdapter(settings(), captureProvider = { FakeCapture() })

        val result = adapter.render(
            VideoRenderRequest(
                name = "demo",
                visual = FileVisualSource(deckFile()),
                segments = segments,
                segmentDurations = listOf(1.0, 1.0),
                outputDir = outputDir,
                viewportWidth = 1920,
                viewportHeight = 1080,
                parallel = false,
                timeoutMillis = 5_000,
            )
        )

        assertThat(result.produced?.name).isEqualTo("capsule.webm")
        assertThat(result.failedSegments).isEmpty()
    }

    @Test
    fun `render parallel reports a failing segment without aborting the render`() {
        val outputDir = tempDir.resolve("out").toFile()
        val adapter = RevealJsEngineAdapter(
            settings(),
            captureProvider = { FakeCapture(failWhenPathContains = "slide-02") },
        )

        val result = adapter.render(
            VideoRenderRequest(
                name = "demo",
                visual = FileVisualSource(deckFile()),
                segments = segments,
                segmentDurations = listOf(1.0, 1.0),
                outputDir = outputDir,
                viewportWidth = 1920,
                viewportHeight = 1080,
                parallel = true,
                timeoutMillis = 5_000,
            )
        )

        assertThat(result.failedSegments).containsExactly(2)
    }

    @Test
    fun `concat with no input returns false`() {
        assertThat(
            RevealJsEngineAdapter.concat(emptyList(), tempDir.resolve("out.webm").toFile())
        ).isFalse()
    }

    @Test
    fun `adapter name and availability are stable`() {
        val adapter = RevealJsEngineAdapter(settings(), captureProvider = { FakeCapture() })

        assertThat(adapter.name()).isEqualTo("revealjs")
        assertThat(adapter.isAvailable()).isTrue()
    }
}
