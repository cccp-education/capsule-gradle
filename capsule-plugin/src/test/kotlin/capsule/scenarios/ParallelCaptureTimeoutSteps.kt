package capsule.scenarios

import capsule.CapsuleExtension
import capsule.CapsuleVideoTask
import capsule.PlaywrightCapture
import capsule.feed.CapsuleScript
import capsule.feed.SlideSegment
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Files

/**
 * Cucumber BDD — CR-2.6 parallel capture timeout.
 *
 * Black-box behaviour of CapsuleVideoTask.captureSlideParallel exercised from
 * the outside: a stuck capture must time out (not hang), a throwing capture
 * must be degraded (reported, never fatal for the deck).
 */
class ParallelCaptureTimeoutSteps {

    private var task: CapsuleVideoTask? = null
    private var script: CapsuleScript? = null
    private var deckFile: File? = null
    private var audioDir: File? = null
    private var outputDir: File? = null
    private var captureFactory: (() -> PlaywrightCapture)? = null
    private var timedOut = false
    private var failedCount: Int? = null

    @Given("a deck with {int} slides")
    fun aDeckWithSlides(slideCount: Int) {
        val project = ProjectBuilder.builder().build()
        val ext = CapsuleExtension(project.objects)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")
        ext.viewportWidth.set(1408)
        ext.viewportHeight.set(792)
        ext.slideDurationSeconds.set(5.0)
        ext.playwrightTimeout.set(120_000.0)

        val t = project.tasks.register("capsuleVideo", CapsuleVideoTask::class.java).get()
        t.capsuleExtension = ext
        task = t
        script = CapsuleScript(
            deckName = "parallel-deck",
            segments = (1..slideCount).map { SlideSegment(it, "Slide $it", "Note $it") }
        )

        val temp = Files.createTempDirectory("cucumber-parallel-timeout").toFile()
        temp.deleteOnExit()
        audioDir = temp.resolve("audio").also { it.mkdirs() }
        outputDir = temp.resolve("video").also { it.mkdirs() }
        deckFile = temp.resolve("deck.html").also {
            it.writeText(
                """
                <html><head><style>body{margin:0}</style></head><body>
                <div class="reveal"><div class="slides">
                ${(1..slideCount).joinToString("\n") { i -> """<section data-capsule-slide="$i"><h2>Slide $i</h2></section>""" }}
                </div></div>
                <script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
                <script>Reveal.initialize();</script>
                </body></html>
                """.trimIndent()
            )
        }
    }

    @Given("a capture factory that blocks indefinitely")
    fun aCaptureFactoryThatBlocksIndefinitely() {
        captureFactory = {
            object : PlaywrightCapture {
                override fun capture(
                    deckHtmlPath: String, outputDir: File, viewportWidth: Int,
                    viewportHeight: Int, slideDurations: List<Double>
                ) {
                    Thread.sleep(Long.MAX_VALUE)
                }
                override fun isAvailable() = true
                override fun name() = "blocking"
                override fun close() {}
            }
        }
    }

    @Given("a capture factory where slides 1 and 3 throw")
    fun aCaptureFactoryWhereSlidesOneAndThreeThrow() {
        captureFactory = {
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
    }

    @When("parallel capture runs with a timeout of {int} ms")
    fun parallelCaptureRunsWithTimeout(timeoutMillis: Int) {
        val t = task ?: error("No task set")
        val parsed = script ?: error("No script set")
        try {
            failedCount = t.captureSlideParallel(
                deckHtmlPath = deckFile!!.absolutePath,
                outputDir = outputDir!!,
                viewportWidth = 1408,
                viewportHeight = 792,
                parsed = parsed,
                audioDir = audioDir!!,
                captureFactory = captureFactory,
                captureTimeoutMillis = timeoutMillis.toLong()
            )
        } catch (e: Exception) {
            timedOut = true
        }
    }

    @Then("the capture aborts with a timeout")
    fun theCaptureAbortsWithATimeout() {
        Assertions.assertTrue(timedOut, "A blocking capture must abort with a timeout instead of hanging")
    }

    @Then("{int} slides are reported as failed")
    fun slidesAreReportedAsFailed(expected: Int) {
        Assertions.assertEquals(expected, failedCount, "Failed slide count mismatch")
    }

    @And("slide {int} webm is produced")
    fun slideWebmIsProduced(slideIndex: Int) {
        val file = outputDir!!.resolve("slide-%02d.webm".format(slideIndex))
        Assertions.assertTrue(file.exists(), "Slide $slideIndex webm should have been produced: ${file.absolutePath}")
    }
}
