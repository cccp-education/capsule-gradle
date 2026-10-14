package capsule

import capsule.i18n.CapsuleMessages
import org.gradle.api.Project

/**
 * Registrar of the podcast task (CAP-DECOMPOSE BS-4).
 *
 * Pure extraction from [CapsuleManager]: the `registerGeneratePodcastTask` function
 * below was moved verbatim (no logic change). Mirrors the pattern proven by slider `SLD-6`.
 */
internal object CapsulePodcastRegistrar {

    /** Registers the podcast task on [project]. */
    fun register(project: Project) {
        project.registerGeneratePodcastTask()
    }

    private fun Project.registerGeneratePodcastTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register(
            "generateCapsulePodcast",
            capsule.podcast.GenerateCapsulePodcastTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsulePodcast.description", lang)
            task.dependsOn("generateCapsuleVideo")
            task.podcastEnabled.set(project.provider {
                capsuleExt?.podcastEnabled?.get()
                    ?: project.findProperty("capsule.podcast.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.deckName.set(project.provider {
                project.findProperty("deck.name")?.toString()
                    ?: project.layout.buildDirectory.dir("capsule").get().asFile
                        .listFiles { f -> f.isDirectory }?.firstOrNull()?.name
                    ?: "demo"
            })
            task.podcastOutput.convention(
                project.layout.buildDirectory.file(
                    project.provider {
                        val configured = capsuleExt?.podcastOutputFile?.get().orEmpty()
                        if (configured.isNotBlank()) configured
                        else "capsule/${task.deckName.get()}-podcast.mp3"
                    }
                )
            )
            task.audioFiles.from(project.provider {
                val capDir = project.layout.buildDirectory.dir("capsule").get().asFile
                val deckDir = capDir.listFiles { f -> f.isDirectory }?.firstOrNull()
                deckDir?.listFiles { f -> f.name.endsWith(".mp3") && f.name.startsWith("slide-") }
                    ?.toList() ?: emptyList()
            })
        }
    }
}
