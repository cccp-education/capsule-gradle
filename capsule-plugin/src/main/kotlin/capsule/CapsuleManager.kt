package capsule

import capsule.ai.CapsuleLlmService.registerLlmBuildService
import org.gradle.api.Project
import java.io.File

class CapsuleManager(private val project: Project) {

    fun registerTasks() {
        project.registerExtractSpeakerNotesTask()
        project.registerGenerateCapsuleScriptTask()
        project.registerGenerateCapsuleTask()
        project.registerGenerateCapsuleVideoTask()
        project.registerGenerateCapsuleVideoAllLanguagesTask()
        project.registerDeployCapsuleTask()
        project.registerCollectCapsuleContextTask()
        project.registerTransformCapsuleContextTask()
        project.registerScaffoldCapsuleContextTask()
        project.registerAiSmokeTestTask()
        project.registerCollectAugmentedContextTask()
    }

    private fun Project.registerExtractSpeakerNotesTask() {
        capsule.feed.CapsuleFeedTaskRegistrar.register(this)
        capsule.feed.CapsuleFeedTaskRegistrar.registerTranslateAndExtractSpeakerNotes(this)
        capsule.feed.CapsuleFeedTaskRegistrar.registerTranslateAndGenerateCapsuleVideos(this)
    }

    private fun Project.registerGenerateCapsuleScriptTask() {
        tasks.register("generateCapsuleScript", CapsuleScriptTask::class.java) { task ->
            task.group = "generate"
            task.description = "Reads *-script.txt produced by extractSpeakerNotes and validates the capsule script"
            task.dependsOn(capsule.feed.CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES)
        }
    }

    private fun Project.registerGenerateCapsuleTask() {
        tasks.register("generateCapsule", CapsuleBuildTask::class.java) { task ->
            task.group = "generate"
            task.description = "Generates TTS audio files from capsule scripts (Piper placeholder)"
            task.dependsOn("generateCapsuleScript")
        }
    }

    private fun Project.registerGenerateCapsuleVideoTask() {
        tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java) { task ->
            task.group = "generate"
            task.description = "Injects TTS audio into deck HTML then captures video via Playwright Java"
            task.dependsOn("generateCapsule")
        }
    }

    private fun Project.registerGenerateCapsuleVideoAllLanguagesTask() {
        tasks.register(
            "generateCapsuleVideoAllLanguages",
            capsule.multilang.GenerateCapsuleVideoAllLanguagesTask::class.java,
        ) { task ->
            task.group = "generate"
            task.description = "Generates one localized capsule WebM per target language from translated decks + scripts"
        }
    }

    private fun Project.registerDeployCapsuleTask() {
        tasks.register("deployCapsule", CapsuleDistribTask::class.java) { task ->
            task.group = "deploy"
            task.description = "Recadre les capsules en format vertical 9:16 (TikTok/Shorts) via FFmpeg"
            task.dependsOn("generateCapsuleVideo")
        }
    }

    private fun Project.registerCollectCapsuleContextTask() {
        tasks.register("collectCapsuleContext", CapsuleCompositeContextTask::class.java) { task ->
            task.group = "collect"
            task.description = "Exporte le contexte des capsules (chemins videos + metadonnees) en JSON compatible engine N3"
            task.dependsOn("deployCapsule")
        }
    }

    private fun Project.registerTransformCapsuleContextTask() {
        tasks.register("transformCapsuleContext", CapsuleParseContextTask::class.java) { task ->
            task.group = "transform"
            task.description = "Parse le fichier capsule-context.json et retourne une liste de decks"
            task.contextFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-context.json")
            )
            task.outputFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-parse-results.json")
            )
        }

        tasks.register("collectCapsuleRetrieve", CapsuleParseContextTask::class.java) { task ->
            task.group = "collect"
            task.description = "Retrieve capsule decks from capsule-context.json (N3 engine contract)"
            val outputFile = project.findProperty("outputFile") as? String
            if (outputFile != null) {
                task.outputFile.set(File(outputFile))
            }
            task.contextFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-context.json")
            )
        }
    }

    private fun Project.registerScaffoldCapsuleContextTask() {
        tasks.register("scaffoldCapsuleContext", CapsuleScaffoldTask::class.java) { task ->
            task.group = "generate"
            task.description = "Scaffolds a default capsule-context.yml configuration file with comments"
        }
    }

    private fun Project.registerAiSmokeTestTask() {
        val llmServiceProvider = registerLlmBuildService()
        tasks.register("capsuleAiSmokeTest", capsule.ai.CapsuleAiSmokeTestTask::class.java) { task ->
            task.group = "generate"
            task.description = "Smoke-tests the codebase LLM bridge (LlmBuildService + ChatModel adapter) with a minimal prompt"
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }
    }

    private fun Project.registerCollectAugmentedContextTask() {
        tasks.register(
            "collectCapsuleAugmentedContext",
            capsule.context.CollectCapsuleAugmentedContextTask::class.java,
        ) { task ->
            task.group = "collect"
            task.description = "Collects the augmented context (EAGER governance + RAG + Graphify + Docs) and renders it for content generation"
            task.eagerFiles.from(
                project.layout.projectDirectory.file(".agents/INDEX.adoc"),
                project.layout.projectDirectory.file("PROMPT_REPRISE.adoc"),
                project.layout.projectDirectory.file("AGENT.adoc"),
            )
            task.ragContent.set(project.findProperty("context.ragContent")?.toString().orEmpty())
            task.graphifyContent.set(project.findProperty("context.graphifyContent")?.toString().orEmpty())
            task.docsContent.set(project.findProperty("context.docsContent")?.toString().orEmpty())
            task.tokenBudget.set(
                project.findProperty("context.tokenBudget")?.toString()?.toIntOrNull()
                    ?: contracts.context.ContextChannel.DEFAULT_TOKEN_BUDGET
            )
            task.outputFile.set(project.layout.buildDirectory.file("capsule/augmented-context.txt"))
        }
    }

    companion object {
        /**
         * Resolves the appropriate ManimVideoMixer based on ffmpeg availability.
         * - If ffmpeg is not available, returns NoOpManimVideoMixer
         * - Otherwise, returns ManimVideoMixerImpl
         */
        @JvmStatic
        fun resolveManimVideoMixer(ffmpegPath: String = "ffmpeg"): ManimVideoMixer {
            if (ffmpegPath == "noop") return NoOpManimVideoMixer()
            val mixer = ManimVideoMixerImpl(ffmpegPath)
            return if (mixer.isAvailable()) mixer else NoOpManimVideoMixer()
        }

        /**
         * Resolves the appropriate ManimSlideReplacer.
         * Always returns ManimSlideReplacerImpl (always available, pure HTML manipulation).
         */
        @JvmStatic
        fun resolveManimSlideReplacer(): ManimSlideReplacer {
            return ManimSlideReplacerImpl()
        }

        /**
         * Resolves the appropriate ManimEngine based on configuration.
         * - If executablePath is "noop", returns NoOpManimEngine
         * - Otherwise, creates ManimEngineImpl(config) and falls back to NoOpManimEngine if unavailable
         */
        @JvmStatic
        fun resolveManimEngine(config: ManimConfig): ManimEngine {
            if (config.executablePath == "noop") {
                return NoOpManimEngine()
            }
            val engine = ManimEngineImpl(config)
            return if (engine.isAvailable()) engine else NoOpManimEngine()
        }

        /**
         * Resolves the appropriate ManimParallelRenderer based on parallelism.
         * - parallelism = 1: NoOpManimParallelRenderer (sequential fallback)
         * - parallelism > 1: ManimParallelRendererImpl with thread pool
         */
        @JvmStatic
        fun resolveManimParallelRenderer(parallelism: Int = 4): ManimParallelRenderer {
            return if (parallelism <= 1) {
                NoOpManimParallelRenderer()
            } else {
                ManimParallelRendererImpl(parallelism)
            }
        }

        /**
         * Resolves the appropriate SubtitleBurnInService based on ffmpeg availability.
         * - If ffmpegPath is "noop", returns NoOpSubtitleBurnInService
         * - Otherwise, returns SubtitleBurnInServiceImpl if ffmpeg is available
         */
        @JvmStatic
        fun resolveSubtitleBurnInService(ffmpegPath: String = "ffmpeg", style: SubtitleBurnInStyle = SubtitleBurnInStyle()): SubtitleBurnInService {
            if (ffmpegPath == "noop") return NoOpSubtitleBurnInService()
            val service = SubtitleBurnInServiceImpl(ffmpegPath, style)
            return if (service.isAvailable()) service else NoOpSubtitleBurnInService()
        }

        fun readScriptFiles(dir: File): List<File> {
            return dir.listFiles { f ->
                f.name.endsWith("-script.txt") &&
                !f.name.startsWith("example-") &&
                !f.name.contains("-context-")
            }
                ?.toList() ?: emptyList()
        }

        fun resolveScriptDir(project: Project, capsuleExt: CapsuleExtension): File {
            val configured = capsuleExt.sliderScriptDir.get()
            val candidate = project.layout.buildDirectory.dir(configured).get().asFile
            if (candidate.exists() && candidate.listFiles()
                    ?.any { it.name.endsWith("-script.txt") } == true
            ) {
                return candidate
            }
            val sliderOutput = project.rootProject.projectDir.parentFile
                ?.resolve("slider-plugin")
                ?.resolve("slider")
                ?.resolve("build")
                ?.resolve("capsule")
            if (sliderOutput != null && sliderOutput.exists()) return sliderOutput
            return candidate
        }

        fun resolveDeckDir(project: Project, capsuleExt: CapsuleExtension): File {
            val configured = capsuleExt.deckSourceDir.get()
            val candidate = project.layout.buildDirectory.dir(configured).get().asFile
            if (candidate.exists()) return candidate
            val sliderOutput = project.rootProject.projectDir.parentFile
                ?.resolve("slider-plugin")
                ?.resolve("slider")
                ?.resolve("build")
                ?.resolve("docs")
                ?.resolve("asciidocRevealJs")
            if (sliderOutput != null && sliderOutput.exists()) return sliderOutput
            return candidate
        }
    }
}
