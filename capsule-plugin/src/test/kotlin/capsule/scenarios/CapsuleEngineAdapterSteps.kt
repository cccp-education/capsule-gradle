package capsule.scenarios

import capsule.CaptureStrategy
import capsule.PlaywrightCapture
import capsule.adapter.revealjs.RevealJsCaptureSettings
import capsule.adapter.revealjs.RevealJsDeckHtml
import capsule.adapter.revealjs.RevealJsEngineAdapter
import capsule.kernel.FileVisualSource
import capsule.kernel.VideoRenderRequest
import capsule.kernel.VideoRenderResult
import capsule.kernel.VideoSegment
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_engine_adapter.feature`
 * (CAP-ENGINE-DECOUPLING US-2).
 *
 * Exercises [RevealJsDeckHtml] and [RevealJsEngineAdapter] directly with a fake
 * [PlaywrightCapture] (never a real browser). Step texts are prefixed with
 * "engine adapter"/"reveal deck" (S-088 — the glue is classpath-wide).
 */
class CapsuleEngineAdapterSteps {

    private var deckHtml: String = ""
    private var transformed: String = ""
    private var adapter: RevealJsEngineAdapter? = null
    private var request: VideoRenderRequest? = null
    private var result: VideoRenderResult? = null
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
            <section data-capsule-slide="1"><h1>One</h1></section>
            <section data-capsule-slide="2"><h1>Two</h1></section>
            <section data-capsule-slide="3"><h1>Three</h1></section>
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
        adapter = RevealJsEngineAdapter(settings(), captureProvider = { FakeCapture(outputName, null) })
    }

    @Given("a reveal engine adapter backed by a fake capture failing on {string}")
    fun aRevealEngineAdapterBackedByAFakeCaptureFailingOn(pathFragment: String) {
        adapter = RevealJsEngineAdapter(settings(), captureProvider = { FakeCapture("capsule.webm", pathFragment) })
    }

    @Given("a render request for {string} in non-parallel mode")
    fun aRenderRequestForInNonParallelMode(name: String) {
        request = buildRequest(name, parallel = false, segmentCount = 2)
    }

    @Given("a render request for {string} in parallel mode with {int} segments")
    fun aRenderRequestForInParallelModeWithSegments(name: String, segmentCount: Int) {
        request = buildRequest(name, parallel = true, segmentCount = segmentCount)
    }

    private fun buildRequest(name: String, parallel: Boolean, segmentCount: Int): VideoRenderRequest {
        val deck = File(workDir, "deck.html").apply {
            writeText(
                """
                <html><head></head><body><div class="reveal"><div class="slides">
                <section data-capsule-slide="1"><h1>One</h1></section>
                <section data-capsule-slide="2"><h1>Two</h1></section>
                </div></div></body></html>
                """.trimIndent()
            )
        }
        val segments = (1..segmentCount).map { VideoSegment(index = it, title = "S$it", narration = "n$it.") }
        return VideoRenderRequest(
            name = name,
            visual = FileVisualSource(deck),
            segments = segments,
            slideDurations = segments.map { 1.0 },
            outputDir = File(workDir, "out-$name-$parallel").apply { mkdirs() },
            viewportWidth = 1920,
            viewportHeight = 1080,
            parallel = parallel,
            timeoutMillis = 5_000,
        )
    }

    @When("the engine adapter renders the request")
    fun theEngineAdapterRendersTheRequest() {
        result = adapter!!.render(request!!)
    }

    @Then("the produced video is {string}")
    fun theProducedVideoIs(expected: String) {
        assertEquals(expected, result?.produced?.name)
    }

    @Then("no segment failed")
    fun noSegmentFailed() {
        assertTrue(result!!.failedSegments.isEmpty(), "expected no failed segment, got ${result!!.failedSegments}")
    }

    @Then("the failed segments are {string}")
    fun theFailedSegmentsAre(expected: String) {
        assertEquals(expected, result!!.failedSegments.joinToString(", "))
    }
}
