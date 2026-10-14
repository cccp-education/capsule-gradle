package capsule

import org.gradle.api.Project

/**
 * Thin orchestrator of the capsule task registration (CAP-DECOMPOSE BS-4).
 *
 * Every task family is registered by its per-domain registrar; `registerTasks()`
 * only delegates. Mirrors the pattern proven by slider `SLD-6`.
 */
class CapsuleManager(private val project: Project) {

    fun registerTasks() {
        CapsuleCoreRegistrar.register(project)
        CapsuleContextRegistrar.register(project)
        CapsulePodcastRegistrar.register(project)
        CapsuleAiRegistrar.register(project)
        CapsuleQcRegistrar.register(project)
        CapsuleVoiceRegistrar.register(project)
        CapsuleViralRegistrar.register(project)
    }
}
