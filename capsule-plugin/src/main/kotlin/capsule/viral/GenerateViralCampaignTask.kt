package capsule.viral

import capsule.ai.CapsuleLlmService.aiProvider
import capsule.ai.CapsuleLlmService.resolveModel
import codebase.koog.llm.service.LlmBuildService
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `generateViralCampaign` (CAP-VIRAL wiring).
 *
 * Drives the pure [ViralCampaignAssembler] from a **validated storyboard**
 * (US-13) and the campaign configuration: it gates on the storyboard, resolves
 * the context-anchored hook (US-4, LLM — or a configured hook, economy of ink),
 * plans the (language × platform) batch (US-6), reads the market copy (US-9,
 * never re-generated) and writes the [CampaignBundle] manifest (US-10) that the
 * distribution layer (GBL-002) consumes.
 *
 * This is the missing Gradle entry point: the domain was assembled in S-141
 * (`ViralCampaignAssembler`), but no task piloted it. The task is a **thin
 * adapter** — all decisions live in the `capsule.viral` domain.
 *
 * Inputs:
 *   - [storyboardFile]       validated storyboard AsciiDoc (US-13 gate).
 *   - [augmentedContextFile] `build/capsule/augmented-context.txt` (N1 context,
 *                            optional — anchors the hook generation).
 *   - [copyFiles]            bakery copy pieces (US-9, read via
 *                            [FileMarketCopySource]); empty → degraded.
 *   - [languages]/[platforms]/[targetDurationSeconds] the batch matrix.
 *   - [configuredHook]/[configuredCta] explicit hook (skips the LLM).
 *
 * Outputs:
 *   - [storyboardOutput] the deterministic storyboard document (reviewable).
 *   - [deckOutput]       the capturable portrait 1080×1920 HTML deck
 *                        (CAP-SHORT US-1a — one `<section>` per beat).
 *   - [manifestOutput]   `viral-campaign.json` (list of [CampaignBundle]).
 *
 * Economy of ink: skipped when [viralEnabled] is false (default); UP-TO-DATE
 * when the declared inputs/outputs are unchanged.
 *
 * Usage:
 *   ./gradlew generateViralCampaign -Pcapsule.viral.enabled=true \
 *     -Pcapsule.viral.storyboardFile=storyboard.adoc
 */
@DisableCachingByDefault(because = "Viral campaign assembly — reads a storyboard + copy, may call the LLM")
abstract class GenerateViralCampaignTask : DefaultTask() {

    /** Master switch — when false (default), the task is a no-op skip. */
    @get:Input
    abstract val viralEnabled: Property<Boolean>

    /** When true, a validated storyboard is mandatory (editorial gate). */
    @get:Input
    abstract val storyboardRequired: Property<Boolean>

    /** Target platform preset name (`YOUTUBE_SHORT`/`TIKTOK`/`REELS`). */
    @get:Input
    abstract val platform: Property<String>

    /** Short-form duration in [15, 60]. */
    @get:Input
    abstract val targetDurationSeconds: Property<Int>

    /** Explicit hook — when non-blank, the LLM step is skipped (economy of ink). */
    @get:Input
    abstract val configuredHook: Property<String>

    /** Explicit call-to-action, used with [configuredHook]. */
    @get:Input
    abstract val configuredCta: Property<String>

    /** Target languages of the batch matrix. */
    @get:Input
    abstract val languages: ListProperty<String>

    /** Target platforms of the batch matrix (names). */
    @get:Input
    abstract val platforms: ListProperty<String>

    /** Validated storyboard AsciiDoc (US-13). */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val storyboardFile: RegularFileProperty

    /** Rendered N1 augmented context (optional anchor for the hook). */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val augmentedContextFile: RegularFileProperty

    /** bakery copy pieces (US-9) — read, never generated. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val copyFiles: ConfigurableFileCollection

    /** Directory holding the per-variant rendered videos (path prefix). */
    @get:Input
    abstract val renderedVideoDir: Property<String>

    /** Deterministic storyboard document output. */
    @get:OutputFile
    abstract val storyboardOutput: RegularFileProperty

    /**
     * Capturable portrait 1080×1920 HTML deck output (CAP-SHORT US-1a). Derived
     * from the beats by [ViralDeckBuilder]; the input the FFmpeg screenshot socle
     * (US-1b) will capture.
     */
    @get:OutputFile
    abstract val deckOutput: RegularFileProperty

    /** `viral-campaign.json` — the [CampaignBundle] manifest output. */
    @get:OutputFile
    abstract val manifestOutput: RegularFileProperty

    @get:ServiceReference
    abstract val llmService: Property<LlmBuildService>

    @TaskAction
    fun run() {
        if (!viralEnabled.get()) {
            logger.lifecycle("CAPSULE VIRAL → skipped (viralEnabled=false)")
            return
        }

        val storyboard = resolveStoryboard()
        val verdict = ViralStoryboardValidator.validate(storyboard)
        if (verdict is StoryboardValidationResult.Invalid) {
            error(
                "CAPSULE VIRAL → storyboard rejected by the editorial gate: " +
                    verdict.reasons.joinToString("; ") +
                    ". No campaign is rendered before a validated storyboard.",
            )
        }

        val hook = resolveHook(storyboard)

        val platforms = resolvePlatforms()
        val variants = ViralBatchPlanner.plan(
            languages = languages.get(),
            platforms = platforms,
            duration = targetDurationSeconds.get(),
        )

        val copySource = FileMarketCopySource(firstCopyDir())
        val videoDir = File(project.projectDir, renderedVideoDir.get())
        val bundles = ViralCampaignAssembler.assemble(
            storyboard = storyboard,
            variants = variants,
            hook = hook,
            copySource = copySource,
            videoPathFor = { variant -> File(videoDir, "${variant.id}.mp4").absolutePath },
        )

        val storyboardOut = storyboardOutput.get().asFile
        storyboardOut.parentFile.mkdirs()
        storyboardOut.writeText(ViralStoryboardBuilder.build(storyboard))

        val deckOut = deckOutput.get().asFile
        deckOut.parentFile.mkdirs()
        deckOut.writeText(ViralDeckBuilder.build(storyboard))

        val manifestOut = manifestOutput.get().asFile
        manifestOut.parentFile.mkdirs()
        manifestOut.writeText(CampaignBundleSerializer.toManifestJson(bundles))

        logger.lifecycle(
            "CAPSULE VIRAL → ${bundles.size} bundle(s) " +
                "(${languages.get().joinToString(",")} × ${platforms.joinToString(",") { it.name }}) " +
                "→ ${manifestOut.absolutePath}",
        )
    }

    /**
     * Resolves the validated storyboard. When [storyboardFile] is present it is
     * parsed ([ViralStoryboardParser]); otherwise a storyboard is synthesized
     * from the configured hook/cta — only when [storyboardRequired] is false.
     * A required-but-missing storyboard fails the editorial gate.
     */
    private fun resolveStoryboard(): ViralStoryboard {
        val file = storyboardFile.orNull?.asFile
        if (file != null && file.exists()) {
            return ViralStoryboardParser.parse(file.readText())
                ?: error(
                    "CAPSULE VIRAL → storyboard file '${file.path}' is not a validable storyboard " +
                        "(expected the '= Storyboard — <message>' header, beats and the short-form duration).",
                )
        }
        if (storyboardRequired.get()) {
            error(
                "CAPSULE VIRAL → no validated storyboard (set -Pcapsule.viral.storyboardFile=<path>) " +
                    "and storyboardRequired=true. No campaign without an approved storyboard.",
            )
        }
        return synthesizeStoryboard()
    }

    /** Builds a minimal storyboard from the configured hook/cta (gate disabled). */
    private fun synthesizeStoryboard(): ViralStoryboard {
        val hook = configuredHook.get().ifBlank {
            error("CAPSULE VIRAL → storyboardRequired=false but no hook configured to synthesize a storyboard.")
        }
        val cta = configuredCta.get().ifBlank { hook }
        val duration = targetDurationSeconds.get()
            .coerceIn(ViralConfig.MIN_DURATION_SECONDS, ViralConfig.MAX_DURATION_SECONDS)

        // The synthesized storyboard must satisfy the editorial validator:
        // no beat exceeds MAX_BEAT_SECONDS, and the summed beats stay within
        // ±20% of the target. Bracket with a short hook + cta and split the
        // remainder into development beats of at most MAX_BEAT_SECONDS each.
        val hookSeconds = HOOK_SECONDS.coerceAtMost(duration.toDouble())
        val ctaSeconds = CTA_SECONDS.coerceAtMost((duration - hookSeconds).coerceAtLeast(0.0))
        val developmentTotal = (duration - hookSeconds - ctaSeconds).coerceAtLeast(0.0)
        val beats = mutableListOf(StoryboardBeat(role = "hook", intent = hook, durationSeconds = hookSeconds))
        if (developmentTotal > 0.0) {
            val count = kotlin.math.ceil(developmentTotal / ViralStoryboardValidator.MAX_BEAT_SECONDS).toInt().coerceAtLeast(1)
            val per = developmentTotal / count
            repeat(count) { beats += StoryboardBeat(role = "development", intent = hook, durationSeconds = per) }
        }
        beats += StoryboardBeat(role = "cta", intent = cta, durationSeconds = ctaSeconds)

        return ViralStoryboard(
            message = hook,
            angle = cta,
            beats = beats,
            platform = ViralPlatform.fromString(platform.get()),
            language = languages.get().firstOrNull()?.takeIf { it.isNotBlank() } ?: "fr",
            targetDurationSeconds = duration,
        )
    }

    /**
     * Resolves the context-anchored hook (US-4): an explicit [configuredHook]
     * short-circuits the LLM (economy of ink); otherwise the LLM is invoked with
     * the N1 augmented context. A blank/unusable LLM answer degrades to `null`
     * (the assembler falls back to the storyboard message).
     */
    private fun resolveHook(storyboard: ViralStoryboard): ViralHook? {
        val explicit = configuredHook.get()
        if (explicit.isNotBlank()) {
            return ViralHook(hook = explicit, message = storyboard.message, cta = configuredCta.get())
        }
        val plan = ViralHookPlan(
            deckName = storyboard.message.take(40),
            segments = emptyList(),
            language = storyboard.language,
            platform = storyboard.platform,
            targetDurationSeconds = storyboard.targetDurationSeconds,
            augmentedContext = augmentedContext,
            outputPath = manifestOutput.get().asFile.absolutePath,
        )
        val provider = project.aiProvider
        val model = project.resolveModel(provider, llmService)
        return ChatModelViralHookGenerator(model).generate(plan)
    }

    /** Reads the optional N1 augmented context file (blank when absent). */
    private val augmentedContext: String
        get() {
            val file = augmentedContextFile.orNull?.asFile ?: return ""
            return if (file.exists()) file.readText() else ""
        }

    /** Matrix platforms: the CSV list, falling back to the single configured preset. */
    private fun resolvePlatforms(): List<ViralPlatform> {
        val configured = platforms.get().mapNotNull { name ->
            name.takeIf { it.isNotBlank() }?.let { ViralPlatform.fromString(it) }
        }.distinct()
        return configured.ifEmpty { listOf(ViralPlatform.fromString(platform.get())) }
    }

    /** Directory holding bakery copy pieces (first resolved file's parent, or blank). */
    private fun firstCopyDir(): File =
        copyFiles.files.firstOrNull()?.parentFile ?: File(project.projectDir, "build/capsule/viral/copy")

    private companion object {
        /** Synthesized hook beat duration (seconds) — within MAX_BEAT_SECONDS. */
        const val HOOK_SECONDS: Double = 3.0

        /** Synthesized closing call-to-action beat duration (seconds). */
        const val CTA_SECONDS: Double = 3.0
    }
}
