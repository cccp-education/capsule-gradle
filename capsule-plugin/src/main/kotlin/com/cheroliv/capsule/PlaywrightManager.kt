package com.cheroliv.capsule

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import java.io.File
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

        val absolutePath = File(deckHtmlPath).absolutePath
        page!!.navigate("file://$absolutePath")

        page!!.waitForSelector(".reveal",
            Page.WaitForSelectorOptions().setTimeout(timeout))

        page!!.waitForTimeout(2000.0)

        val audioIds = page!!.evaluate("Array.from(document.querySelectorAll('audio')).map(a => a.id)") as? List<*> ?: listOf<Any>()
        val hasAudioElements = audioIds.isNotEmpty()

        println("  Playwright: hasAudio=${hasAudioElements} audioCount=${audioIds.size} slides=$slideCount")

        for (i in 0 until slideCount) {
            val slideMs = (slideDurations[i] * 1000).toLong()
            if (hasAudioElements) {
                val raceTimeoutMs = slideMs + 2000
                page!!.waitForFunction(
                    "new Promise(r => { var a = document.getElementById('audio-$i'); if (a && !a.ended) { var t = setTimeout(r, $raceTimeoutMs); a.onended = r; } else { r(); } })",
                    null,
                    Page.WaitForFunctionOptions().setTimeout(timeout)
                )
            } else {
                page!!.waitForTimeout(slideMs.toDouble())
            }
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
        val placeholder = listOf(
            "# PLAYWRIGHT CAPTURE PLACEHOLDER (noop engine)",
            "# Deck: $deckHtmlPath",
            "# Slides: ${slideDurations.size}",
            "# Viewport: ${viewportWidth}x$viewportHeight"
        ).joinToString("\n")
        outputDir.resolve("capsule.webm").writeText(placeholder)
    }

    override fun close() {}
}

class CapturingException(message: String) : RuntimeException(message)
