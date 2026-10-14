package capsule

import capsule.ai.CapsuleLlmService.registerLlmBuildService
import capsule.i18n.CapsuleMessages
import org.gradle.api.Project

/**
 * Registrar of the AI tasks (CAP-DECOMPOSE BS-4).
 *
 * Pure extraction from [CapsuleManager]: the `registerAiSmokeTestTask` function below
 * was moved verbatim (no logic change). Mirrors the pattern proven by slider `SLD-6`.
 */
internal object CapsuleAiRegistrar {

    /** Registers every AI task on [project]. */
    fun register(project: Project) {
        project.registerAiSmokeTestTask()
    }

    private fun Project.registerAiSmokeTestTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val llmServiceProvider = registerLlmBuildService()
        tasks.register("capsuleAiSmokeTest", capsule.ai.CapsuleAiSmokeTestTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.capsuleAiSmokeTest.description", lang)
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }
    }
}
