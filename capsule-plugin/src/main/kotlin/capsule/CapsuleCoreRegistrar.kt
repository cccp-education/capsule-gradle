package capsule

import capsule.i18n.CapsuleMessages
import org.gradle.api.Project
import java.io.File

/**
 * Registrar of the core production tasks (CAP-DECOMPOSE BS-4).
 *
 * Pure extraction from [CapsuleManager]: the seven `register*` functions below were
 * moved verbatim (no logic change) so `CapsuleManager.registerTasks()` becomes a thin
 * orchestrator. Mirrors the pattern proven by slider `SLD-6` and the in-house
 * `CapsuleViralRegistrar` / `CapsuleQcRegistrar`.
 */
internal object CapsuleCoreRegistrar {

    /** Registers every core production task on [project]. */
    fun register(project: Project) {
        project.registerExtractSpeakerNotesTask()
        project.registerGenerateCapsuleScriptTask()
        project.registerGenerateCapsuleTask()
        project.registerGenerateCapsuleVideoTask()
        project.registerGenerateCapsuleVideoAllLanguagesTask()
        project.registerDeployCapsuleTask()
        project.registerDistributeCapsuleVideoTask()
    }

    private fun Project.registerExtractSpeakerNotesTask() {
        capsule.feed.CapsuleFeedTaskRegistrar.register(this)
        capsule.feed.CapsuleFeedTaskRegistrar.registerTranslateAndExtractSpeakerNotes(this)
        capsule.feed.CapsuleFeedTaskRegistrar.registerTranslateAndGenerateCapsuleVideos(this)
        capsule.feed.CapsuleFeedTaskRegistrar.registerGenerateCapsuleContentAndVideos(this)
    }

    private fun Project.registerGenerateCapsuleScriptTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("generateCapsuleScript", CapsuleScriptTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleScript.description", lang)
            task.dependsOn(capsule.feed.CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES)
        }
    }

    private fun Project.registerGenerateCapsuleTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("generateCapsule", CapsuleBuildTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsule.description", lang)
            task.dependsOn("generateCapsuleScript")
        }
    }

    private fun Project.registerGenerateCapsuleVideoTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleVideo.description", lang)
            task.dependsOn("generateCapsule")
        }
    }

    private fun Project.registerGenerateCapsuleVideoAllLanguagesTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register(
            "generateCapsuleVideoAllLanguages",
            capsule.multilang.GenerateCapsuleVideoAllLanguagesTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleVideoAllLanguages.description", lang)
        }
    }

    private fun Project.registerDeployCapsuleTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("deployCapsule", CapsuleDistribTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.deploy", lang)
            task.description = CapsuleMessages.get("task.deployCapsule.description", lang)
            task.dependsOn("generateCapsuleVideo")
        }
    }

    private fun Project.registerDistributeCapsuleVideoTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("distributeCapsuleVideo", DistributeCapsuleVideoTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.distribute", lang)
            task.description = CapsuleMessages.get("task.distributeCapsuleVideo.description", lang)
            val config = CapsuleConfigLoader.load(File(project.projectDir, "capsule-context.yml"))
            val merged = CapsuleConfigMerger.merge(project.projectDir, config, emptyMap())
            task.videoDestinationDir.set(merged.output.videoDestinationDir)
            task.versioning.set(merged.output.versioning.name)
            task.versionPrefix.set(merged.output.versionPrefix)
            task.format.set(merged.output.format.name)
        }
    }
}
