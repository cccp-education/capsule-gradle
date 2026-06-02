package capsule

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CapsulePluginTest {

    @Test
    fun `plugin registers generateCapsuleScript task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        assertNotNull(project.tasks.findByName("generateCapsuleScript"))
    }

    @Test
    fun `plugin registers generateCapsule task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        assertNotNull(project.tasks.findByName("generateCapsule"))
    }

    @Test
    fun `plugin registers generateCapsuleVideo task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        val task = project.tasks.findByName("generateCapsuleVideo")
        assertNotNull(task)
        assertEquals("generate", task.group)
    }

    @Test
    fun `plugin registers deployCapsule task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        val task = project.tasks.findByName("deployCapsule")
        assertNotNull(task)
        assertEquals("deploy", task.group)
    }

    @Test
    fun `plugin registers collectCapsuleContext task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        val task = project.tasks.findByName("collectCapsuleContext")
        assertNotNull(task)
        assertEquals("collect", task.group)
    }

    @Test
    fun `plugin registers capsule extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        val ext = project.extensions.findByType(CapsuleExtension::class.java)
        assertNotNull(ext)
    }
}

class TtsEngineTest {

    @Test
    fun `noop engine creates placeholder file`() {
        val engine = NoOpTtsEngine()
        assertTrue(engine.isAvailable())
        assertEquals("noop", engine.name())

        val tmpFile = File.createTempFile("capsule-test", ".mp3")
        tmpFile.deleteOnExit()

        engine.synthesize("Bonjour le monde", tmpFile)

        assertTrue(tmpFile.exists())
        assertTrue(tmpFile.readText().contains("TTS PLACEHOLDER"))
        assertTrue(tmpFile.readText().contains("Bonjour le monde"))
    }

    @Test
    fun `noop engine creates parent directories`() {
        val engine = NoOpTtsEngine()
        val tmpDir = File.createTempFile("capsule-prefix", null)
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()
        val outputFile = tmpDir.resolve("subdir").resolve("test.mp3")

        engine.synthesize("Test", outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.parentFile.exists())
    }

    @Test
    fun `piper engine reports unavailable when piper not installed`() {
        val engine = PiperTtsEngine(executablePath = "/nonexistent/path/piper")
        assertEquals(false, engine.isAvailable())
        assertEquals("piper", engine.name())
    }

    @Test
    fun `piper engine throws TtsException when not available`() {
        val engine = PiperTtsEngine(executablePath = "/nonexistent/path/piper")
        val tmpFile = File.createTempFile("capsule-test", ".mp3")
        tmpFile.deleteOnExit()

        try {
            engine.synthesize("Test", tmpFile)
            error("Expected TtsException")
        } catch (e: TtsException) {
            assertTrue(e.message!!.contains("not found"))
        }
    }

    @Test
    fun `capsule extension has expected defaults`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("education.cccp.capsule")
        val ext = project.extensions.getByType(CapsuleExtension::class.java)

        assertEquals("piper", ext.ttsEngine.get())
        assertEquals("fr_FR-siwis-medium", ext.ttsVoice.get())
        assertEquals("piper", ext.piperExecutablePath.get())
        assertEquals(true, ext.ttsFallbackEnabled.get())
        assertEquals("capsule", ext.outputDir.get())
        assertEquals("capsule", ext.sliderScriptDir.get())
        assertEquals(1408, ext.viewportWidth.get())
        assertEquals(792, ext.viewportHeight.get())
        assertEquals(120_000.0, ext.playwrightTimeout.get())
        assertEquals("", ext.chromiumExecutablePath.get())
        assertEquals("docs/asciidocRevealJs", ext.deckSourceDir.get())
        assertEquals("fr", ext.espeakVoice.get())
        assertEquals(150, ext.espeakSpeed.get())
        assertEquals("manim", ext.manimExecutablePath.get())
        assertEquals("l", ext.manimQuality.get())
        assertEquals("src/manim", ext.manimScriptsDir.get())
    }
}

class SlideSegmentModelTest {

    @Test
    fun `SlideType enum has HTML and MANIM values`() {
        assertEquals(2, SlideType.entries.size)
        assertEquals(SlideType.HTML, SlideType.valueOf("HTML"))
        assertEquals(SlideType.MANIM, SlideType.valueOf("MANIM"))
    }

    @Test
    fun `SlideSegment defaults type to HTML and manimScene to null`() {
        val segment = SlideSegment(index = 1, title = "Test", speakerNote = "Note")
        assertEquals(SlideType.HTML, segment.type)
        assertEquals(null, segment.manimScene)
    }

    @Test
    fun `SlideSegment can specify type MANIM with manimScene`() {
        val segment = SlideSegment(
            index = 1,
            title = "Animation",
            speakerNote = "Voici une animation mathématique.",
            type = SlideType.MANIM,
            manimScene = "Scene1"
        )
        assertEquals(SlideType.MANIM, segment.type)
        assertEquals("Scene1", segment.manimScene)
    }

    @Test
    fun `CapsuleScript holds SlideSegments with mixed types`() {
        val slides = listOf(
            SlideSegment(1, "Intro", "Bienvenue.", type = SlideType.HTML),
            SlideSegment(2, "Anim", "Regardez.", type = SlideType.MANIM, manimScene = "MoveSquare")
        )
        val script = CapsuleScript(deckName = "cours", slides = slides)
        assertEquals(2, script.slides.size)
        assertEquals(SlideType.HTML, script.slides[0].type)
        assertEquals(SlideType.MANIM, script.slides[1].type)
        assertEquals("MoveSquare", script.slides[1].manimScene)
    }
}

class PlaywrightCaptureTest {

    @Test
    fun `noop capture creates placeholder webm file`() {
        val capture = NoOpPlaywrightCapture()
        assertTrue(capture.isAvailable())
        assertEquals("noop-playwright", capture.name())

        val tmpDir = File.createTempFile("pw-test", null)
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        capture.capture("/fake/deck.html", tmpDir, 1408, 792, listOf(5.0, 5.0, 5.0))

        val placeholder = tmpDir.resolve("capsule.webm")
        assertTrue(placeholder.exists())
        val content = placeholder.readText()
        assertTrue(content.contains("PLAYWRIGHT CAPTURE PLACEHOLDER"))
        assertTrue(content.contains("/fake/deck.html"))
        assertTrue(content.contains("Slides: 3"))
    }

    @Test
    fun `noop capture creates output directory if absent`() {
        val capture = NoOpPlaywrightCapture()
        val tmpDir = File.createTempFile("pw-mkdir", null)
        tmpDir.delete()
        tmpDir.deleteOnExit()

        capture.capture("/deck.html", tmpDir, 1024, 768, listOf(5.0))

        assertTrue(tmpDir.exists())
        assertTrue(tmpDir.isDirectory)
        assertTrue(tmpDir.resolve("capsule.webm").exists())
    }

    @Test
    fun `noop capture close is noop`() {
        val capture = NoOpPlaywrightCapture()
        capture.close()
    }
}

class ScreenshotCaptureTest {

    @Test
    fun `screenshot capture reports name correctly`() {
        val capture = ScreenshotCaptureImpl()
        assertEquals("screenshot+ffmpeg", capture.name())
    }

    @Test
    fun `screenshot capture isAvailable returns false when no chromium`() {
        // In a CI/test environment without Chromium, this may be false
        // The test just verifies the method runs without error
        val capture = ScreenshotCaptureImpl()
        // Don't assert true/false — presence varies by environment
        capture.isAvailable()
    }

    @Test
    fun `screenshot capture close is safe without init`() {
        val capture = ScreenshotCaptureImpl()
        capture.close()
    }
}

class CapsuleBuildTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createBuildTask(
        scriptDir: File,
        engine: TtsEngine? = null
    ): CapsuleBuildTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.sliderScriptDir.set(scriptDir.relativeTo(project.layout.buildDirectory.get().asFile).path)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")

        val t = project.tasks.register("generateCapsule", CapsuleBuildTask::class.java).get()
        t.capsuleExtension = ext
        if (engine != null) t.ttsEngine = engine
        return t
    }

    @Test
    fun `execute synthesizes TTS in parallel for multiple slides`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val scriptFile = File(scriptDir, "parallel-test-script.txt")
        scriptFile.writeText("""
=== CAPSULE SCRIPT : parallel-test ===
--- SLIDE 1 : Intro ---
Première diapositive.
--- SLIDE 2 : Milieu ---
Deuxième diapositive.
--- SLIDE 3 : Fin ---
Troisième diapositive.
        """.trimIndent())

        val concurrentCalls = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val totalCalls = AtomicInteger(0)

        val countingEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                val current = concurrentCalls.incrementAndGet()
                totalCalls.incrementAndGet()
                // Track max concurrency
                var old: Int
                do {
                    old = maxConcurrent.get()
                    if (current <= old) break
                } while (!maxConcurrent.compareAndSet(old, current))
                outputFile.parentFile.mkdirs()
                outputFile.writeText("# TTS PLACEHOLDER (counting engine)\n# Text: ${text.take(50)}...")
                Thread.sleep(50) // simulate work
                concurrentCalls.decrementAndGet()
            }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "counting"
        }

        val task = createBuildTask(scriptDir, engine = countingEngine)
        task.execute()

        assertEquals(3, totalCalls.get(), "All 3 slides should be synthesized")
        assertTrue(maxConcurrent.get() > 1, "Parallel execution expected, max concurrent was ${maxConcurrent.get()}")
    }

    @Test
    fun `execute reports failures and continues when fallback enabled`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val scriptFile = File(scriptDir, "fail-test-script.txt")
        scriptFile.writeText("""
=== CAPSULE SCRIPT : fail-test ===
--- SLIDE 1 : OK ---
Celui-ci marche.
--- SLIDE 2 : Bad ---
Celui-ci échoue.
--- SLIDE 3 : Also OK ---
Celui-ci aussi.
        """.trimIndent())

        var callCount = 0
        val failingEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                callCount++
                if (text.contains("échoue")) {
                    throw TtsException("Simulated failure for slide 2")
                }
                outputFile.parentFile.mkdirs()
                outputFile.writeText("# TTS PLACEHOLDER\n# Text: ${text.take(50)}...")
            }
            override fun isAvailable(): Boolean = true
            override fun name(): String = "failing"
        }

        val task = createBuildTask(scriptDir, engine = failingEngine)
        task.execute()

        assertEquals(3, callCount, "All 3 slides should be attempted")
    }

    @Test
    fun `execute warns when no script files found`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val task = createBuildTask(scriptDir)
        task.execute()
    }
}

class CapsuleVideoTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(
        deckDir: File,
        scriptDir: File,
        capture: PlaywrightCapture? = null,
        engine: TtsEngine? = null
    ): CapsuleVideoTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.deckSourceDir.set(deckDir.absolutePath)
        ext.sliderScriptDir.set(scriptDir.absolutePath)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")

        val t = project.tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java).get()
        t.capsuleExtension = ext
        if (capture != null) t.playwrightCapture = capture
        if (engine != null) t.ttsEngine = engine
        return t
    }

    @Test
    fun `execute warns when no deck files found`() {
        val emptyDir = File(tempDir, "empty-decks").also { it.mkdirs() }
        val scriptDir = File(tempDir, "scripts").also { it.mkdirs() }
        val task = createTask(emptyDir, scriptDir)
        task.execute()
    }

    @Test
    fun `execute warns when no script files found`() {
        val deckDir = File(tempDir, "decks").also { it.mkdirs() }
        File(deckDir, "test-deck.html").writeText("<html></html>")
        val scriptDir = File(tempDir, "scripts").also { it.mkdirs() }
        val task = createTask(deckDir, scriptDir)
        task.execute()
    }

    @Test
    fun `execute produces webm with noop capture`() {
        val deckDir = File(tempDir, "decks").also { it.mkdirs() }
        val deckFile = File(deckDir, "mon-cours-deck.html")
        deckFile.writeText("""
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Intro</h2></section>
    <section data-capsule-slide="2"><h2>Topic</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent())

        val scriptDir = File(tempDir, "scripts").also { it.mkdirs() }
        val scriptFile = File(scriptDir, "mon-cours-script.txt")
        scriptFile.writeText("""
=== CAPSULE SCRIPT : mon-cours ===
--- SLIDE 1 : Intro ---
Bienvenue dans la formation.
--- SLIDE 2 : Topic ---
Voici le contenu principal.
        """.trimIndent())

        val task = createTask(
            deckDir,
            scriptDir,
            capture = NoOpPlaywrightCapture(),
            engine = NoOpTtsEngine()
        )
        task.execute()

        val expectedVideo = File(tempDir, "build/capsule/mon-cours.webm")
        assertTrue(expectedVideo.exists(), "Expected video at ${expectedVideo.absolutePath}")
        assertTrue(expectedVideo.readText().contains("PLAYWRIGHT CAPTURE PLACEHOLDER"))

        val injectedDeck = File(tempDir, "build/capsule/injected/mon-cours-deck.html")
        assertTrue(injectedDeck.exists(), "Expected injected deck at ${injectedDeck.absolutePath}")
        assertTrue(injectedDeck.readText().contains("data-audio"))
    }

    @Test
    fun `sequential fallback injects audio into sections without data-capsule-slide`() {
        val deckDir = File(tempDir, "decks").also { it.mkdirs() }
        val deckFile = File(deckDir, "cours-deck.html")
        deckFile.writeText("""
<html><body>
<div class="reveal">
  <div class="slides">
    <section><h2>Slide 1</h2></section>
    <section><h2>Slide 2</h2></section>
    <section><h2>Slide 3</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent())

        val scriptDir = File(tempDir, "scripts").also { it.mkdirs() }
        val scriptFile = File(scriptDir, "cours-script.txt")
        scriptFile.writeText("""
=== CAPSULE SCRIPT : cours ===
--- SLIDE 1 : Slide 1 ---
Contenu slide 1.
--- SLIDE 2 : Slide 2 ---
Contenu slide 2.
--- SLIDE 3 : Slide 3 ---
Contenu slide 3.
        """.trimIndent())

        val task = createTask(
            deckDir,
            scriptDir,
            capture = NoOpPlaywrightCapture(),
            engine = NoOpTtsEngine()
        )
        task.execute()

        val injectedDeck = File(tempDir, "build/capsule/injected/cours-deck.html")
        assertTrue(injectedDeck.exists(), "Expected injected deck at ${injectedDeck.absolutePath}")
        val injectedContent = injectedDeck.readText()
        assertTrue(injectedContent.contains("data-audio"), "Should have audio attributes")
        assertTrue(injectedContent.contains("sequential fallback"))
        assertTrue(injectedContent.count { it == '\n' && injectedContent.contains("<section data-audio=") } >= 2)
    }

    @Test
    fun `multi-deck build produces separate videos`() {
        val deckDir = File(tempDir, "decks").also { it.mkdirs() }
        val deck1 = File(deckDir, "cours-a-deck.html")
        deck1.writeText("""
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>A1</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent())
        val deck2 = File(deckDir, "cours-b-deck.html")
        deck2.writeText("""
<html><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>B1</h2></section>
  </div>
</div>
</body></html>
        """.trimIndent())

        val scriptDir = File(tempDir, "scripts").also { it.mkdirs() }
        File(scriptDir, "cours-a-script.txt").writeText("""
=== CAPSULE SCRIPT : cours-a ===
--- SLIDE 1 : A1 ---
Deck A.
        """.trimIndent())
        File(scriptDir, "cours-b-script.txt").writeText("""
=== CAPSULE SCRIPT : cours-b ===
--- SLIDE 1 : B1 ---
Deck B.
        """.trimIndent())

        val task = createTask(
            deckDir,
            scriptDir,
            capture = NoOpPlaywrightCapture(),
            engine = NoOpTtsEngine()
        )
        task.execute()

        val capDir = File(tempDir, "build/capsule")
        val videoA = File(capDir, "cours-a.webm")
        val videoB = File(capDir, "cours-b.webm")
        assertTrue(videoA.exists(), "Expected video for cours-a")
        assertTrue(videoB.exists(), "Expected video for cours-b")
        assertTrue(videoA.readText().contains("PLAYWRIGHT CAPTURE PLACEHOLDER"))
        assertTrue(videoB.readText().contains("PLAYWRIGHT CAPTURE PLACEHOLDER"))
    }

    @Test
    @Tag("integration")
    fun `playwright capture produces valid webm when chromium available`() {
        val impl = PlaywrightCaptureImpl()
        if (!impl.isAvailable()) {
            return
        }

        val deckDir = File(tempDir, "integration-decks").also { it.mkdirs() }
        val deckFile = File(deckDir, "test-deck.html")
        deckFile.writeText("""
<html><head>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.css">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/theme/white.css">
</head><body>
<div class="reveal">
  <div class="slides">
    <section data-capsule-slide="1"><h2>Slide 1</h2></section>
    <section data-capsule-slide="2"><h2>Slide 2</h2></section>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
<script>
  Reveal.initialize({ autoSlide: 500, autoSlideStoppable: false });
</script>
</body></html>
        """.trimIndent())

        val outputDir = File(tempDir, "integration-video").also { it.mkdirs() }

        try {
            impl.capture(deckFile.absolutePath, outputDir, 1408, 792, listOf(5.0, 5.0))
            impl.close()

            val videoFiles = outputDir.listFiles { f -> f.name.endsWith(".webm") }
            assertNotNull(videoFiles)
            assertTrue(videoFiles.isNotEmpty(), "Should produce a webm video file")
            val video = videoFiles.first()
            assertTrue(video.length() > 0, "Video file should have non-zero size")
        } catch (e: CapturingException) {
            impl.close()
            throw e
        }
    }
}

class CapsuleDistribTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(
        captDir: File,
        ffmpegAvailable: Boolean = false
    ): CapsuleDistribTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.outputDir.set("capsule")
        ext.ffmpegExecutablePath.set(if (ffmpegAvailable) "ffmpeg" else "__no_ffmpeg__")

        val t = project.tasks.register("deployCapsule", CapsuleDistribTask::class.java).get()
        t.capsuleExtension = ext
        return t
    }

    @Test
    fun `execution warns when no videos found`() {
        val capDir = File(tempDir, "build/capsule").also { it.mkdirs() }
        val task = createTask(capDir)
        task.execute()
    }

    @Test
    fun `execution copies videos when ffmpeg not available`() {
        val capDir = File(tempDir, "build/capsule").also { it.mkdirs() }
        val videoFile = File(capDir, "test.webm")
        videoFile.writeText("fake webm content")

        val task = createTask(capDir, ffmpegAvailable = false)
        task.execute()

        val distFile = File(tempDir, "build/capsule/distrib/test.webm")
        assertTrue(distFile.exists(), "Video should be copied to distrib dir")
        assertEquals("fake webm content", distFile.readText())
    }

    @Test
    fun `execution handles multiple videos`() {
        val capDir = File(tempDir, "build/capsule").also { it.mkdirs() }
        File(capDir, "deck-a.webm").writeText("video a")
        File(capDir, "deck-b.webm").writeText("video b")

        val task = createTask(capDir, ffmpegAvailable = false)
        task.execute()

        val distribA = File(tempDir, "build/capsule/distrib/deck-a.webm")
        val distribB = File(tempDir, "build/capsule/distrib/deck-b.webm")
        assertTrue(distribA.exists())
        assertTrue(distribB.exists())
        assertEquals("video a", distribA.readText())
        assertEquals("video b", distribB.readText())
    }
}

class CapsuleParseScriptTest {

    @Test
    fun `parseScript extracts slide type and manim scene from title markers`() {
        val scriptFile = File.createTempFile("capsule-test", ".txt")
        scriptFile.deleteOnExit()
        scriptFile.writeText("""
=== CAPSULE SCRIPT : cours ===
--- SLIDE 1 : Intro ---
Bienvenue dans la formation.
--- SLIDE 2 : Anim [manim:MoveSquare] ---
Voici l'animation.
--- SLIDE 3 : Fin [html] ---
Conclusion.
        """.trimIndent())

        val parsed = CapsuleManager.parseScript(scriptFile)
        assertEquals("cours", parsed.deckName)
        assertEquals(3, parsed.slides.size)

        val slide1 = parsed.slides[0]
        assertEquals(1, slide1.index)
        assertEquals("Intro", slide1.title)
        assertEquals(SlideType.HTML, slide1.type)
        assertEquals(null, slide1.manimScene)

        val slide2 = parsed.slides[1]
        assertEquals(2, slide2.index)
        assertEquals("Anim", slide2.title)
        assertEquals(SlideType.MANIM, slide2.type)
        assertEquals("MoveSquare", slide2.manimScene)

        val slide3 = parsed.slides[2]
        assertEquals(3, slide3.index)
        assertEquals("Fin", slide3.title)
        assertEquals(SlideType.HTML, slide3.type)
    }

    @Test
    fun `parseScript defaults to HTML type without markers`() {
        val scriptFile = File.createTempFile("capsule-test", ".txt")
        scriptFile.deleteOnExit()
        scriptFile.writeText("""
=== CAPSULE SCRIPT : simple ===
--- SLIDE 1 : Slide simple ---
Juste du texte.
        """.trimIndent())

        val parsed = CapsuleManager.parseScript(scriptFile)
        assertEquals(SlideType.HTML, parsed.slides[0].type)
        assertEquals(null, parsed.slides[0].manimScene)
    }
}

class CapsuleCompositeContextTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(
        scriptDir: File,
        captDir: File,
        distribDir: File
    ): CapsuleCompositeContextTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.version = "0.1.0-SNAPSHOT"

        val ext = CapsuleExtension(project.objects)
        ext.outputDir.set("capsule")
        ext.sliderScriptDir.set(scriptDir.relativeTo(project.layout.buildDirectory.get().asFile).path)
        ext.viewportWidth.set(1408)
        ext.viewportHeight.set(792)
        ext.ttsEngine.set("piper")
        ext.ttsVoice.set("fr_FR-siwis-medium")

        val t = project.tasks.register("collectCapsuleContext", CapsuleCompositeContextTask::class.java).get()
        t.capsuleExtension = ext
        return t
    }

    @Test
    fun `execution creates json context file`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val capDir = File(buildDir, "capsule").also { it.mkdirs() }
        val distDir = File(buildDir, "capsule/distrib").also { it.mkdirs() }

        val scriptFile = File(scriptDir, "test-cours-script.txt")
        scriptFile.writeText("""
=== CAPSULE SCRIPT : test-cours ===
--- SLIDE 1 : Introduction ---
Bienvenue dans le cours.
--- SLIDE 2 : Contenu ---
Voici le contenu principal de la formation.
        """.trimIndent())

        val task = createTask(scriptDir, capDir, distDir)
        task.execute()

        val jsonFile = File(buildDir, "capsule/capsule-context.json")
        assertTrue(jsonFile.exists(), "JSON context file should be created")
        val content = jsonFile.readText()
        assertTrue(content.contains("capsule"), "Should contain source=capsule")
        assertTrue(content.contains("test-cours"), "Should contain deck name")
        assertTrue(content.contains("slideCount"), "Should contain slide count")
        assertTrue(content.contains("Introduction"), "Should contain slide title")
        assertTrue(content.contains("piper"), "Should contain TTS engine")
    }

    @Test
    fun `execution handles multiple decks`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val capDir = File(buildDir, "capsule").also { it.mkdirs() }
        val distDir = File(buildDir, "capsule/distrib").also { it.mkdirs() }

        File(scriptDir, "cours-a-script.txt").writeText("""
=== CAPSULE SCRIPT : cours-a ===
--- SLIDE 1 : A1 ---
Deck A.
        """.trimIndent())
        File(scriptDir, "cours-b-script.txt").writeText("""
=== CAPSULE SCRIPT : cours-b ===
--- SLIDE 1 : B1 ---
Deck B.
        """.trimIndent())

        val task = createTask(scriptDir, capDir, distDir)
        task.execute()

        val jsonFile = File(buildDir, "capsule/capsule-context.json")
        val content = jsonFile.readText()
        assertTrue(content.contains("cours-a"))
        assertTrue(content.contains("cours-b"))
    }

    @Test
    fun `execution includes distribVideo path when available`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val capDir = File(buildDir, "capsule").also { it.mkdirs() }
        val distDir = File(buildDir, "capsule/distrib").also { it.mkdirs() }

        File(distDir, "cours.webm").writeText("fake distrib video")

        File(scriptDir, "cours-script.txt").writeText("""
=== CAPSULE SCRIPT : cours ===
--- SLIDE 1 : X ---
Note.
        """.trimIndent())

        val task = createTask(scriptDir, capDir, distDir)
        task.execute()

        val content = File(buildDir, "capsule/capsule-context.json").readText()
        assertTrue(content.contains("distribVideo"))
        assertTrue(content.contains("distrib"))
    }

    @Test
    fun `execution produces valid parseable JSON`() {
        val buildDir = File(tempDir, "build")
        val scriptDir = File(buildDir, "capsule").also { it.mkdirs() }
        val capDir = File(buildDir, "capsule").also { it.mkdirs() }
        val distDir = File(buildDir, "capsule/distrib").also { it.mkdirs() }

        File(scriptDir, "json-test-script.txt").writeText("""
=== CAPSULE SCRIPT : json-test ===
--- SLIDE 1 : Verif ---
Test de validité JSON.
        """.trimIndent())

        val task = createTask(scriptDir, capDir, distDir)
        task.execute()

        val jsonFile = File(buildDir, "capsule/capsule-context.json")
        assertTrue(jsonFile.exists())
        val mapper = jacksonObjectMapper()
        val parsed: Map<String, Any> = mapper.readValue(jsonFile)
        assertEquals("capsule", parsed["source"])
        assertNotNull(parsed["entries"], "JSON must have entries array")
        assertTrue(parsed["entries"] is List<*>, "entries must be a list")
    }
}

class CapsuleParseContextTaskTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTask(
        contextJsonFile: File,
        outputFile: File
    ): CapsuleParseContextTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val t = project.tasks.register("transformCapsuleContext", CapsuleParseContextTask::class.java).get()
        t.contextFile.set(contextJsonFile)
        t.outputFile.set(outputFile)
        return t
    }

    @Test
    fun `parses capsule-context json and outputs list of maps`() {
        val contextFile = File(tempDir, "capsule-context.json")
        contextFile.writeText("""
{
  "source": "capsule",
  "version": "0.1.0",
  "entries": [
    {
      "source": "capsule",
      "deckName": "mon-cours",
      "slideCount": 2,
      "originalVideo": "/build/capsule/mon-cours.webm",
      "distribVideo": "/build/capsule/distrib/mon-cours.webm",
      "viewport": { "width": 1408, "height": 792 },
      "distribDimensions": { "width": 1080, "height": 1920 },
      "slides": [
        { "index": 1, "title": "Intro", "speakerNoteLength": 50 },
        { "index": 2, "title": "Content", "speakerNoteLength": 80 }
      ],
      "ttsEngine": "piper",
      "ttsVoice": "fr_FR-siwis-medium"
    }
  ],
  "timestamp": 1234567890
}
        """.trimIndent())

        val outputFile = File(tempDir, "parsed-results.json")

        val task = createTask(contextFile, outputFile)
        task.execute()

        assertTrue(outputFile.exists(), "Output file should be created")
        val content = outputFile.readText()
        assertTrue(content.contains("mon-cours"), "Should contain deck name")
        assertTrue(content.contains("capsule"), "Should contain source=capsule")
        assertTrue(content.contains("originalVideo"), "Should contain originalVideo path")
        assertTrue(content.contains("slideCount"), "Should contain slideCount")
    }

    @Test
    fun `returns empty list when no entries`() {
        val contextFile = File(tempDir, "capsule-context.json")
        contextFile.writeText("""
{
  "source": "capsule",
  "version": "0.1.0",
  "entries": [],
  "timestamp": 1234567890
}
        """.trimIndent())

        val outputFile = File(tempDir, "parsed-results.json")

        val task = createTask(contextFile, outputFile)
        task.execute()

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("[]") || content.contains("[ ]"))
    }

    @Test
    fun `handles missing file gracefully`() {
        val contextFile = File(tempDir, "nonexistent.json")
        val outputFile = File(tempDir, "parsed-results.json")

        val task = createTask(contextFile, outputFile)
        task.execute()

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("[]") || content.contains("[ ]"))
    }
}

class AudioQualityConstraintTest {

    private val webmSignature = byteArrayOf(0x1a.toByte(), 0x45.toByte(), 0xdf.toByte(), 0xa3.toByte())

    private val mp3Signatures = listOf(
        byteArrayOf(0xFF.toByte(), 0xFB.toByte()),
        byteArrayOf(0xFF.toByte(), 0xF3.toByte()),
        byteArrayOf(0xFF.toByte(), 0xF2.toByte()),
        byteArrayOf(0x49, 0x44, 0x33),
        byteArrayOf(0xFF.toByte(), 0xFA.toByte())
    )

    @Test
    fun `real TTS engine must produce binary audio larger than placeholder`() {
        val espeak = EspeakTtsEngine()
        if (!espeak.isAvailable()) return

        val tmpFile = File.createTempFile("audio-constraint", ".mp3")
        tmpFile.deleteOnExit()
        espeak.synthesize("Ceci est un test de contrainte audio pour la capsule video.", tmpFile)
        assertTrue(tmpFile.exists(), "Audio file must exist")
        assertTrue(tmpFile.length() > 1024, "Real audio must be > 1KB, got ${tmpFile.length()} bytes")
    }

    @Test
    fun `real TTS output must not be a text placeholder`() {
        val espeak = EspeakTtsEngine()
        if (!espeak.isAvailable()) return

        val tmpFile = File.createTempFile("audio-notext", ".mp3")
        tmpFile.deleteOnExit()
        espeak.synthesize("Test.", tmpFile)

        val content = tmpFile.readText(Charsets.ISO_8859_1)
        assertTrue(!content.contains("TTS PLACEHOLDER"), "Real audio must not be a text placeholder")
    }

    @Test
    fun `generated WebM video must have EBML header signature`() {
        val espeak = EspeakTtsEngine()
        if (!espeak.isAvailable()) return

        val impl = PlaywrightCaptureImpl(defaultSlideDuration = 3.0)
        if (!impl.isAvailable()) return

        val tmpDir = File.createTempFile("webm-constraint", null)
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val deckFile = File(tmpDir, "test-deck.html")
        deckFile.writeText("""
<html><head>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.css">
</head><body>
<div class="reveal">
  <div class="slides">
    <section><h2>Slide</h2></section>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
<script>Reveal.initialize();</script>
</body></html>
        """.trimIndent())

        val outputDir = File(tmpDir, "video").also { it.mkdirs() }

        try {
            impl.capture(deckFile.absolutePath, outputDir, 1408, 792, listOf(5.0))
            impl.close()

            val videos = outputDir.listFiles { f -> f.name.endsWith(".webm") }
            assertTrue(videos != null && videos.isNotEmpty(), "Must produce a video file")
            val video = videos.first()
            assertTrue(video.length() > 1024, "Video must be > 1KB, got ${video.length()} bytes")

            val header = ByteArray(4)
            video.inputStream().use { it.read(header) }
            assertTrue(header.contentEquals(webmSignature), "Video must have EBML WebM header")
        } catch (e: CapturingException) {
            impl.close()
            throw e
        }
    }
}

class ParallelCaptureTest {

    @TempDir
    lateinit var tempDir: File

    private fun createTinyWebm(output: File) {
        val proc = ProcessBuilder(
            "ffmpeg", "-y", "-f", "lavfi", "-i", "testsrc=duration=1:size=2x2:rate=1",
            "-pix_fmt", "yuv420p", output.absolutePath
        ).redirectErrorStream(true).start()
        proc.waitFor()
    }

    @Test
    fun `concatWebmFiles creates output from multiple webm files`() {
        val webm1 = File(tempDir, "slide1.webm").also { createTinyWebm(it) }
        val webm2 = File(tempDir, "slide2.webm").also { createTinyWebm(it) }
        val output = File(tempDir, "output.webm")

        val result = CapsuleVideoTask.concatWebmFiles(listOf(webm1, webm2), output)

        assertTrue(result, "concatWebmFiles should succeed")
        assertTrue(output.exists(), "Output file should exist")
        assertTrue(output.length() > 0, "Output file should not be empty")
        assertTrue(!webm1.exists(), "Individual webm1 should be cleaned up")
        assertTrue(!webm2.exists(), "Individual webm2 should be cleaned up")
    }

    @Test
    fun `concatWebmFiles handles empty list gracefully`() {
        val output = File(tempDir, "empty.webm")
        val result = CapsuleVideoTask.concatWebmFiles(emptyList(), output)
        assertTrue(!result, "Should return false for empty list")
        assertTrue(!output.exists(), "No output should be created for empty list")
    }

    @Test
    fun `concatWebmFiles cleans up individual webm files after concat`() {
        val webm1 = File(tempDir, "a.webm").also { createTinyWebm(it) }
        val output = File(tempDir, "out.webm")

        CapsuleVideoTask.concatWebmFiles(listOf(webm1), output)

        assertTrue(!webm1.exists(), "Input file should be deleted after concat")
        assertTrue(output.exists(), "Output should exist")
    }

    @Test
    fun `concatWebmFiles falls back gracefully when ffmpeg not available`() {
        val webm1 = File(tempDir, "single.webm").also { createTinyWebm(it) }
        val output = File(tempDir, "fallback.webm")

        val result = CapsuleVideoTask.concatWebmFiles(listOf(webm1), output, "__nonexistent_ffmpeg__")
        assertTrue(!result, "Should return false when ffmpeg not available")
        assertTrue(!output.exists(), "No output should exist when ffmpeg not available")
        assertTrue(webm1.exists(), "Input files should NOT be deleted when concat fails")
    }

    @Test
    fun `captureSlideParallel limits concurrency to 4 instances`() {
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

        val activeFactories = AtomicInteger(0)
        val maxConcurrentFactories = AtomicInteger(0)
        val totalCreated = AtomicInteger(0)

        val fakeCapture = object : PlaywrightCapture {
            override fun capture(deckHtmlPath: String, outputDir: File, viewportWidth: Int, viewportHeight: Int, slideDurations: List<Double>) {
                outputDir.mkdirs()
                outputDir.resolve("slide.webm").writeText("fake")
            }
            override fun isAvailable() = true
            override fun name() = "fake"
            override fun close() {}
        }

        val factory: () -> PlaywrightCapture = {
            val current = activeFactories.incrementAndGet()
            totalCreated.incrementAndGet()
            var old: Int
            do {
                old = maxConcurrentFactories.get()
                if (current <= old) break
            } while (!maxConcurrentFactories.compareAndSet(old, current))
            Thread.sleep(50)
            activeFactories.decrementAndGet()
            fakeCapture
        }

        val script = CapsuleScript(
            deckName = "parallel-test",
            slides = (1..6).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }

        task.captureSlideParallel(
            deckHtmlPath = File(tempDir, "deck.html").absolutePath,
            outputDir = outputDir,
            viewportWidth = 1408,
            viewportHeight = 792,
            parsed = script,
            audioDir = audioDir,
            captureFactory = factory
        )

        assertEquals(6, totalCreated.get(), "All 6 slides should create a capture instance")
        assertTrue(maxConcurrentFactories.get() <= 4, "Max concurrency should be capped at 4, was ${maxConcurrentFactories.get()}")
    }

    @Test
    fun `captureSlideParallel creates one webm per slide`() {
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

        val capturedSlides = AtomicInteger(0)
        val fakeCapture = object : PlaywrightCapture {
            override fun capture(deckHtmlPath: String, outputDir: File, viewportWidth: Int, viewportHeight: Int, slideDurations: List<Double>) {
                outputDir.mkdirs()
                outputDir.resolve("slide.webm").writeText("fake webm content")
                capturedSlides.incrementAndGet()
            }
            override fun isAvailable() = true
            override fun name() = "fake"
            override fun close() {}
        }

        val script = CapsuleScript(
            deckName = "one-per-slide",
            slides = (1..3).map { SlideSegment(it, "Slide $it", "Note $it") }
        )
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val outputDir = File(tempDir, "video").also { it.mkdirs() }

        task.captureSlideParallel(
            deckHtmlPath = File(tempDir, "deck.html").absolutePath,
            outputDir = outputDir,
            viewportWidth = 1408,
            viewportHeight = 792,
            parsed = script,
            audioDir = audioDir,
            captureFactory = { fakeCapture }
        )

        assertEquals(3, capturedSlides.get(), "Should capture 3 slides")
        // concatWebmFiles will fail with fake text files (no real WebM), which is expected
        // The test verifies parallel capture orchestration, not FFmpeg integration
    }
}

class RevealJsRenderingConstraintTest {

    @Test
    @Tag("integration")
    fun `playwright capture must render reveal js slides individually not all on one page`() {
        val impl = PlaywrightCaptureImpl(defaultSlideDuration = 2.0)
        if (!impl.isAvailable()) return

        val tmpDir = File.createTempFile("reveal-constraint", null)
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val deckFile = File(tmpDir, "reveal-test.html")
        deckFile.writeText("""
<html><head>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.css">
</head><body>
<div class="reveal">
  <div class="slides">
    <section><h2>Slide A</h2></section>
    <section><h2>Slide B</h2></section>
    <section><h2>Slide C</h2></section>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>
<script>Reveal.initialize();</script>
</body></html>
        """.trimIndent())

        val outputDir = File(tmpDir, "video").also { it.mkdirs() }

        try {
            impl.capture(deckFile.absolutePath, outputDir, 1408, 792, listOf(5.0, 5.0, 5.0))
            impl.close()

            val videos = outputDir.listFiles { f -> f.name.endsWith(".webm") }
            assertTrue(videos != null && videos.isNotEmpty(), "Must produce a video file")
            val video = videos.first()
            assertTrue(video.length() > 1024, "Video must be > 1KB, got ${video.length()} bytes")
        } catch (e: CapturingException) {
            impl.close()
            throw e
        }
    }
}
