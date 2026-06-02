package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

@DisableCachingByDefault(because = "Filesystem-bound: injects audio into HTML deck and captures video via Playwright")
open class CapsuleVideoTask : DefaultTask() {

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    lateinit var capsuleExtension: CapsuleExtension

    companion object {
        private const val AUDIO_INJECT_SCRIPT = """
<!-- CAPSULE-GRADLE: Autoplay audio injection -->
<script>
(function() {
  var currentAudio = null;
  var sections = document.querySelectorAll('.reveal .slides section[data-audio]');
  var audios = [];
  sections.forEach(function(sec) {
    var src = sec.getAttribute('data-audio');
    if (src) {
      var audio = new Audio(src.replace('file://', ''));
      audio.id = 'audio-' + audios.length;
      document.body.appendChild(audio);
      audios.push(audio);
    }
  });
  function playSlideAudio(idx) {
    if (currentAudio) { currentAudio.pause(); currentAudio.currentTime = 0; }
    currentAudio = audios[idx];
    if (currentAudio) {
      currentAudio.currentTime = 0;
      currentAudio.play().catch(function(e) { console.warn('Audio play failed:', e); });
    }
  }
  if (typeof Reveal !== 'undefined') {
    Reveal.on('slidechanged', function(event) {
      playSlideAudio(event.indexh);
    });
    if (audios.length > 0) playSlideAudio(0);
  }
})();
</script>
"""

        @JvmStatic
        fun concatWebmFiles(webmFiles: List<File>, outputFile: File, ffmpegPath: String = "ffmpeg"): Boolean {
            if (webmFiles.isEmpty()) return false
            val concatList = File(outputFile.parentFile, "concat-${System.currentTimeMillis()}.txt")
            concatList.writeText(webmFiles.joinToString("\n") { "file '${it.absolutePath.replace("'", "'\\''")}'" })
            val success = try {
                val proc = ProcessBuilder(
                    ffmpegPath, "-y", "-f", "concat", "-safe", "0", "-i", concatList.absolutePath, "-c", "copy", outputFile.absolutePath
                ).redirectErrorStream(true).start()
                val exitCode = proc.waitFor()
                exitCode == 0 && outputFile.exists()
            } catch (e: Exception) {
                false
            }
            concatList.delete()
            if (success) {
                webmFiles.forEach { it.delete() }
            }
            return success
        }

        /**
         * Extracts a single slide from a reveal.js deck HTML and produces a standalone HTML document
         * that can be rendered independently by Playwright/ScreenshotCapture.
         *
         * @param deckHtml The full deck HTML source
         * @param slideIndex 0-based index of the section within `.slides` container
         * @return Standalone HTML with only the requested slide, preserving head, styles, and scripts
         */
        @JvmStatic
        fun createSingleSlideHtml(deckHtml: String, slideIndex: Int): String {
            val headMatch = Regex("""(?s)(<head>.*?</head>)""").find(deckHtml)
            val headSection = headMatch?.value ?: ""

            val slidesDivRegex = Regex("""(?s)<div class="slides">\s*(.*?)\s*</div>""", RegexOption.DOT_MATCHES_ALL)
            val slidesDivMatch = slidesDivRegex.find(deckHtml)
            if (slidesDivMatch == null) {
                return deckHtml
            }
            val slidesContent = slidesDivMatch.groupValues[1]

            // Extract top-level <section> elements (may contain nested <section> for vertical stacks)
            val topLevelSections = extractTopLevelSections(slidesContent)

            if (slideIndex < 0 || slideIndex >= topLevelSections.size) {
                return deckHtml
            }

            val targetSection = topLevelSections[slideIndex]

            return buildString {
                appendLine("<!DOCTYPE html>")
                appendLine("<html>")
                appendLine(headSection)
                appendLine("<body>")
                appendLine("""<div class="reveal">""")
                appendLine("""  <div class="slides">""")
                appendLine("    $targetSection")
                appendLine("  </div>")
                appendLine("</div>")
                appendLine("""<script src="https://cdn.jsdelivr.net/npm/reveal.js@5.1.0/dist/reveal.js"></script>""")
                appendLine("<script>Reveal.initialize();</script>")
                appendLine("</body>")
                appendLine("</html>")
            }.trimIndent()
        }

        /**
         * Extracts top-level <section> elements from the slides container content,
         * handling nested sections (vertical stacks) by tracking depth.
         */
        private fun extractTopLevelSections(slidesContent: String): List<String> {
            val sections = mutableListOf<String>()
            val sectionOpenRegex = Regex("""<section\b[^>]*>""")
            val sectionCloseRegex = Regex("""</section>""")

            var depth = 0
            var currentStart = -1
            var pos = 0

            while (pos < slidesContent.length) {
                val openMatch = sectionOpenRegex.find(slidesContent, pos)
                val closeMatch = sectionCloseRegex.find(slidesContent, pos)

                val nextOpen = openMatch?.range?.first ?: Int.MAX_VALUE
                val nextClose = closeMatch?.range?.first ?: Int.MAX_VALUE

                if (nextOpen < nextClose && openMatch != null) {
                    if (depth == 0) {
                        currentStart = openMatch.range.first
                    }
                    depth++
                    pos = openMatch.range.last + 1
                } else if (closeMatch != null) {
                    depth--
                    if (depth == 0 && currentStart >= 0) {
                        sections.add(slidesContent.substring(currentStart, closeMatch.range.last + 1))
                        currentStart = -1
                    }
                    pos = closeMatch.range.last + 1
                } else {
                    break
                }
            }

            return sections
        }
    }

    @get:Internal
    internal var playwrightCapture: PlaywrightCapture? = null

    @get:Internal
    internal var ttsEngine: TtsEngine? = null

    init {
        outputDir.convention(
            project.layout.buildDirectory.dir("capsule")
        )
    }

    private fun resolvePlaywrightCapture(slideDurations: List<Double>): PlaywrightCapture {
        if (playwrightCapture != null) return playwrightCapture!!

        val defaultDur = capsuleExtension.slideDurationSeconds.get()
        val impl = PlaywrightCaptureImpl(
            timeout = capsuleExtension.playwrightTimeout.get(),
            defaultSlideDuration = defaultDur
        )
        return if (impl.isAvailable()) {
            val totalSecs = slideDurations.sum()
            logger.lifecycle("Playwright capture: available ({} slides, {}s total)", slideDurations.size, String.format("%.1f", totalSecs))
            impl
        } else {
            logger.warn("Playwright not available, falling back to noop capture")
            NoOpPlaywrightCapture()
        }
    }

    private fun resolveTtsEngine(): TtsEngine {
        if (ttsEngine != null) return ttsEngine!!

        return when (capsuleExtension.ttsEngine.get().lowercase()) {
            "piper" -> {
                val engine = PiperTtsEngine(
                    capsuleExtension.piperExecutablePath.get(),
                    capsuleExtension.ttsVoice.get()
                )
                if (engine.isAvailable()) engine else NoOpTtsEngine()
            }
            "espeak" -> {
                val voice = capsuleExtension.espeakVoice.get()
                val speed = capsuleExtension.espeakSpeed.get()
                val engine = EspeakTtsEngine(voice = voice, speed = speed)
                if (engine.isAvailable()) engine else NoOpTtsEngine()
            }
            else -> NoOpTtsEngine()
        }
    }

    private fun computeSlideDurations(parsed: CapsuleScript, audioDir: File): List<Double> {
        val defaultDur = capsuleExtension.slideDurationSeconds.get()
        return parsed.slides.map { seg ->
            val idx = String.format("%02d", seg.index)
            val mp3 = audioDir.resolve("slide-$idx.mp3")
            if (mp3.exists()) {
                val realDur = ffprobeDuration(mp3)
                if (realDur > 0.0) realDur else defaultDur
            } else defaultDur
        }
    }

    @TaskAction
    fun execute() {
        val deckDir = CapsuleManager.resolveDeckDir(project, capsuleExtension)
        val scriptDir = CapsuleManager.resolveScriptDir(project, capsuleExtension)

        val deckFiles = deckDir.listFiles { f -> f.name.endsWith("-deck.html") }?.toList()
            ?: emptyList()
        val scripts = CapsuleManager.readScriptFiles(scriptDir)

        if (deckFiles.isEmpty()) {
            logger.warn("No *-deck.html files found in {}", deckDir.absolutePath)
            logger.warn("Run 'asciidoctorRevealJs' from slider-gradle first.")
            return
        }

        if (scripts.isEmpty()) {
            logger.warn("No capsule scripts found. Run 'asciidocCapsule' from slider-gradle first.")
            return
        }

        val outDir = project.layout.buildDirectory.dir(
            capsuleExtension.outputDir.get()
        ).get().asFile
        outDir.mkdirs()

        val engine = resolveTtsEngine()

        for (script in scripts) {
            val parsed = CapsuleManager.parseScript(script)
            val audioDir = outDir.resolve(parsed.deckName)
            audioDir.mkdirs()

            for (seg in parsed.slides) {
                val idx = String.format("%02d", seg.index)
                val ttsFile = audioDir.resolve("slide-$idx.mp3")
                if (!ttsFile.exists()) {
                    try {
                        engine.synthesize(seg.speakerNote, ttsFile)
                        logger.lifecycle("  TTS → {} ({} chars)", ttsFile.name, seg.speakerNote.length)
                    } catch (e: TtsException) {
                        logger.warn("  TTS SKIP slide {}: {}", seg.index, e.message)
                    }
                }
            }

            val deckFile = deckFiles.find { it.nameWithoutExtension.startsWith(parsed.deckName) }
                ?: deckFiles.firstOrNull()
            if (deckFile == null) {
                logger.warn("No matching deck HTML found for '{}'", parsed.deckName)
                continue
            }

            val modifiedDeck = injectAudio(deckFile, parsed, audioDir)
            val videoOutputDir = outDir.resolve(parsed.deckName).resolve("video")
            videoOutputDir.mkdirs()

            val slideDurations = computeSlideDurations(parsed, audioDir)

            if (capsuleExtension.parallelCaptureEnabled.get()) {
                // Parallel path: createSingleSlideHtml per slide + captureSlideParallel
                logger.lifecycle("  Parallel capture enabled for '{}' ({} slides)", parsed.deckName, parsed.slides.size)
                captureSlideParallel(
                    deckHtmlPath = modifiedDeck.absolutePath,
                    outputDir = videoOutputDir,
                    viewportWidth = capsuleExtension.viewportWidth.get(),
                    viewportHeight = capsuleExtension.viewportHeight.get(),
                    parsed = parsed,
                    audioDir = audioDir
                )
                val concatVideo = videoOutputDir.resolve("${parsed.deckName}.webm")
                if (concatVideo.exists()) {
                    val finalVideo = outDir.resolve("${parsed.deckName}.webm")
                    concatVideo.copyTo(finalVideo, overwrite = true)
                    mixAudioWithVideo(finalVideo, audioDir, parsed.slides, capsuleExtension.slideDurationSeconds.get())
                    logger.lifecycle("CAPSULE (parallel) → {}", finalVideo.absolutePath)
                } else {
                    logger.warn("Parallel capture produced no video for '{}'", parsed.deckName)
                }
            } else {
                // Sequential path (default): capture entire deck at once
                val deckCapture = resolvePlaywrightCapture(slideDurations)
                try {
                    deckCapture.capture(
                        deckHtmlPath = modifiedDeck.absolutePath,
                        outputDir = videoOutputDir,
                        viewportWidth = capsuleExtension.viewportWidth.get(),
                        viewportHeight = capsuleExtension.viewportHeight.get(),
                        slideDurations = slideDurations
                    )
                    deckCapture.close()

                    val generatedVideo = videoOutputDir.listFiles { f -> f.name.endsWith(".webm") }
                        ?.firstOrNull()
                    if (generatedVideo != null) {
                        val finalVideo = outDir.resolve("${parsed.deckName}.webm")
                        generatedVideo.copyTo(finalVideo, overwrite = true)
                        mixAudioWithVideo(finalVideo, audioDir, parsed.slides, capsuleExtension.slideDurationSeconds.get())
                        logger.lifecycle("CAPSULE → {}", finalVideo.absolutePath)
                    } else {
                        logger.warn("No video generated by Playwright capture for '{}'", parsed.deckName)
                    }
                } catch (e: CapturingException) {
                    logger.error("Playwright capture failed for '{}': {}", parsed.deckName, e.message)
                    throw e
                }
            }
        }
    }

    internal fun captureSlideParallel(
        deckHtmlPath: String,
        outputDir: File,
        viewportWidth: Int,
        viewportHeight: Int,
        parsed: CapsuleScript,
        audioDir: File,
        captureFactory: (() -> PlaywrightCapture)? = null
    ) {
        val executor = Executors.newFixedThreadPool(4)
        val futures = mutableListOf<Future<File?>>()
        val slideDurations = computeSlideDurations(parsed, audioDir)

        // Read the deck HTML once for createSingleSlideHtml extraction
        val deckHtml = File(deckHtmlPath).readText()

        for ((idx, seg) in parsed.slides.withIndex()) {
            val slideDir = outputDir.resolve("slide-${String.format("%02d", seg.index)}")
            futures.add(executor.submit<File?> {
                val capture = captureFactory?.invoke() ?: resolvePlaywrightCapture(listOf(slideDurations[idx]))
                try {
                    // Create a standalone HTML for this specific slide
                    val singleSlideHtml = createSingleSlideHtml(deckHtml, idx)
                    val singleSlideFile = slideDir.resolve("slide.html")
                    slideDir.mkdirs()
                    singleSlideFile.writeText(singleSlideHtml)

                    capture.capture(singleSlideFile.absolutePath, slideDir, viewportWidth, viewportHeight, listOf(slideDurations[idx]))
                    val source = slideDir.resolve("slide.webm")
                    if (source.exists()) {
                        val target = outputDir.resolve("slide-${String.format("%02d", seg.index)}.webm")
                        source.copyTo(target, overwrite = true)
                        target
                    } else null
                } finally {
                    capture.close()
                }
            })
        }

        val webmFiles = futures.mapNotNull { it.get() }
        executor.shutdown()

        if (webmFiles.isNotEmpty()) {
            val finalVideo = outputDir.resolve("${parsed.deckName}.webm")
            concatWebmFiles(webmFiles, finalVideo)
        }
    }

    private fun injectAudio(deckFile: File, script: CapsuleScript, audioDir: File): File {
        val originalHtml = deckFile.readText()
        val injectedDir = project.layout.buildDirectory.dir("capsule/injected").get().asFile
        injectedDir.mkdirs()

        val hasAudio = script.slides.any { seg ->
            val idx = String.format("%02d", seg.index)
            val audioFile = audioDir.resolve("slide-$idx.mp3")
            audioFile.exists()
        }

        if (!hasAudio) {
            val outFile = injectedDir.resolve(deckFile.name)
            outFile.writeText(originalHtml)
            return outFile
        }

        val hasDataCapsuleSlide = originalHtml.contains("data-capsule-slide=")

        val injectedHtml = originalHtml.lines().map { line ->
            if (line.contains("<section") && !line.contains("</section>")) {
                var mutableLine = line
                for (seg in script.slides) {
                    if (line.contains("data-capsule-slide=\"${seg.index}\"") || line.contains("data-capsule-slide='${seg.index}'")) {
                        val idx = String.format("%02d", seg.index)
                        val audioPath = audioDir.resolve("slide-$idx.mp3").absolutePath
                        mutableLine = mutableLine.replace(
                            "<section",
                            "<section data-audio=\"file://$audioPath\""
                        )
                        break
                    }
                }
                mutableLine
            } else {
                line
            }
        }.joinToString("\n")

        if (!hasDataCapsuleSlide) {
            return injectAudioSequentialFallback(deckFile, script, audioDir, injectedDir)
        }

        val injected = injectedHtml.replace(
            "</body>",
            "$AUDIO_INJECT_SCRIPT</body>"
        )

        val outFile = injectedDir.resolve(deckFile.name)
        outFile.writeText(injected)
        return outFile
    }

    private fun isPlaceholder(file: File): Boolean {
        return file.length() < 500 && file.readText().startsWith("# TTS PLACEHOLDER")
    }

    private fun injectAudioSequentialFallback(
        deckFile: File,
        script: CapsuleScript,
        audioDir: File,
        injectedDir: File
    ): File {
        val originalHtml = deckFile.readText()

        val slideRegex = Regex("""<section\b[^>]*>""")
        val sections = slideRegex.findAll(originalHtml).toList()

        val injectedHtml = buildString {
            var lastEnd = 0
            var slideIdx = 0
            for (match in sections) {
                append(originalHtml.substring(lastEnd, match.range.first))
                var tag = match.value
                if (slideIdx < script.slides.size) {
                    val seg = script.slides[slideIdx]
                    val idx = String.format("%02d", seg.index)
                    val audioPath = audioDir.resolve("slide-$idx.mp3").absolutePath
                    tag = tag.replace("<section", "<section data-audio=\"file://$audioPath\"")
                }
                append(tag)
                lastEnd = match.range.last + 1
                slideIdx++
            }
            append(originalHtml.substring(lastEnd))
        }

        val sequentialScript = AUDIO_INJECT_SCRIPT.replace(
            "<!-- CAPSULE-GRADLE: Autoplay audio injection -->",
            "<!-- CAPSULE-GRADLE: Autoplay audio injection (sequential fallback) -->"
        )

        val injected = injectedHtml.replace(
            "</body>",
            "$sequentialScript</body>"
        )

        val outFile = injectedDir.resolve(deckFile.name)
        outFile.writeText(injected)
        return outFile
    }

    private fun mixAudioWithVideo(videoFile: File, audioDir: File, slides: List<SlideSegment>, slideDurationSeconds: Double) {
        val mp3Files = slides.mapNotNull { seg ->
            val idx = String.format("%02d", seg.index)
            val f = audioDir.resolve("slide-$idx.mp3")
            f.takeIf { it.exists() }
        }
        if (mp3Files.isEmpty()) return

        val cmd = mutableListOf("ffmpeg", "-y", "-i", videoFile.absolutePath)
        val concatInputs = mutableListOf<String>()

        for ((i, mp3) in mp3Files.withIndex()) {
            val inputIdx = i + 1
            cmd.addAll(listOf("-i", mp3.absolutePath))
            concatInputs.add("[$inputIdx:a]")
        }

        val filterComplex = "${concatInputs.joinToString("")}concat=n=${mp3Files.size}:v=0:a=1[aout]"
        cmd.addAll(listOf("-filter_complex", filterComplex, "-map", "0:v", "-map", "[aout]", "-c:v", "copy", "-c:a", "libvorbis", "-shortest"))

        val tmpFile = File(videoFile.absolutePath + ".tmp.webm")
        cmd.add(tmpFile.absolutePath)

        try {
            val proc = ProcessBuilder(cmd).redirectErrorStream(true).start()
            val exitCode = proc.waitFor()
            if (exitCode == 0 && tmpFile.exists()) {
                tmpFile.renameTo(videoFile)
                val totalSlides = mp3Files.size
                val audioDur = mp3Files.sumOf { f ->
                    ffprobeDuration(f)
                }
                logger.lifecycle("  Audio mix: {} slides concatenated (audio={}s, video={}s)", totalSlides, String.format("%.1f", audioDur), String.format("%.1f", ffprobeDuration(videoFile)))
            } else {
                logger.warn("  Audio mix failed (ffmpeg exit code {}), video remains silent", exitCode)
                tmpFile.delete()
            }
        } catch (e: Exception) {
            logger.warn("  Audio mix error: {} — video remains silent", e.message)
            tmpFile.delete()
        }
    }

    private fun ffprobeDuration(file: File): Double {
        return try {
            val proc = ProcessBuilder("ffprobe", "-v", "quiet", "-show_entries", "format=duration", "-of", "csv=p=0", file.absolutePath)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out.toDoubleOrNull() ?: 0.0
        } catch (_: Exception) { 0.0 }
    }
}
