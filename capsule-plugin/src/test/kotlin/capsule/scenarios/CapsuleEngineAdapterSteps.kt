package capsule.scenarios

import capsule.CaptureStrategy
import capsule.PlaywrightCapture
import capsule.adapter.revealjs.RevealJsCaptureSettings
import capsule.adapter.revealjs.RevealJsDeckHtml
import capsule.adapter.revealjs.RevealJsEngineAdapter
import capsule.adapter.revealjs.RevealJsRenderPlan
import video.kernel.FileVisualSource
import video.kernel.VideoRenderRequest
import video.kernel.VideoRenderResult
import video.kernel.segment.VideoSegment
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_engine_adapter.feature`
 * (CAP-ENGINE-DECOUPLING US-2 / video-kernel US-1, 0.0.2).
 *
 * Exercises [RevealJsDeckHtml] and [RevealJsEngineAdapter] directly with a fake
 * [PlaywrightCapture] (never a real browser). Step texts are prefixed with
 * "engine adapter"/"reveal deck" (S-088 — the glue is classpath-wide).
 */
class CapsuleEngineAdapterSteps {

    private var deckHtml: String = ""
    private var transformed: String = ""
    private var request: VideoRenderRequest? = null
    private var result: VideoRenderResult? = null
    private var fakeOutputName: String = "capsule.webm"
    private var fakeFailPath: String? = null
    private var segments: List<VideoSegment> = emptyList()
    private var parallel: Boolean = false
    private val workDir: File = Files.createTempDirectory("capsule-engine-adapter").toFile()

    private class FakeCapture(
        private val outputName: String,
        private val failWhenPathContains: String?,
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
        remotionProjectDir = File(workDir, "remotion"),
        remotionNodePath = "node",
        remotionConcurrency = 1,
        remotionFps = 30,
        manimSources = emptyList(),
    )

    @Given("a reveal deck with 3 slides")
    fun aRevealDeckWith3Slides() {
        deckHtml = """
            <html><head><style>.reveal{}</style></head><body><div class="reveal"><div class="slides">
            <section data-capsule-slide="1">
              <h1>One</h1>
            </section>
            <section data-capsule-slide="2">
              <h1>Two</h1>
            </section>
            <section data-capsule-slide="3">
              <h1>Three</h1>
            </section>
            </div></div></body></html>
        """.trimIndent()
    }

    @When("slide {int} of the deck is extracted as a standalone document")
    fun slideOfTheDeckIsExtractedAsAStandaloneDocument(slideNumber: Int) {
        transformed = RevealJsDeckHtml.singleSlideHtml(deckHtml, slideNumber - 1)
    }

    @Then("the standalone document contains {string}")
    fun theStandaloneDocumentContains(fragment: String) {
        assertTrue(transformed.contains(fragment), "expected standalone document to contain '$fragment'")
    }

    @Then("the standalone document does not contain {string}")
    fun theStandaloneDocumentDoesNotContain(fragment: String) {
        assertTrue(!transformed.contains(fragment), "expected standalone document to NOT contain '$fragment'")
    }

    @When("the audio file {string} is wired to section {int}")
    fun theAudioFileIsWiredToSection(audioPath: String, section: Int) {
        transformed = RevealJsDeckHtml.injectDataSlideAudio(deckHtml, mapOf(section to audioPath))
    }

    @Then("the deck carries data-audio {string} on section {int}")
    fun theDeckCarriesDataAudioOnSection(attribute: String, section: Int) {
        assertTrue(
            transformed.contains("""<section data-audio="$attribute" data-capsule-slide="$section">"""),
            "expected section $section to carry $attribute — got:\n$transformed",
        )
    }

    @Given("a reveal engine adapter backed by a fake capture writing {string}")
    fun aRevealEngineAdapterBackedByAFakeCaptureWriting(outputName: String) {
        fakeOutputName = outputName
        fakeFailPath = null
    }

    @Given("a reveal engine adapter backed by a fake capture failing on {string}")
    fun aRevealEngineAdapterBackedByAFakeCaptureFailingOn(pathFragment: String) {
        fakeOutputName = "capsule.webm"
        fakeFailPath = pathFragment
    }

    @Given("a render request for {string} in non-parallel mode")
    fun aRenderRequestForInNonParallelMode(name: String) {
        buildRequest(name, parallel = false, segmentCount = 2)
    }

    @Given("a render request for {string} in parallel mode with {int} segments")
    fun aRenderRequestForInParallelModeWithSegments(name: String, segmentCount: Int) {
        buildRequest(name, parallel = true, segmentCount = segmentCount)
    }

    private fun buildRequest(name: String, parallel: Boolean, segmentCount: Int) {
        this.parallel = parallel
        segments = (1..segmentCount).map { VideoSegment(index = it, title = "S$it", narration = "n$it.") }
        val deck = File(workDir, "deck.html").apply {
            writeText(
                """
                <html><head></head><body><div class="reveal"><div class="slides">
                <section data-capsule-slide="1">
                  <h1>One</h1>
                </section>
                <section data-capsule-slide="2">
                  <h1>Two</h1>
                </section>
                </div></div></body></html>
                """.trimIndent()
            )
        }
        request = VideoRenderRequest(
            name = name,
            visual = FileVisualSource(deck),
            outputDir = File(workDir, "out-$name-$parallel").apply { mkdirs() },
            timeoutMillis = 5_000,
        )
    }

    @When("the engine adapter renders the request")
    fun theEngineAdapterRendersTheRequest() {
        val plan = RevealJsRenderPlan(
            segments = segments,
            segmentDurations = segments.map { 1.0 },
            viewportWidth = 1920,
            viewportHeight = 1080,
            parallel = parallel,
            captureTimeoutMillis = 5_000,
        )
        val adapter = RevealJsEngineAdapter(
            settings(),
            plan,
            captureProvider = { FakeCapture(fakeOutputName, fakeFailPath) },
        )
        result = adapter.render(request!!)
    }

    @Then("the produced video is {string}")
    fun theProducedVideoIs(expected: String) {
        assertEquals(expected, result?.produced?.name)
    }

    @Then("no segment failed")
    fun noSegmentFailed() {
        assertTrue(result!!.failedUnits.isEmpty(), "expected no failed unit, got ${result!!.failedUnits}")
    }

    @Then("the failed segments are {string}")
    fun theFailedSegmentsAre(expected: String) {
        assertEquals(expected, result!!.failedUnits.joinToString(", "))
    }
}
