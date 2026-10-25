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
        project.registerPublishCapsuleToYouTubeTask()
    }

    private fun Project.registerPublishCapsuleToYouTubeTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("publishCapsuleToYouTube", capsule.publish.PublishCapsuleToYouTubeTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.deploy", lang)
            task.description = "Publish the produced MP4 to YouTube (OAuth 2.0; opt-in via publish.enabled)"
            val configFile = File(projectDir, "capsule-context.yml")
            val config = CapsuleConfigLoader.load(configFile)
            // 4-source precedence (ENV < props < YAML < CLI): an ABSENT YAML must
            // not shadow gradle.properties with its defaults (e.g. publish.enabled
            // stayed false even when `capsule.publish.enabled=true` was set).
            val merged = CapsuleConfigMerger.merge(projectDir, config, emptyMap(), yamlLoaded = configFile.exists())
            val publish = merged.publish
            val extension = extensions.getByType(CapsuleExtension::class.java)
            task.videoFiles.from(
                layout.buildDirectory.dir(extension.outputDir).map { dir ->
                    dir.asFileTree.matching { it.include("**/*.mp4") }
                },
            )
            task.outputDir.set(layout.buildDirectory.dir("capsule/publish"))
            task.publishEnabled.set(publish.enabled)
            task.platform.set(publish.platform)
            task.credentialsFile.set(publish.credentialsFile)
            task.title.set(publish.title)
            task.publishDescription.set(publish.description)
            task.tags.set(publish.tags)
            task.hashtags.set(publish.hashtags)
            task.links.set(publish.links)
            task.nextVideo.set(publish.nextVideo)
            task.previousVideo.set(publish.previousVideo)
            task.thumbnailFile.set(publish.thumbnailFile)
            task.visibility.set(publish.visibility)
            task.categoryId.set(publish.categoryId)
            task.languageCode.set(publish.languageCode)
            task.publishAt.set(publish.publishAt)
            task.chapters.set(emptyList<String>())
            task.strict.set(publish.strict)
        }
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
            val configFile = File(project.projectDir, "capsule-context.yml")
            val config = CapsuleConfigLoader.load(configFile)
            val merged = CapsuleConfigMerger.merge(
                project.projectDir,
                config,
                emptyMap(),
                yamlLoaded = configFile.exists(),
            )
            task.videoDestinationDir.set(merged.output.videoDestinationDir)
            task.versioning.set(merged.output.versioning.name)
            task.versionPrefix.set(merged.output.versionPrefix)
            task.format.set(merged.output.format.name)
        }
    }
}
