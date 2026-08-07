package capsule.multilang

import capsule.CapsuleManager
import capsule.CapsuleVideoTask
import capsule.MediaProbeUtil
import capsule.feed.CapsuleScriptReader
import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task that generates one localized capsule WebM per target language.
 *
 * Builds a [CapsuleVideoPlan] via [CapsuleVideoPlanner] from the translated
 * deck + script artifacts produced upstream by the slider pipeline
 * (`translateDeck` → `extractSpeakerNotes`), then delegates the iteration to
 * the pure [CapsuleVideoAllLanguagesRunner] which applies the Economy of Ink
 * rule (skip languages whose WebM already exists and probes valid).
 *
 * Each entry runs the existing single-language pipeline
 * ([CapsuleVideoTask.synthesizeTtsForScript] + [CapsuleVideoTask.captureDeckSequential]
 * / [CapsuleVideoTask.captureDeckParallel]) with the TTS language, Piper model
 * and espeak voice adapted to the entry language.
 *
 * Thin adapter: orchestration (plan building + skip decision) lives in the
 * pure `capsule.multilang` domain, filesystem-bound work reuses the proven
 * [CapsuleVideoTask] pipeline (CR-1 refactor).
 */
@DisableCachingByDefault(because = "Filesystem-bound: captures one WebM per language via TTS + Playwright")
open class GenerateCapsuleVideoAllLanguagesTask : CapsuleVideoTask() {

    @TaskAction
    override fun execute() {
        val deckDir = CapsuleManager.resolveDeckDir(project, capsuleExtension)
        val scriptDir = CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val outDir = project.layout.buildDirectory.dir(capsuleExtension.outputDir.get()).get().asFile
        outDir.mkdirs()

        val targetLanguages = resolveTargetLanguages()
        if (targetLanguages.isEmpty()) {
            logger.warn("No supported target languages in LanguageCatalog — nothing to do.")
            return
        }

        val plan = CapsuleVideoPlanner.plan(deckDir, scriptDir, outDir, targetLanguages)

        val rendered = CapsuleVideoAllLanguagesRunner.run(
            plan = plan,
            probeDuration = MediaProbeUtil::probeDuration,
            render = { entry -> renderEntry(entry) },
        )

        logger.lifecycle(
            "CAPSULE MULTILANG → {}/{} languages rendered ({} skipped: existing valid WebM)",
            rendered.size,
            plan.size(),
            plan.size() - rendered.size,
        )
    }

    private fun resolveTargetLanguages(): List<SupportedLanguage> =
        LanguageCatalog.supportedCodes()
            .mapNotNull { code -> LanguageCatalog.findByCode(code) }

    private fun renderEntry(entry: CapsuleVideoEntry) {
        val resolved = MultiLanguageResolver.resolve(entry.language.code)
        if (resolved == null) {
            logger.warn("No TTS voice mapping for '{}' — skipping", entry.language.code)
            return
        }

        capsuleExtension.ttsLanguage.set(entry.language.code)
        capsuleExtension.ttsVoice.set(resolved.piperModel)
        capsuleExtension.espeakVoice.set(resolved.espeakVoice)
        ttsEngine = null

        val parsed = CapsuleScriptReader.read(entry.scriptFile)
        if (parsed.isEmpty) {
            logger.warn("Script '{}' carries no slides — skipping", entry.scriptFile.name)
            return
        }

        val outDir = entry.outputVideo.parentFile
        val audioDir = outDir.resolve(parsed.deckName)
        audioDir.mkdirs()

        val engine = resolveTtsEngine()
        synthesizeTtsForScript(parsed, audioDir, engine)

        val modifiedDeck = injectAudio(entry.deckFile, parsed, audioDir)
        val videoOutputDir = audioDir.resolve("video")
        videoOutputDir.mkdirs()

        val slideDurations = computeSlideDurations(parsed, audioDir)

        val subtitleFile: File? = if (capsuleExtension.subtitleEnabled.get()) {
            generateSubtitles(parsed, slideDurations, outDir)
        } else null

        if (capsuleExtension.parallelCaptureEnabled.get()) {
            captureDeckParallel(parsed, modifiedDeck, videoOutputDir, audioDir, outDir, subtitleFile)
        } else {
            captureDeckSequential(parsed, modifiedDeck, videoOutputDir, audioDir, outDir, slideDurations, subtitleFile)
        }
    }
}
