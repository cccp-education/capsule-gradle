package capsule

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

interface PlaywrightCapture {
    fun capture(deckHtmlPath: String, outputDir: File, viewportWidth: Int, viewportHeight: Int, slideDurations: List<Double>)
    fun isAvailable(): Boolean
    fun name(): String
    fun close()
}

class PlaywrightCaptureImpl(
    private val timeout: Double = 120_000.0,
    private val transitionPause: Double = 500.0,
    private val endMargin: Double = 2000.0,
    private val defaultSlideDuration: Double = 5.0
) : PlaywrightCapture {

    private val logger = Logging.getLogger(PlaywrightCaptureImpl::class.java)

    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private var context: BrowserContext? = null
    private var page: Page? = null

    override fun isAvailable(): Boolean = try {
        Playwright.create().use { pw ->
            pw.chromium().launch(BrowserType.LaunchOptions().setHeadless(true)).use { it.close() }
        }
        true
    } catch (e: Exception) {
        false
    }

    override fun name(): String = "playwright-java"

    override fun capture(
        deckHtmlPath: String,
        outputDir: File,
        viewportWidth: Int,
        viewportHeight: Int,
        slideDurations: List<Double>
    ) {
        val slideCount = slideDurations.size
        playwright = Playwright.create()
        browser = playwright!!.chromium().launch(
            BrowserType.LaunchOptions().setHeadless(true)
        )
        context = browser!!.newContext(
            Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setRecordVideoDir(Paths.get(outputDir.absolutePath))
                .setRecordVideoSize(viewportWidth, viewportHeight)
        )
        page = context!!.newPage()
        page!!.setDefaultNavigationTimeout(timeout)
        page!!.setDefaultTimeout(timeout)

        val absolutePath = File(deckHtmlPath).absolutePath
        page!!.navigate("file://$absolutePath")

        page!!.waitForSelector(".reveal",
            Page.WaitForSelectorOptions().setTimeout(timeout))

        page!!.waitForTimeout(2000.0)

        val audioIds = page!!.evaluate("Array.from(document.querySelectorAll('audio')).map(a => a.id)") as? List<*> ?: listOf<Any>()
        val hasAudioElements = audioIds.isNotEmpty()

        logger.lifecycle("  Playwright: hasAudio={} audioCount={} slides={}", hasAudioElements, audioIds.size, slideCount)

        for (i in 0 until slideCount) {
            val slideMs = (slideDurations[i] * 1000).toLong()
            // Always use waitForTimeout: audio doesn't play in headless Chromium.
            // FFmpeg will mix the audio tracks onto the captured video later.
            page!!.waitForTimeout(slideMs.toDouble())
            page!!.waitForTimeout(transitionPause)
            if (i < slideCount - 1) {
                page!!.evaluate("typeof Reveal !== 'undefined' && Reveal.next()")
            }
        }

        page!!.waitForTimeout(endMargin)
    }

    override fun close() {
        context?.close()
        browser?.close()
        playwright?.close()
        context = null
        browser = null
        playwright = null
        page = null
    }
}

class NoOpPlaywrightCapture : PlaywrightCapture {
    override fun isAvailable(): Boolean = true
    override fun name(): String = "noop-playwright"

    override fun capture(
        deckHtmlPath: String,
        outputDir: File,
        viewportWidth: Int,
        viewportHeight: Int,
        slideDurations: List<Double>
    ) {
        outputDir.mkdirs()
        outputDir.resolve("capsule.webm").writeBytes(MINIMAL_WEBM)
    }

    override fun close() {}

    companion object {
        private val MINIMAL_WEBM: ByteArray = "1a45dfa301000000000000001f4286810142f7810142f2810442f381084282847765626d42878104428581021853806701000000000000001e1549a96601000000000000000d2ad7b1830f4240448984000000000000000000"
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}

class CapturingException(message: String) : RuntimeException(message)

/**
 * Screenshot-based capture: takes a PNG screenshot of each slide then uses FFmpeg
 * to produce a WebM of the exact audio duration per slide, and concatenates the
 * per-slide WebMs into a single `capsule.webm`. No real-time recording — orders
 * of magnitude faster and more reliable than Playwright video recording.
 *
 * Multi-slide support (CAP-CR3-3 US-3): iterates over [slideDurations], capturing
 * `slide-N.png` per slide, navigating with `Reveal.next()` between slides, then
 * converts each PNG to a `slide-N.webm` of the exact slide duration, and finally
 * concatenates all per-slide WebMs via the FFmpeg concat demuxer.
 */
class ScreenshotCaptureImpl(
    private val timeout: Double = 120_000.0
) : PlaywrightCapture {

    private val logger = Logging.getLogger(ScreenshotCaptureImpl::class.java)

    private var playwright: Playwright? = null
    private var browser: Browser? = null

    override fun isAvailable(): Boolean = try {
        Playwright.create().use { pw ->
            pw.chromium().launch(BrowserType.LaunchOptions().setHeadless(true)).use { it.close() }
        }
        true
    } catch (_: Exception) { false }

    override fun name(): String = "screenshot+ffmpeg"

    override fun capture(
        deckHtmlPath: String,
        outputDir: File,
        viewportWidth: Int,
        viewportHeight: Int,
        slideDurations: List<Double>
    ) {
        require(slideDurations.isNotEmpty()) { "slideDurations must not be empty" }
        val plan = ScreenshotPlanner.plan(outputDir, slideDurations)
        outputDir.mkdirs()

        val absolutePath = File(deckHtmlPath).absolutePath

        playwright = Playwright.create()
        browser = playwright!!.chromium().launch(BrowserType.LaunchOptions().setHeadless(true))

        val page = browser!!.newPage(
            Browser.NewPageOptions().setViewportSize(viewportWidth, viewportHeight)
        )
        page.setDefaultNavigationTimeout(timeout)
        page.setDefaultTimeout(timeout)
        page.navigate("file://$absolutePath")
        page.waitForSelector(".reveal, section, body",
            Page.WaitForSelectorOptions().setTimeout(timeout))
        page.waitForTimeout(800.0)

        for (entry in plan.slides) {
            page.screenshot(Page.ScreenshotOptions().setPath(entry.pngFile.toPath()))
            if (entry.index < plan.slides.lastIndex) {
                page.evaluate("typeof Reveal !== 'undefined' && Reveal.next()")
                page.waitForTimeout(300.0)
            }
        }
        page.close()

        for (entry in plan.slides) {
            val argv = ScreenshotPlanner.ffmpegPngToWebmArgs(entry, viewportWidth, viewportHeight)
            val proc = ProcessBuilder(argv).redirectErrorStream(true).start()
            val exitCode = proc.waitFor()
            if (exitCode != 0) {
                val err = proc.inputStream.bufferedReader().readText()
                throw CapturingException("FFmpeg PNG→WebM failed (slide ${entry.index}): $err")
            }
        }

        plan.concatListFile.writeText(ScreenshotPlanner.renderConcatList(plan))
        val concatArgv = ScreenshotPlanner.ffmpegConcatArgs(plan)
        val concatProc = ProcessBuilder(concatArgv).redirectErrorStream(true).start()
        val concatExit = concatProc.waitFor()
        if (concatExit != 0) {
            val err = concatProc.inputStream.bufferedReader().readText()
            throw CapturingException("FFmpeg concat failed: $err")
        }
        logger.lifecycle("  ScreenshotCapture: {} slides → {}", plan.size, plan.finalWebm.name)
    }

    override fun close() {
        browser?.close()
        playwright?.close()
        browser = null
        playwright = null
    }
}
