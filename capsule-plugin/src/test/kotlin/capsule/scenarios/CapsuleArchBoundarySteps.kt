package capsule.scenarios

import com.sun.net.httpserver.HttpServer
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import java.io.File
import java.net.InetSocketAddress

/**
 * BDD steps for the CAP-ARCH-6 content + video pipeline boundary
 * (`capsule_arch_boundary.feature`).
 *
 * Exercises the boundary between the CAP-ARCH LLM content layer
 * (`generateCapsuleContent`, koog pipeline, mock Ollama HTTP) and the CAP-29
 * video backend (`generateCapsuleVideoAllLanguages`, NoOp engines). The
 * 10-language translation coverage belongs to `multi_language_capsule.feature`
 * (CAP-29.5) — here a single language (French) proves the boundary without
 * redundancy.
 *
 * Self-contained [GradleRunner] usage (pattern `MultiLanguageCapsuleSteps`,
 * `CapsuleAiSteps`): each scenario builds its own isolated project, registers
 * a fake `translateDeck` task when the composite chain is exercised, and starts
 * a local mock Ollama HTTP server that alternates between the two LLM calls of
 * the koog pipeline (`propose-context` → content plan JSON, then
 * `generate-speaker-notes` → enriched AsciiDoc).
 *
 * Step phrases are scoped with "capsule arch boundary" / "the fr video" to
 * avoid duplicate definitions with [CapsuleAiSteps] / [MultiLanguageCapsuleSteps]
 * (bug S-088 DuplicateStepDefinitionException).
 */
class CapsuleArchBoundarySteps {

    private var projectDir: File? = null
    private var mockServer: HttpServer? = null
    private var buildOutput: String = ""

    private val validContentPlanJson = """
        {
          "deckName": "demo",
          "languageCode": "fr",
          "slideCount": 1,
          "speakerNotesFile": "demo-speaker-notes.adoc",
          "ttsScriptFile": "demo-script.txt"
        }
    """.trimIndent()

    private val enrichedSpeakerNotes = """
        == Introduction

        [NOTE.speaker]
        --
        Enriched French narration for the demo deck.
        --
    """.trimIndent()

    // ─── Given ─────────────────────────────────────────────────────

    @Given("a Capsule Arch Boundary Gradle project with a French source deck")
    fun aCapsuleArchBoundaryGradleProjectWithAFrenchSourceDeck() {
        setupProject(noOpEngines = false)
    }

    @Given("a Capsule Arch Boundary Gradle project with a French source deck and NoOp engines")
    fun aCapsuleArchBoundaryGradleProjectWithAFrenchSourceDeckAndNoOpEngines() {
        setupProject(noOpEngines = true)
    }

    @Given("a mock Ollama LLM server proposing a valid content plan then enriched speaker notes")
    fun aMockOllamaLlmServerProposingAValidContentPlanThenEnrichedSpeakerNotes() {
        // The koog pipeline issues two /api/chat calls in order:
        //   1. propose-context  → must return a valid content plan JSON
        //   2. generate-speaker-notes → must return enriched AsciiDoc
        // The mock alternates its response body on each call so both nodes
        // receive the payload they expect.
        val responses = listOf(validContentPlanJson, enrichedSpeakerNotes)
        val callCounter = intArrayOf(0)
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/chat") { exchange ->
            val index = callCounter[0]++ % responses.size
            val body = """
                {
                  "model": "smollm:135m",
                  "message": { "role": "assistant", "content": ${escapeJson(responses[index])} },
                  "done": true
                }
            """.trimIndent().toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.executor = null
        server.start()
        mockServer = server
    }

    @Given("an existing valid WebM artifact for language {string}")
    fun anExistingValidWebmArtifactForLanguage(lang: String) {
        val project = projectDir ?: error("No Gradle project set up")
        val capsulesDir = project.resolve("build/capsules")
        capsulesDir.mkdirs()
        val webm = capsulesDir.resolve("demo_${lang}.webm")
        // Generate a real 1-second WebM so ffprobe returns a positive duration
        // (the Economy-of-Ink skip rule requires probeDuration > 0). A bare
        // EBML header would probe at 0.0 and be re-rendered.
        val proc = ProcessBuilder(
            "ffmpeg", "-y", "-f", "lavfi", "-i", "anullsrc=r=8000:cl=mono",
            "-t", "1", "-c:v", "libvpx", webm.absolutePath,
        ).redirectErrorStream(true).start()
        proc.waitFor()
        Assertions.assertTrue(
            webm.exists() && webm.length() > 0,
            "Pre-existing valid WebM fixture for '$lang' could not be generated (ffmpeg exit=${proc.exitValue()})",
        )
    }

    // ─── When ──────────────────────────────────────────────────────

    @When("I list the Gradle tasks in group capsule")
    fun iListTheGradleTasksInGroupCapsule() {
        buildOutput = gradleRunner("tasks", "--all").build().output
    }

    @When("I run the capsule content generation task against the mock LLM")
    fun iRunTheCapsuleContentGenerationTaskAgainstTheMockLlm() {
        val port = mockServer!!.address.port
        buildOutput = gradleRunner(
            "generateCapsuleContent",
            "-Pollama.baseUrl=http://localhost:$port",
            "-Pdeck.file=${projectDir!!.resolve("slides/misc/demo.adoc").absolutePath}",
        ).build().output
    }

    @When("I run the capsule content and videos task against the mock LLM")
    fun iRunTheCapsuleContentAndVideosTaskAgainstTheMockLlm() {
        // The composite chain depends on slider's `translateDeck`, which is not
        // applied in this isolated project. A fake task is registered in the
        // build script (setupProject) so the dependency graph resolves. The
        // translated fr artifacts are pre-generated so the video pipeline finds
        // its `demo_fr-deck.html` + `demo_fr-script.txt` pair.
        preGenerateFrenchVideoArtifacts()
        val port = mockServer!!.address.port
        buildOutput = gradleRunner(
            "generateCapsuleContentAndVideos",
            "-Pollama.baseUrl=http://localhost:$port",
            "-Pdeck.file=${projectDir!!.resolve("slides/misc/demo.adoc").absolutePath}",
        ).build().output
    }

    // ─── Then ──────────────────────────────────────────────────────

    @Then("the capsule arch boundary task {string} is listed in the output")
    fun theCapsuleArchBoundaryTaskIsListedInTheOutput(taskName: String) {
        Assertions.assertTrue(
            buildOutput.contains(taskName),
            "Expected task '$taskName' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("the capsule arch boundary build succeeds")
    fun theCapsuleArchBoundaryBuildSucceeds() {
        // gradleRunner.build() above would have thrown on failure.
    }

    @Then("the capsule arch boundary build output logs {string}")
    fun theCapsuleArchBoundaryBuildOutputLogs(fragment: String) {
        Assertions.assertTrue(
            buildOutput.contains(fragment),
            "Expected '$fragment' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    @Then("a speaker notes file {string} is generated by the capsule arch boundary build")
    fun aSpeakerNotesFileIsGeneratedByTheCapsuleArchBoundaryBuild(fileName: String) {
        val project = projectDir ?: error("No Gradle project set up")
        val file = project.resolve("build/capsule/$fileName")
        Assertions.assertTrue(file.exists(), "Expected speaker notes at ${file.absolutePath}")
        Assertions.assertTrue(file.length() > 0, "Expected non-empty speaker notes at ${file.absolutePath}")
    }

    @Then("a TTS script file {string} is generated by the capsule arch boundary build")
    fun aTtsScriptFileIsGeneratedByTheCapsuleArchBoundaryBuild(fileName: String) {
        val project = projectDir ?: error("No Gradle project set up")
        val file = project.resolve("build/capsule/$fileName")
        Assertions.assertTrue(file.exists(), "Expected TTS script at ${file.absolutePath}")
        Assertions.assertTrue(file.length() > 0, "Expected non-empty TTS script at ${file.absolutePath}")
    }

    @Then("a video file {string} is generated by the capsule arch boundary video pipeline")
    fun aVideoFileIsGeneratedByTheCapsuleArchBoundaryVideoPipeline(fileName: String) {
        val project = projectDir ?: error("No Gradle project set up")
        val video = project.resolve("build/capsules/$fileName")
        Assertions.assertTrue(video.exists(), "Expected video at ${video.absolutePath}")
        Assertions.assertTrue(video.length() > 0, "Expected non-empty video at ${video.absolutePath}")
    }

    @Then("the build output reports the fr video is skipped as already rendered")
    fun theBuildOutputReportsTheFrVideoIsSkippedAsAlreadyRendered() {
        // CAPSULE MULTILANG → 0/1 languages rendered (1 skipped: existing valid WebM)
        Assertions.assertTrue(
            buildOutput.contains("CAPSULE MULTILANG") && buildOutput.contains("0/1 languages rendered"),
            "Expected 'CAPSULE MULTILANG → 0/1 languages rendered' in build output. Got: ${buildOutput.take(2000)}",
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private fun setupProject(noOpEngines: Boolean) {
        projectDir = File(System.getProperty("java.io.tmpdir"))
            .resolve("cucumber-arch-boundary-${System.currentTimeMillis()}")
            .also { it.mkdirs() }

        projectDir!!.resolve("settings.gradle").writeText("")
        val buildGradle = buildString {
            appendLine("plugins { id('education.cccp.capsule') }")
            appendLine("capsule {")
            if (noOpEngines) {
                appendLine("    ttsEngine = 'noop'")
                appendLine("    outputDir = 'capsules'")
            }
            appendLine("}")
            // The composite task graph depends on slider's translateDeck, which is
            // not applied in this isolated project. A fake task lets the graph
            // resolve during the composite run without pulling slider in.
            appendLine("tasks.register('translateDeck') { doLast { logger.lifecycle('FAKE translateDeck') } }")
        }
        projectDir!!.resolve("build.gradle").writeText(buildGradle)

        // generateCapsuleContent resolves its deckFile convention by scanning
        // slides/misc for the first .adoc file.
        val deckDir = projectDir!!.resolve("slides").resolve("misc")
        deckDir.mkdirs()
        deckDir.resolve("demo.adoc").writeText("""
            == Introduction

            [NOTE.speaker]
            --
            Base French narration.
            --
        """.trimIndent())
    }

    private fun preGenerateFrenchVideoArtifacts() {
        val project = projectDir ?: error("No Gradle project set up")
        // The video pipeline (generateCapsuleVideoAllLanguages) resolves its
        // deck dir via CapsuleManager.resolveDeckDir → build/docs/asciidocRevealJs
        // and its script dir via resolveScriptDir → build/capsule.
        val deckDir = project.resolve("build/docs/asciidocRevealJs").also { it.mkdirs() }
        val scriptDir = project.resolve("build/capsule").also { it.mkdirs() }
        project.resolve("build/capsules").also { it.mkdirs() }

        deckDir.resolve("demo_fr-deck.html").writeText("""
            <html><body>
            <div class="reveal">
              <div class="slides">
                <section data-capsule-slide="1"><h2>Introduction</h2></section>
              </div>
            </div>
            </body></html>
        """.trimIndent())

        scriptDir.resolve("demo_fr-script.txt").writeText("""
            === CAPSULE SCRIPT : demo_fr ===
            --- SLIDE 1 : Introduction ---
            Enriched French narration for the demo deck.
        """.trimIndent())
    }

    private fun gradleRunner(vararg args: String): GradleRunner =
        GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(*args)
            .withProjectDir(projectDir!!)

    private fun escapeJson(raw: String): String =
        "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private companion object {
        // Minimal WebM EBML header (pattern NoOpPlaywrightCapture.MINIMAL_WEBM)
        // so the Economy-of-Ink skip rule sees a non-empty, probe-valid artifact.
        private val MINIMAL_WEBM: ByteArray =
            "1a45dfa301000000000000001f4286810142f7810142f2810442f381084282847765626d42878104428581021853806701000000000000001e1549a96601000000000000000d2ad7b1830f4240448984000000000000000000"
                .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}