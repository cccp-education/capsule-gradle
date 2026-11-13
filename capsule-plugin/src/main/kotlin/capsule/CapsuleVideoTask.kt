package capsule

import capsule.a11y.AudioDescriptionInput
import capsule.a11y.AudioDescriptionMixCommand
import capsule.a11y.AudioDescriptionMixSettings
import capsule.a11y.AudioDescriptionPlanner
import capsule.a11y.AudioDescriptionScriptParser
import capsule.a11y.NarrationWindow
import capsule.adapter.revealjs.RevealJsCaptureSettings
import capsule.adapter.revealjs.RevealJsDeckHtml
import capsule.adapter.revealjs.RevealJsEngineAdapter
import capsule.adapter.revealjs.RevealJsRenderPlan
import capsule.audio.AudioPostConfig
import capsule.audio.NoOpAudioPostProcessor
import capsule.audio.music.MusicAssetResolver
import capsule.audio.music.MusicLibraryManifestLoader
import capsule.audio.music.MusicResolution
import capsule.audio.music.ResolvedMusicAsset
import capsule.feed.CapsuleScript
import capsule.feed.CapsuleScriptReader
import capsule.feed.SlideSegment
import capsule.validation.VideoIntegrityProbe
import capsule.voice.PronunciationLexicon
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import video.kernel.FileVisualSource
import video.kernel.VideoRenderRequest
import video.kernel.segment.VideoKind
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@DisableCachingByDefault(because = "Filesystem-bound: injects audio into HTML deck and captures video via Playwright")
open class CapsuleVideoTask : DefaultTask() {
    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = capsuleExtensionCache ?: project.extensions.getByType(CapsuleExtension::class.java).also { capsuleExtensionCache = it }
        set(value) {
            capsuleExtensionCache = value
        }

    private var capsuleExtensionCache: CapsuleExtension? = null

    companion object {
        /**
         * Default per-slide capture timeout (5 minutes). A capture that exceeds
         * this bound aborts the parallel run and shuts the executor down.
         * Configurable via the [CapsuleExtension.captureTimeoutMinutes] DSL (CR-2.3).
         */
        const val DEFAULT_CAPTURE_TIMEOUT_MILLIS: Long = 5 * 60 * 1000L

        /** PRT-06 — sidecar suffix holding the render signature next to a video. */
        internal const val SIGNATURE_SUFFIX: String = ".sig"
    }

    @get:Internal
    internal var playwrightCapture: PlaywrightCapture? = null

    @get:Internal
    internal var ttsEngine: TtsEngine? = null

    @get:Internal
    internal var manimEngine: ManimEngine? = null

    @get:Internal
    internal var manimVideoMixer: ManimVideoMixer? = null

    @get:Internal
    internal var manimSlideReplacer: ManimSlideReplacer? = null

    @get:Internal
    internal var manimParallelRenderer: ManimParallelRenderer? = null

    @get:Internal
    internal var subtitleGenerator: SubtitleGenerator? = null

    @get:Internal
    internal var subtitleBurnInService: SubtitleBurnInService? = null

    init {
        outputDir.convention(
            project.layout.buildDirectory.dir("capsule"),
        )
    }

    /**
     * Builds the reveal.js engine adapter from the current extension state
     * (CAP-ENGINE-DECOUPLING US-2). The Playwright/Screenshot/Remotion engine
     * resolution and capture orchestration now live in the adapter; the task
     * keeps only its config snapshot and post-processing.
     *
     * @param captureProvider optional fake engine supplier (tests); when `null`
     *        the adapter resolves the real engine from the settings.
     */
    private fun revealJsAdapter(
        plan: RevealJsRenderPlan,
        captureProvider: (() -> PlaywrightCapture)? = null,
    ): RevealJsEngineAdapter =
        RevealJsEngineAdapter(
            settings =
                RevealJsCaptureSettings(
                    playwrightTimeout = capsuleExtension.playwrightTimeout.get(),
                    defaultSlideDuration = capsuleExtension.slideDurationSeconds.get(),
                    configuredStrategy = capsuleExtension.captureStrategy.get(),
                    viralEnabled = capsuleExtension.viralEnabled.getOrElse(false),
                    strict = capsuleExtension.strictMode.get(),
                    ffmpegPath = capsuleExtension.ffmpegExecutablePath.get(),
                    parallelThreads = capsuleExtension.parallelCaptureThreads.get(),
                    previewOnly = capsuleExtension.previewOnly.get(),
                    chromiumPath = capsuleExtension.chromiumExecutablePath.get(),
                    remotionProjectDir =
                        project.layout.buildDirectory
                            .dir(capsuleExtension.remotionProjectDir.get())
                            .get()
                            .asFile,
                    remotionNodePath = capsuleExtension.remotionNodeExecutablePath.get(),
                    remotionConcurrency = capsuleExtension.remotionConcurrency.get(),
                    remotionFps = capsuleExtension.remotionFps.get(),
                    // Manim writes under <mediaDir>/videos/<module>/<quality>/ ;
                    // the quality directory depends on the resolution asked for,
                    // so every leaf is offered rather than one guessed path.
                    manimSources = CapsuleVideoResolvers.manimVideoDirs(project, capsuleExtension),
                ),
            plan = plan,
            logger = logger,
            captureProvider = captureProvider,
        )

    internal fun resolveTtsEngine(): TtsEngine {
        if (ttsEngine != null) return ttsEngine!!
        return CapsuleVideoResolvers.resolveTtsEngine(project, capsuleExtension)
    }

    private fun resolveManimEngineInternal(): ManimEngine {
        if (manimEngine != null) return manimEngine!!
        return CapsuleVideoResolvers.resolveManimEngine(capsuleExtension, logger)
    }

    private fun resolveManimVideoMixerInternal(): ManimVideoMixer {
        if (manimVideoMixer != null) return manimVideoMixer!!
        return CapsuleVideoResolvers.resolveManimVideoMixer(capsuleExtension, logger)
    }

    private fun resolveManimSlideReplacerInternal(): ManimSlideReplacer {
        if (manimSlideReplacer != null) return manimSlideReplacer!!
        return CapsuleVideoResolvers.resolveManimSlideReplacer(logger)
    }

    private fun resolveManimParallelRenderer(): ManimParallelRenderer {
        if (manimParallelRenderer != null) return manimParallelRenderer!!
        return CapsuleVideoResolvers.resolveManimParallelRenderer(capsuleExtension, logger)
    }

    internal fun computeSlideDurations(
        parsed: CapsuleScript,
        audioDir: File,
    ): List<Double> = computeSlideDurationsWithManim(parsed, audioDir, emptyMap())

    /**
     * Computes slide durations, using Manim MP4 probe duration when available.
     *
     * Priority for each slide:
     * 1. MANIM slide with probed duration > 0: use manim duration
     * 2. TTS MP3 file exists with probed duration > 0: use audio duration
     * 3. Fallback: use capsuleExtension.slideDurationSeconds default
     */
    internal fun computeSlideDurationsWithManim(
        parsed: CapsuleScript,
        audioDir: File,
        manimDurations: Map<Int, Double>,
    ): List<Double> {
        val defaultDur = capsuleExtension.slideDurationSeconds.get()
        return parsed.segments.map { seg ->
            // Priority 1: MANIM slide with probed video duration
            val manimDur = manimDurations[seg.index]
            if (seg.kind == VideoKind.ANIMATION && manimDur != null && manimDur > 0.0) {
                manimDur
            } else {
                // Priority 2: TTS audio file with probed duration
                val idx = String.format("%02d", seg.index)
                val mp3 = audioDir.resolve("slide-$idx.mp3")
                if (mp3.exists()) {
                    val realDur = MediaProbeUtil.probeDuration(mp3)
                    if (realDur > 0.0) realDur else defaultDur
                } else {
                    defaultDur
                }
            }
        }
    }

    internal fun synthesizeTtsForScript(
        parsed: CapsuleScript,
        audioDir: File,
        engine: TtsEngine,
    ): List<Int> {
        val failedSlides = mutableListOf<Int>()
        for (seg in parsed.segments) {
            val idx = String.format("%02d", seg.index)
            val ttsFile = audioDir.resolve("slide-$idx.mp3")
            if (!ttsFile.exists()) {
                try {
                    engine.synthesize(seg.narration, ttsFile)
                    logger.lifecycle("  TTS → {} ({} chars)", ttsFile.name, seg.narration.length)
                } catch (e: TtsException) {
                    logger.warn("  TTS retry slide {}: {}", seg.index, e.message)
                    try {
                        engine.synthesize(seg.narration, ttsFile)
                        logger.lifecycle("  TTS → {} (retry, {} chars)", ttsFile.name, seg.narration.length)
                    } catch (e2: TtsException) {
                        failedSlides.add(seg.index)
                        logger.error("  TTS SKIP slide {} after retry: {}", seg.index, e2.message)
                    }
                }
            }
        }
        if (failedSlides.isNotEmpty()) {
            logger.error(
                "  TTS: {} slide(s) failed synthesis after retry: {}",
                failedSlides.size,
                failedSlides.joinToString(", "),
            )
        }
        return failedSlides
    }

    internal fun renderManimSlides(
        parsed: CapsuleScript,
        manim: ManimEngine,
        manimMixer: ManimVideoMixer,
        manimScriptsDir: File,
        manimOutputDir: File,
        audioDir: File,
        manimDurations: MutableMap<Int, Double>,
    ): Map<Int, File> {
        val manimSlides = parsed.segments.filter { it.kind == VideoKind.ANIMATION }
        if (manimSlides.isEmpty()) return emptyMap()

        logger.lifecycle("  Manim slides detected: {} slides with Manim animations", manimSlides.size)
        manimOutputDir.mkdirs()

        val parallelRenderer = resolveManimParallelRenderer()
        val rendered = parallelRenderer.renderAll(manimSlides, manim, manimScriptsDir, manimOutputDir)
        logger.lifecycle("  Manim render complete: {}/{} slides rendered", rendered.size, manimSlides.size)

        for ((slideIdx, manimVideo) in rendered) {
            val seg = manimSlides.find { it.index == slideIdx } ?: continue
            val sceneName = seg.scene ?: continue

            logger.lifecycle("    Manim → {} (scene: {})", manimVideo.name, sceneName)

            val probedDur = manim.probeDuration(manimVideo)
            if (probedDur > 0.0) {
                manimDurations[seg.index] = probedDur
                logger.lifecycle("    Manim duration: {}s (scene: {})", String.format("%.1f", probedDur), sceneName)
            }

            val slideIdxStr = String.format("%02d", seg.index)
            val ttsFile = audioDir.resolve("slide-$slideIdxStr.mp3")
            val muxedFile = manimOutputDir.resolve("$sceneName-muxed.mp4")
            try {
                val muxed = manimMixer.mix(manimVideo, ttsFile, muxedFile)
                logger.lifecycle("    Manim+TTS → {} ({} bytes)", muxed.name, muxed.length())
                val muxedDur = manimMixer.probeDuration(muxed)
                if (muxedDur > 0.0) {
                    manimDurations[seg.index] = muxedDur
                    logger.lifecycle("    Muxed duration: {}s (scene: {})", String.format("%.1f", muxedDur), sceneName)
                }
            } catch (e: MixerException) {
                logger.warn("    Manim mux failed for scene '{}': {} — using unmixed video", sceneName, e.message)
            }
        }
        return rendered
    }

    internal fun replaceManimSlidesInDeck(
        parsed: CapsuleScript,
        modifiedDeck: File,
        manimOutputDir: File,
        renderedFiles: Map<Int, File>,
        manimReplacer: ManimSlideReplacer,
        subtitleFile: File?,
    ): File {
        val manimSlides = parsed.segments.filter { it.kind == VideoKind.ANIMATION }
        var finalDeckHtml = modifiedDeck.readText()

        val manimSlideIndices =
            manimSlides.mapIndexedNotNull { _, seg ->
                val fullIdx = parsed.segments.indexOf(seg)
                if (fullIdx >= 0) {
                    val sceneName = seg.scene ?: return@mapIndexedNotNull null
                    val muxedFile = manimOutputDir.resolve("$sceneName-muxed.mp4")
                    val manimFile = manimOutputDir.resolve("$sceneName.mp4")
                    val renderFile = renderedFiles[seg.index]
                    val videoPath =
                        when {
                            muxedFile.exists() -> muxedFile.absolutePath
                            renderFile != null && renderFile.exists() -> renderFile.absolutePath
                            manimFile.exists() -> manimFile.absolutePath
                            else -> null
                        }
                    if (videoPath != null) {
                        logger.lifecycle("    Manim slide replacement: slide {} → {}", seg.index, videoPath)
                        fullIdx to videoPath
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

        for ((slideIdx, videoPath) in manimSlideIndices) {
            finalDeckHtml = manimReplacer.replaceSlideAt(finalDeckHtml, slideIdx, videoPath)
        }

        if (subtitleFile != null) {
            finalDeckHtml = injectSubtitleTrack(finalDeckHtml, subtitleFile)
            modifiedDeck.writeText(finalDeckHtml)
        }

        return if (manimSlideIndices.isNotEmpty() || subtitleFile != null) {
            val replacedDeck =
                project.layout.buildDirectory
                    .dir("capsule/replaced")
                    .get()
                    .asFile
            replacedDeck.mkdirs()
            val outFile = replacedDeck.resolve(modifiedDeck.name)
            outFile.writeText(finalDeckHtml)
            outFile
        } else {
            modifiedDeck
        }
    }

    /**
     * Resolves the capture viewport (CAP-VIRAL US-2). When the `viral` section
     * is enabled, the short-form 9:16 portrait viewport (1080×1920) is used;
     * otherwise the pedagogical landscape default is preserved (backward compat).
     */
    private fun resolveViewport(): Pair<Int, Int> = CapsuleVideoResolvers.resolveViewport(capsuleExtension)

    internal fun captureDeckSequential(
        parsed: CapsuleScript,
        finalDeckFile: File,
        videoOutputDir: File,
        audioDir: File,
        outDir: File,
        slideDurations: List<Double>,
        subtitleFile: File?,
    ) {
        val (viewportWidth, viewportHeight) = resolveViewport()
        val plan =
            RevealJsRenderPlan(
                segments = parsed.segments,
                segmentDurations = slideDurations,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                parallel = false,
                captureTimeoutMillis = DEFAULT_CAPTURE_TIMEOUT_MILLIS,
            )
        val adapter = revealJsAdapter(plan, playwrightCapture?.let { { it } })
        val result =
            try {
                adapter.render(
                    VideoRenderRequest(
                        name = parsed.name,
                        visual = FileVisualSource(finalDeckFile),
                        outputDir = videoOutputDir,
                        timeoutMillis = DEFAULT_CAPTURE_TIMEOUT_MILLIS,
                    ),
                )
            } catch (e: CapturingException) {
                logger.error("Playwright capture failed for '{}': {}", parsed.name, e.message)
                throw e
            }

        if (capsuleExtension.previewOnly.get()) {
            val pngs = videoOutputDir.listFiles { f -> f.name.endsWith(".png") }?.toList().orEmpty()
            logger.lifecycle("CAPSULE PREVIEW → {} PNGs in {}", pngs.size, videoOutputDir.absolutePath)
            return
        }

        // The adapter already picks the concatenated deck video over a single
        // slide (SCREENSHOT leaves `slide-N.<ext>` next to `capsule.<ext>`, and
        // listFiles() order is unspecified).
        val generatedVideo = result.produced
        if (generatedVideo != null) {
            val finalVideo = outDir.resolve("${parsed.name}.${generatedVideo.extension}")
            generatedVideo.copyTo(finalVideo, overwrite = true)
            mixAudioWithVideo(finalVideo, audioDir, parsed.segments)
            applyAudioDescriptionIfEnabled(finalVideo, audioDir, parsed.segments)
            burnInSubtitlesIfEnabled(finalVideo, subtitleFile)
            applyAudioPostIfEnabled(finalVideo)
            val finalOutput = convertFormatIfEnabled(finalVideo)
            logger.lifecycle("CAPSULE → {}", finalOutput.absolutePath)
        } else {
            logger.warn("No video generated by Playwright capture for '{}'", parsed.name)
        }
    }

    internal fun captureDeckParallel(
        parsed: CapsuleScript,
        finalDeckFile: File,
        videoOutputDir: File,
        audioDir: File,
        outDir: File,
        subtitleFile: File?,
    ) {
        logger.lifecycle("  Parallel capture enabled for '{}' ({} slides)", parsed.name, parsed.segments.size)
        val (viewportWidth, viewportHeight) = resolveViewport()
        captureSlideParallel(
            deckHtmlPath = finalDeckFile.absolutePath,
            outputDir = videoOutputDir,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            parsed = parsed,
            audioDir = audioDir,
            captureTimeoutMillis = capsuleExtension.captureTimeoutMinutes.get().toLong() * 60_000L,
        )
        if (capsuleExtension.previewOnly.get()) {
            val pngs = videoOutputDir.listFiles { f -> f.name.endsWith(".png") }?.toList().orEmpty()
            logger.lifecycle("CAPSULE PREVIEW (parallel) → {} PNGs in {}", pngs.size, videoOutputDir.absolutePath)
            return
        }
        val concatVideo =
            listOf("webm", "mp4")
                .map { videoOutputDir.resolve("${parsed.name}.$it") }
                .firstOrNull { it.exists() }
        if (concatVideo != null) {
            val finalVideo = outDir.resolve("${parsed.name}.${concatVideo.extension}")
            concatVideo.copyTo(finalVideo, overwrite = true)
            mixAudioWithVideo(finalVideo, audioDir, parsed.segments)
            applyAudioDescriptionIfEnabled(finalVideo, audioDir, parsed.segments)
            burnInSubtitlesIfEnabled(finalVideo, subtitleFile)
            applyAudioPostIfEnabled(finalVideo)
            val produced = convertFormatIfEnabled(finalVideo)
            logger.lifecycle("CAPSULE (parallel) → {}", produced.absolutePath)
        } else {
            logger.warn("Parallel capture produced no video for '{}'", parsed.name)
        }
    }

    @TaskAction
    open fun execute() {
        val deckDir = CapsuleFactory.resolveDeckDir(project, capsuleExtension)
        val scriptDir = CapsuleFactory.resolveScriptDir(project, capsuleExtension)

        val deckFiles =
            deckDir.listFiles { f -> f.name.endsWith("-deck.html") }?.toList()
                ?: emptyList()
        val scripts = CapsuleFactory.readScriptFiles(scriptDir)

        if (deckFiles.isEmpty()) {
            logger.warn("No *-deck.html files found in {}", deckDir.absolutePath)
            logger.warn("Run 'asciidoctorRevealJs' from slider-gradle first.")
            return
        }

        if (scripts.isEmpty()) {
            logger.warn("No capsule scripts found. Run 'asciidocCapsule' from slider-gradle first.")
            return
        }

        val outDir =
            project.layout.buildDirectory
                .dir(
                    capsuleExtension.outputDir.get(),
                ).get()
                .asFile
        outDir.mkdirs()

        val engine = resolveTtsEngine()
        val manim = resolveManimEngineInternal()
        val manimMixer = resolveManimVideoMixerInternal()
        val manimReplacer = resolveManimSlideReplacerInternal()
        val manimConfig =
            ManimConfig(
                executablePath = capsuleExtension.manimExecutablePath.get(),
                quality = capsuleExtension.manimQuality.get(),
                scriptsDir = capsuleExtension.manimScriptsDir.get(),
                outputDir = capsuleExtension.manimOutputDir.get(),
            )
        val manimScriptsDir = project.file(manimConfig.scriptsDir)

        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)

            val deckFile =
                deckFiles.find { it.nameWithoutExtension.startsWith(parsed.name) }
                    ?: deckFiles.firstOrNull()
            if (deckFile == null) {
                logger.warn("No matching deck HTML found for '{}'", parsed.name)
                continue
            }

            // PRT-06 — render signature: capture engine + deck/narration content +
            // a coarse timeline fingerprint. An unchanged deck can skip a re-capture.
            val signature =
                RenderSignature.of(
                    engineFingerprint =
                        capsuleExtension.captureStrategy
                            .get()
                            .name
                            .lowercase(),
                    contentFingerprint = deckFile.readText() + "\u0001" + script.readText(),
                    timelineFingerprint = capsuleExtension.slideDurationSeconds.get().toString(),
                )

            val audioDir = outDir.resolve(parsed.name)
            audioDir.mkdirs()

            // PRT-06 — opt-in reuse: skip the whole capture when the produced video
            // is unchanged (signature) and still decodes (PRT-04).
            if (capsuleExtension.reuseCapturedVideoEnabled.getOrElse(false)) {
                val existing = findFinalVideo(outDir, parsed.name)
                if (existing != null && isReusable(existing, signature)) {
                    logger.lifecycle("CAPSULE REUSE → {} (signature unchanged, decode OK)", existing.name)
                    continue
                }
            }

            synthesizeTtsForScript(parsed, audioDir, engine)

            val modifiedDeck = injectAudio(deckFile, parsed, audioDir)
            val videoOutputDir = outDir.resolve(parsed.name).resolve("video")
            videoOutputDir.mkdirs()

            val manimDurations = mutableMapOf<Int, Double>()
            val manimOutputDir =
                project.layout.buildDirectory
                    .dir(manimConfig.outputDir)
                    .get()
                    .asFile
                    .resolve(parsed.name)
                    .resolve("manim")
            val renderedFiles = renderManimSlides(parsed, manim, manimMixer, manimScriptsDir, manimOutputDir, audioDir, manimDurations)

            val slideDurations = computeSlideDurationsWithManim(parsed, audioDir, manimDurations)

            // PRT-07 — deterministic timeline: durations snapped to integer frames.
            // Observable here (economy-of-ink reuse and capture/subtitle wiring are
            // the follow-up US of the timeline EPIC).
            val timeline =
                capsule.timeline.TimelineBuilder.build(
                    slideDurations,
                    capsule.timeline.TimelineConfig(fps = capsuleExtension.remotionFps.getOrElse(30)),
                )
            logger.lifecycle(
                "  Timeline: {} frames at {} fps ({} s) for '{}'",
                timeline.totalFrames,
                timeline.fps,
                String.format("%.1f", timeline.totalSeconds),
                parsed.name,
            )

            // Generate subtitles if enabled
            val subtitleFile: File? =
                if (capsuleExtension.subtitleEnabled.get()) {
                    generateSubtitles(parsed, slideDurations, outDir)
                } else {
                    null
                }

            val finalDeckFile =
                replaceManimSlidesInDeck(
                    parsed,
                    modifiedDeck,
                    manimOutputDir,
                    renderedFiles,
                    manimReplacer,
                    subtitleFile,
                )

            if (capsuleExtension.parallelCaptureEnabled.get()) {
                captureDeckParallel(parsed, finalDeckFile, videoOutputDir, audioDir, outDir, subtitleFile)
            } else {
                captureDeckSequential(parsed, finalDeckFile, videoOutputDir, audioDir, outDir, slideDurations, subtitleFile)
            }

            // PRT-06 — record the signature next to the produced video.
            findFinalVideo(outDir, parsed.name)?.let { produced ->
                File(produced.absolutePath + SIGNATURE_SUFFIX).writeText(signature)
            }
        }
    }

    /**
     * PRT-06 — the produced final video for [deckName] in [outDir], whatever the
     * capture engine's container (WebM for the browser engines, MP4 for Remotion).
     */
    private fun findFinalVideo(
        outDir: File,
        deckName: String,
    ): File? = listOf("webm", "mp4").map { outDir.resolve("$deckName.$it") }.firstOrNull { it.exists() }

    /**
     * PRT-06 — a video is reusable only when the stored signature matches AND the
     * file still decodes (PRT-04): a broken container keeps its signature but must
     * not be served.
     */
    private fun isReusable(
        video: File,
        signature: String,
    ): Boolean {
        val signatureFile = File(video.absolutePath + SIGNATURE_SUFFIX)
        if (!signatureFile.exists() || signatureFile.readText().trim() != signature) return false
        return VideoIntegrityProbe.probe(video, capsuleExtension.ffmpegExecutablePath.get()).isClean
    }

    internal fun generateSubtitles(
        parsed: CapsuleScript,
        slideDurations: List<Double>,
        outDir: File,
    ): File? {
        val format = SubtitleFormat.fromString(capsuleExtension.subtitleFormat.get())
        val generator = resolveSubtitleGenerator(format)
        // PRT-01 — "dit / lu": when the voice speaks the pronunciation form, the cue
        // must show the DISPLAY form. `reversals` maps the spoken words back to their
        // symbols (e.g. `pour cent` → `%`); acronyms are re-collected by
        // SubtitleTextNormalizer. Disabled by default → historical text unchanged.
        val reversals =
            if (capsuleExtension.ttsPronunciationEnabled.getOrElse(false)) {
                PronunciationLexicon.reversals()
            } else {
                emptyMap()
            }
        // PRT-03 — optional carton subdivision (one readable cue per carton);
        // default keeps the historical one-cue-per-slide behaviour.
        val entries =
            if (capsuleExtension.subtitleCartonsEnabled.getOrElse(false)) {
                SubtitleTimingCalculator.calculateFromCartons(
                    parsed.segments,
                    slideDurations,
                    capsuleExtension.subtitleMaxChars.getOrElse(SubtitleCartoner.DEFAULT_MAX_CHARS),
                    reversals,
                )
            } else {
                val displaySegments =
                    if (reversals.isEmpty()) {
                        parsed.segments
                    } else {
                        parsed.segments.map {
                            it.copy(narration = SubtitleTextNormalizer.spokenToDisplay(it.narration, reversals))
                        }
                    }
                SubtitleTimingCalculator.calculate(displaySegments, slideDurations)
            }
        val content = generator.generate(entries)
        val subtitleFile = outDir.resolve("${parsed.name}${format.fileExtension}")
        subtitleFile.writeText(content)
        logger.lifecycle("  Subtitles → {} ({} cues, {} format)", subtitleFile.name, entries.size, format.name.lowercase())
        return subtitleFile
    }

    private fun resolveSubtitleGenerator(format: SubtitleFormat): SubtitleGenerator {
        if (subtitleGenerator != null) return subtitleGenerator!!
        return CapsuleVideoResolvers.resolveSubtitleGenerator(format)
    }

    private fun resolveSubtitleBurnInService(): SubtitleBurnInService {
        if (subtitleBurnInService != null) return subtitleBurnInService!!
        return CapsuleVideoResolvers.resolveSubtitleBurnInService(capsuleExtension, logger)
    }

    internal fun injectSubtitleTrack(
        deckHtml: String,
        subtitleFile: File,
    ): String {
        val format = SubtitleFormat.fromString(capsuleExtension.subtitleFormat.get())
        val lang = HtmlEscape.escape(capsuleExtension.ttsLanguage.get())
        val src = HtmlEscape.escape(subtitleFile.name)
        val label = HtmlEscape.escape(format.name)
        val trackElement = """<track kind="captions" src="$src" srclang="$lang" label="$label captions" default>"""
        // The autoplay audio script is already injected by injectAudio — re-adding it
        // here would build a second set of Audio elements and play the narration twice.
        return deckHtml.replace(
            "</body>",
            "$trackElement\n</body>",
        )
    }

    internal fun burnInSubtitlesIfEnabled(
        videoFile: File,
        subtitleFile: File?,
    ) {
        if (!capsuleExtension.subtitleBurnIn.get()) return
        if (subtitleFile == null || !subtitleFile.exists()) {
            logger.warn("Subtitle burn-in skipped: no subtitle file available")
            return
        }

        val service = resolveSubtitleBurnInService()
        val tmpFile = File(videoFile.absolutePath + ".burnin.${videoFile.extension}")
        try {
            val burned = service.burnIn(videoFile, subtitleFile, tmpFile)
            if (burned.exists() && burned.length() > 0) {
                if (FileReplace.moveOver(burned, videoFile)) {
                    logger.lifecycle("  Subtitle burn-in: {} burned into {}", subtitleFile.name, videoFile.name)
                } else {
                    logger.error("  Subtitle burn-in: could not put '{}' in place of {} — keeping original video", burned.name, videoFile.name)
                }
            } else {
                logger.warn("  Subtitle burn-in produced empty file, keeping original video")
            }
        } catch (e: BurnInException) {
            logger.warn("  Subtitle burn-in error: {} — keeping original video", e.message)
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    /**
     * Audio post-production (CAP-AUDIO US-3). Applies BGM mix, loudness
     * normalization (EBU R128), and sidechain ducking to the final WebM,
     * *after* subtitle burn-in and *before* MP4 format conversion.
     *
     * Economy of ink (AGENT.adoc): the step is skipped entirely when both
     * [CapsuleExtension.audioPostBgmEnabled] and
     * [CapsuleExtension.audioPostDuckingEnabled] are `false` (default —
     * backward compat). When enabled, the resolved [capsule.audio.AudioPostProcessor]
     * is invoked; a `false` result (NoOp fallback, ffmpeg unavailable,
     * missing BGM file) keeps the original video (degraded mode).
     */
    internal fun applyAudioPostIfEnabled(finalVideo: File) {
        val bgmEnabled = capsuleExtension.audioPostBgmEnabled.get()
        val duckingEnabled = capsuleExtension.audioPostDuckingEnabled.get()
        val twoPassLoudnessEnabled = capsuleExtension.audioPostTwoPassLoudnessEnabled.getOrElse(false)
        if (!bgmEnabled && !duckingEnabled && !twoPassLoudnessEnabled) return

        if (!finalVideo.exists()) {
            logger.warn("Audio post: final video not found ('{}') — skipping", finalVideo.name)
            return
        }

        val ffmpegPath = capsuleExtension.ffmpegExecutablePath.get()
        val processor = CapsuleFactory.resolveAudioPostProcessor(ffmpegPath, strict = capsuleExtension.strictMode.get())
        if (processor !is NoOpAudioPostProcessor) {
            logger.lifecycle(
                "Audio post: {} (available, ffmpeg={})",
                processor.name(),
                ffmpegPath,
            )
        }

        val config =
            AudioPostConfig(
                bgmEnabled = bgmEnabled,
                bgmFile = resolveBgmFile(),
                bgmLevel = capsuleExtension.audioPostBgmLevel.get(),
                loudnessTarget = capsuleExtension.audioPostLoudnessTarget.get(),
                duckingEnabled = duckingEnabled,
                twoPassLoudnessEnabled = twoPassLoudnessEnabled,
            )

        val tmpFile = File(finalVideo.absolutePath + ".audiopost.${finalVideo.extension}")
        try {
            val success = processor.process(finalVideo, tmpFile, config)
            if (success && tmpFile.exists() && tmpFile.length() > 0) {
                if (FileReplace.moveOver(tmpFile, finalVideo)) {
                    logger.lifecycle("Audio post: BGM + loudness + ducking applied to {}", finalVideo.name)
                } else {
                    logger.error("Audio post: could not put the processed file in place of {} — keeping original video", finalVideo.name)
                }
            } else {
                logger.warn("Audio post: {} returned false — keeping original video", processor.name())
            }
        } catch (e: capsule.audio.AudioPostException) {
            logger.warn("Audio post: error '{}' — keeping original video", e.message)
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }

    /**
     * Mixes the audio description into the final video (CAP-AUDIODESC US-3).
     *
     * Opt-in ([CapsuleExtension.a11yAudioDescriptionEnabled]). The cues are read
     * from the explicit script ([AudioDescriptionScriptParser]), placed into the
     * narration silences by the pure [AudioDescriptionPlanner], synthesized by the
     * existing TTS engine, then mixed under the narration with ducking; a
     * **separate audio track** is written next to the video. Every failure
     * degrades silently to the narration-only video.
     */
    internal fun applyAudioDescriptionIfEnabled(
        finalVideo: File,
        audioDir: File,
        slides: List<SlideSegment>,
    ) {
        if (!capsuleExtension.a11yAudioDescriptionEnabled.get()) return

        val scriptFile = capsuleExtension.a11yAudioDescriptionFile.get()
        if (scriptFile.isBlank()) {
            logger.warn("Audio description: enabled but no script configured — skipping")
            return
        }
        val cues = AudioDescriptionScriptParser.load(File(scriptFile))
        if (cues.isEmpty()) {
            logger.warn("Audio description: no cue in '{}' — skipping", scriptFile)
            return
        }

        val windows = narrationWindows(audioDir, slides)
        if (windows.isEmpty()) {
            logger.warn("Audio description: no narration window — skipping")
            return
        }
        val plan = AudioDescriptionPlanner.plan(cues, windows, totalDurationSeconds = windows.last().endSeconds)
        if (plan.segments.isEmpty()) {
            logger.warn("Audio description: {} cue(s) do not fit the narration silences — skipping", plan.unplaced.size)
            return
        }

        val engine =
            runCatching { resolveTtsEngine() }
                .getOrElse { error ->
                    logger.warn("Audio description: TTS engine unavailable ({}) — skipping", error.message)
                    return
                }
        if (!engine.isAvailable()) {
            logger.warn("Audio description: TTS '{}' unavailable — skipping", engine.name())
            return
        }

        val descriptionDir = audioDir.resolve("audiodescription").also { it.mkdirs() }
        val inputs =
            plan.segments.mapIndexedNotNull { index, segment ->
                val output = descriptionDir.resolve("description-${String.format("%02d", index + 1)}.mp3")
                if (!output.exists()) {
                    runCatching { engine.synthesize(segment.text, output) }
                        .onFailure { logger.warn("Audio description: TTS failed for cue {} — skipped", index + 1) }
                }
                output.takeIf { it.isFile && it.length() > 0 }?.let { AudioDescriptionInput(it, segment.startSeconds) }
            }
        if (inputs.isEmpty()) {
            logger.warn("Audio description: no synthesized cue — narration only")
            return
        }

        val settings = AudioDescriptionMixSettings(levelDb = capsuleExtension.a11yAudioDescriptionLevelDb.get())
        val ffmpegPath = capsuleExtension.ffmpegExecutablePath.get()
        val mixed = File(finalVideo.absolutePath + ".audiodesc.${finalVideo.extension}")
        val separate = File(finalVideo.parentFile, finalVideo.nameWithoutExtension + "-audiodesc.m4a")
        val argv =
            AudioDescriptionMixCommand.buildArgv(
                videoFile = finalVideo,
                inputs = inputs,
                outputVideo = mixed,
                separateAudio = separate,
                settings = settings,
                audioCodec = AudioCodecs.forContainer(finalVideo.extension),
                ffmpegPath = ffmpegPath,
            )

        try {
            val result = ProcessRunner.run(argv)
            if (result.isSuccess && mixed.exists()) {
                if (FileReplace.moveOver(mixed, finalVideo)) {
                    logger.lifecycle(
                        "Audio description: {} cue(s) mixed, separate track → {}",
                        inputs.size,
                        separate.name,
                    )
                } else {
                    logger.error("Audio description: could not put the mixed file in place of {} — narration only", finalVideo.name)
                }
            } else {
                logger.warn("Audio description mix failed (ffmpeg exit code {}), narration only: {}", result.exitCode, result.tail(8))
            }
        } catch (e: Exception) {
            logger.warn("Audio description mix error: {} — narration only", e.message)
        } finally {
            if (mixed.exists()) mixed.delete()
        }
    }

    /**
     * Narration windows from the per-slide audio durations, in document order
     * (the video audio is the concatenation of those files — see
     * [mixAudioWithVideo]).
     */
    private fun narrationWindows(
        audioDir: File,
        slides: List<SlideSegment>,
    ): List<NarrationWindow> {
        var cursor = 0.0
        val windows = mutableListOf<NarrationWindow>()
        for (segment in slides) {
            val index = String.format("%02d", segment.index)
            val audio = audioDir.resolve("slide-$index.mp3")
            if (!audio.isFile) continue
            val duration = MediaProbeUtil.probeDuration(audio)
            if (duration <= 0.0) continue
            windows.add(NarrationWindow(cursor, cursor + duration))
            cursor += duration
        }
        return windows
    }

    /**
     * Resolves the BGM file for audio post (HF-MUSIC-LIBRARY US-6).
     *
     * Precedence: an explicit [CapsuleExtension.audioPostBgmFile] wins (backward
     * compat); otherwise a library manifest
     * ([CapsuleExtension.audioPostBgmLibraryFile]) is read and the track/mood
     * selection is resolved and copied into the build output (sha256 skip).
     * Any failure degrades silently to "no BGM" (the video is never broken).
     */
    internal fun resolveBgmFile(): String {
        val explicit = capsuleExtension.audioPostBgmFile.get()
        if (explicit.isNotBlank()) return explicit
        val libraryFile = capsuleExtension.audioPostBgmLibraryFile.get()
        if (libraryFile.isBlank()) return ""

        val manifest = File(libraryFile)
        if (!manifest.isFile) {
            logger.warn("Audio post: music library manifest not found ('{}') — keeping original", libraryFile)
            return ""
        }
        val library =
            runCatching { MusicLibraryManifestLoader.load(manifest) }
                .getOrElse { error ->
                    logger.warn("Audio post: invalid music library manifest ('{}') — keeping original", error.message)
                    return ""
                }
        val audioRoot = manifest.absoluteFile.parentFile?.absolutePath ?: ""
        val resolution =
            MusicAssetResolver.resolve(
                library = library,
                audioRoot = audioRoot,
                trackId = capsuleExtension.audioPostBgmTrack.get(),
                mood = capsuleExtension.audioPostBgmMood.get(),
            )
        return when (resolution) {
            is MusicResolution.Rejected -> {
                logger.warn("Audio post: library BGM not resolved — {}", resolution.reason)
                ""
            }
            is MusicResolution.Resolved -> copyResolvedBgm(resolution.asset)
        }
    }

    /** Copies a resolved library asset into the build output (sha256 skip). */
    private fun copyResolvedBgm(asset: ResolvedMusicAsset): String {
        val source = File(asset.sourcePath)
        if (!source.isFile) {
            logger.warn("Audio post: library audio file not found ('{}') — keeping original", asset.sourcePath)
            return ""
        }
        val targetDir = outputDir.get().asFile.resolve("assets/audio")
        targetDir.mkdirs()
        val target = targetDir.resolve(asset.targetName)
        val existingSha = target.takeIf { it.isFile }?.let { sha256(it) }
        if (MusicAssetResolver.needsCopy(existingSha, asset.sha256)) {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            logger.lifecycle("Audio post: copied library BGM '{}' → {}", asset.trackId, target.absolutePath)
        }
        return target.absolutePath
    }

    private fun sha256(file: File): String? =
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read = input.read(buffer)
                while (read >= 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrNull()

    /**
     * Post-burnIn format conversion (CAP-MP4 US-2). Dispatches on
     * [CapsuleExtension.outputFormat] via [FormatConversion.convertIfNeeded]:
     * - WEBM → no-op (backward compat).
     * - MP4  → transcode WebM→MP4 via FFmpeg, remove the WebM intermediate.
     * - BOTH → transcode and keep the WebM.
     *
     * Economy of ink: skips transcode if the MP4 already exists and probes
     * with a positive duration.
     *
     * @return the produced final video file (WebM or MP4) for logging.
     */
    internal fun convertFormatIfEnabled(finalVideo: File): File {
        val format = capsuleExtension.outputFormat.get()
        if (format == OutputFormat.WEBM) return finalVideo
        // Remotion already renders MP4; re-encoding it into MP4 would cost a full
        // transcode for nothing.
        if (format == OutputFormat.MP4 && finalVideo.extension.equals("mp4", ignoreCase = true)) {
            return finalVideo
        }
        val ffmpegPath = capsuleExtension.ffmpegExecutablePath.get()
        val converter =
            CapsuleFactory.resolveFormatConverter(
                ffmpegPath,
                strict = capsuleExtension.strictMode.get(),
                bt709 = capsuleExtension.outputBt709.getOrElse(false),
            )
        if (converter !is NoOpVideoFormatConverter) {
            logger.lifecycle("Format conversion: {} (available, ffmpeg={})", converter.name(), ffmpegPath)
        } else if (format != OutputFormat.WEBM) {
            logger.warn("Format conversion: NoOp fallback (ffmpeg unavailable), keeping WebM")
        }
        val produced =
            FormatConversion.convertIfNeeded(
                finalVideo = finalVideo,
                format = format,
                converter = converter,
                probeDuration = { f -> MediaProbeUtil.probeDuration(f) },
            )
        if (produced != finalVideo && produced.exists()) {
            logger.lifecycle("  Format conversion: {} → {}", finalVideo.name, produced.name)
        }
        return produced
    }

    internal fun captureSlideParallel(
        deckHtmlPath: String,
        outputDir: File,
        viewportWidth: Int,
        viewportHeight: Int,
        parsed: CapsuleScript,
        audioDir: File,
        captureFactory: (() -> PlaywrightCapture)? = null,
        captureTimeoutMillis: Long = DEFAULT_CAPTURE_TIMEOUT_MILLIS,
    ): Int {
        val slideDurations = computeSlideDurations(parsed, audioDir)
        val provider = captureFactory ?: playwrightCapture?.let { { it } }
        val plan =
            RevealJsRenderPlan(
                segments = parsed.segments,
                segmentDurations = slideDurations,
                viewportWidth = viewportWidth,
                viewportHeight = viewportHeight,
                parallel = true,
                captureTimeoutMillis = captureTimeoutMillis,
            )
        val adapter = revealJsAdapter(plan, provider)
        val result =
            adapter.render(
                VideoRenderRequest(
                    name = parsed.name,
                    visual = FileVisualSource(File(deckHtmlPath)),
                    outputDir = outputDir,
                    timeoutMillis = captureTimeoutMillis,
                ),
            )
        return result.failedUnits.size
    }

    internal fun injectAudio(
        deckFile: File,
        script: CapsuleScript,
        audioDir: File,
    ): File {
        val originalHtml = deckFile.readText()
        val injectedDir =
            project.layout.buildDirectory
                .dir("capsule/injected")
                .get()
                .asFile
        injectedDir.mkdirs()

        val audioPathByIndex =
            script.segments.associate { seg ->
                seg.index to audioDir.resolve("slide-${String.format("%02d", seg.index)}.mp3").absolutePath
            }
        val hasAudio = audioPathByIndex.values.any { File(it).exists() }

        val outFile = injectedDir.resolve(deckFile.name)
        if (!hasAudio) {
            outFile.writeText(originalHtml)
            return outFile
        }

        // The reveal.js-specific HTML manipulation lives in the adapter
        // (CAP-ENGINE-DECOUPLING US-2). The sequential fallback rewrites the deck
        // from scratch: building the `data-capsule-slide` version first and
        // discarding it was wasted work, and re-read the file a second time.
        val injectedHtml =
            if (!RevealJsDeckHtml.hasDataCapsuleSlide(originalHtml)) {
                val orderedPaths = script.segments.map { audioPathByIndex.getValue(it.index) }
                val withAudio = RevealJsDeckHtml.injectSequentialAudio(originalHtml, orderedPaths)
                RevealJsDeckHtml.injectAudioScript(withAudio, markerSuffix = "sequential fallback")
            } else {
                val withAudio = RevealJsDeckHtml.injectDataSlideAudio(originalHtml, audioPathByIndex)
                RevealJsDeckHtml.injectAudioScript(withAudio)
            }

        outFile.writeText(injectedHtml)
        return outFile
    }

    private fun mixAudioWithVideo(
        videoFile: File,
        audioDir: File,
        slides: List<SlideSegment>,
    ) {
        val mp3Files =
            slides.mapNotNull { seg ->
                val idx = String.format("%02d", seg.index)
                val f = audioDir.resolve("slide-$idx.mp3")
                f.takeIf { it.exists() }
            }
        if (mp3Files.isEmpty()) return

        val cmd = mutableListOf(capsuleExtension.ffmpegExecutablePath.get(), "-y", "-i", videoFile.absolutePath)
        val concatInputs = mutableListOf<String>()

        for ((i, mp3) in mp3Files.withIndex()) {
            val inputIdx = i + 1
            cmd.addAll(listOf("-i", mp3.absolutePath))
            concatInputs.add("[$inputIdx:a]")
        }

        val filterComplex = "${concatInputs.joinToString("")}concat=n=${mp3Files.size}:v=0:a=1[aout]"
        // Vorbis has no place in an MP4 and AAC none in a WebM: the container the
        // capture engine produced decides the audio codec.
        val audioCodec = AudioCodecs.forContainer(videoFile.extension)
        cmd.addAll(listOf("-filter_complex", filterComplex, "-map", "0:v", "-map", "[aout]", "-c:v", "copy", "-c:a", audioCodec, "-shortest"))

        val tmpFile = File(videoFile.absolutePath + ".tmp.${videoFile.extension}")
        cmd.add(tmpFile.absolutePath)

        try {
            val result = ProcessRunner.run(cmd)
            if (result.isSuccess && tmpFile.exists()) {
                if (!FileReplace.moveOver(tmpFile, videoFile)) {
                    logger.error("  Audio mix: could not put the mixed file in place of {} — video remains silent", videoFile.name)
                    return
                }
                val totalSlides = mp3Files.size
                val audioDur =
                    mp3Files.sumOf { f ->
                        MediaProbeUtil.probeDuration(f)
                    }
                logger.lifecycle(
                    "  Audio mix: {} slides concatenated (audio={}s, video={}s)",
                    totalSlides,
                    String.format("%.1f", audioDur),
                    String.format("%.1f", MediaProbeUtil.probeDuration(videoFile)),
                )
            } else {
                logger.warn("  Audio mix failed (ffmpeg exit code {}), video remains silent: {}", result.exitCode, result.tail(8))
            }
        } catch (e: Exception) {
            logger.warn("  Audio mix error: {} — video remains silent", e.message)
        } finally {
            if (tmpFile.exists()) tmpFile.delete()
        }
    }
}
