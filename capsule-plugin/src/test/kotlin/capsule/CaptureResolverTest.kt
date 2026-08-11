package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TDD unit tests for [CaptureResolver] (CAP-CR3-3 US-2).
 *
 * [CaptureResolver] dispatches on [CaptureStrategy] to build the
 * appropriate [PlaywrightCapture]. Tests inject fakes via
 * [CaptureResolver.PlaywrightFactory] / [CaptureResolver.ScreenshotFactory]
 * to avoid Playwright/FFmpeg I/O.
 */
class CaptureResolverTest {

    private val noOp = NoOpPlaywrightCapture()

    private class FakeCapture(
        private val available: Boolean,
        private val engineName: String
    ) : PlaywrightCapture {
        override fun capture(deckHtmlPath: String, outputDir: java.io.File, viewportWidth: Int, viewportHeight: Int, slideDurations: List<Double>) {}
        override fun isAvailable(): Boolean = available
        override fun name(): String = engineName
        override fun close() {}
    }

    @Test
    fun `strategy PLAYWRIGHT returns PlaywrightCapture when available`() {
        val playwright = FakeCapture(available = true, engineName = "playwright-java")
        val resolved = CaptureResolver.resolve(
            strategy = CaptureStrategy.PLAYWRIGHT,
            strict = false,
            playwrightFactory = { playwright },
            screenshotFactory = { FakeCapture(true, "screenshot+ffmpeg") },
            noOpCapture = noOp
        )
        assertEquals("playwright-java", resolved.name())
    }

    @Test
    fun `strategy SCREENSHOT returns ScreenshotCapture when available`() {
        val screenshot = FakeCapture(available = true, engineName = "screenshot+ffmpeg")
        val resolved = CaptureResolver.resolve(
            strategy = CaptureStrategy.SCREENSHOT,
            strict = false,
            playwrightFactory = { FakeCapture(true, "playwright-java") },
            screenshotFactory = { screenshot },
            noOpCapture = noOp
        )
        assertEquals("screenshot+ffmpeg", resolved.name())
    }

    @Test
    fun `strategy SCREENSHOT unavailable and non-strict falls back to NoOp`() {
        val resolved = CaptureResolver.resolve(
            strategy = CaptureStrategy.SCREENSHOT,
            strict = false,
            playwrightFactory = { FakeCapture(true, "playwright-java") },
            screenshotFactory = { FakeCapture(false, "screenshot+ffmpeg") },
            noOpCapture = noOp
        )
        assertEquals("noop-playwright", resolved.name())
    }

    @Test
    fun `strategy PLAYWRIGHT unavailable and non-strict falls back to NoOp`() {
        val resolved = CaptureResolver.resolve(
            strategy = CaptureStrategy.PLAYWRIGHT,
            strict = false,
            playwrightFactory = { FakeCapture(false, "playwright-java") },
            screenshotFactory = { FakeCapture(true, "screenshot+ffmpeg") },
            noOpCapture = noOp
        )
        assertEquals("noop-playwright", resolved.name())
    }

    @Test
    fun `strategy SCREENSHOT unavailable and strict throws IllegalStateException`() {
        val ex = assertFailsWith<IllegalStateException> {
            CaptureResolver.resolve(
                strategy = CaptureStrategy.SCREENSHOT,
                strict = true,
                playwrightFactory = { FakeCapture(true, "playwright-java") },
                screenshotFactory = { FakeCapture(false, "screenshot+ffmpeg") },
                noOpCapture = noOp,
                enginePath = "/usr/bin/ffmpeg"
            )
        }
        assertTrue(ex.message!!.contains("screenshot"), "error should name the engine")
        assertTrue(ex.message!!.contains("/usr/bin/ffmpeg"), "error should mention the path")
        assertTrue(ex.message!!.contains("strictMode"), "error should suggest disabling strictMode")
    }

    @Test
    fun `strategy PLAYWRIGHT unavailable and strict throws IllegalStateException`() {
        val ex = assertFailsWith<IllegalStateException> {
            CaptureResolver.resolve(
                strategy = CaptureStrategy.PLAYWRIGHT,
                strict = true,
                playwrightFactory = { FakeCapture(false, "playwright-java") },
                screenshotFactory = { FakeCapture(true, "screenshot+ffmpeg") },
                noOpCapture = noOp,
                enginePath = "/usr/bin/chromium"
            )
        }
        assertTrue(ex.message!!.contains("playwright"), "error should name the engine")
        assertTrue(ex.message!!.contains("/usr/bin/chromium"), "error should mention the path")
    }
}