package capsule

import capsule.i18n.CapsuleMessages
import org.gradle.api.Project

/**
 * Registrar of the QC / verification tasks (CAP-DECOMPOSE BS-3).
 *
 * Pure extraction from [CapsuleManager]: the four `register*` functions below were
 * moved verbatim (no logic change) so `CapsuleManager.registerTasks()` stays a thin
 * orchestrator that delegates to per-domain registrars. Mirrors the pattern proven
 * by slider `SLD-6` and the in-house `CapsuleViralRegistrar` / `CapsuleFeedTaskRegistrar`.
 */
internal object CapsuleQcRegistrar {

    /** Registers every QC / verification task on [project]. */
    fun register(project: Project) {
        project.registerValidateCapsuleVideoDurationTask()
        project.registerPreflightCapsuleContentTask()
        project.registerRenderQcBoardsTask()
        project.registerGenerateCapsuleChaptersTask()
    }

    private fun Project.registerPreflightCapsuleContentTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("preflightCapsuleContent", capsule.qc.PreflightCapsuleContentTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = "Pre-flight the capsule scripts before any metered TTS synthesis (PRT-05)."
            task.enabled.set(project.provider {
                capsuleExt?.qcPreflightEnabled?.get()
                    ?: project.findProperty("capsule.qc.preflightEnabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.maxNoteChars.set(project.provider {
                capsuleExt?.qcMaxNoteChars?.get()
                    ?: project.findProperty("capsule.qc.maxNoteChars")?.toString()?.toIntOrNull()
                    ?: 500
            })
            task.strict.set(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
        }
    }

    private fun Project.registerRenderQcBoardsTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("renderQcBoards", capsule.qc.RenderQcBoardsTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = CapsuleMessages.get("task.renderQcBoards.description", lang)
            task.enabled.set(project.provider {
                capsuleExt?.qcBoardsEnabled?.get()
                    ?: project.findProperty("capsule.qc.boardsEnabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.outputDir.convention(project.layout.buildDirectory.dir("capsule/qc"))
        }
    }

    private fun Project.registerValidateCapsuleVideoDurationTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register(
            "validateCapsuleVideoDuration",
            capsule.validation.ValidateCapsuleVideoDurationTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = CapsuleMessages.get("task.validateCapsuleVideoDuration.description", lang)
            task.durationEnabled.set(project.provider {
                capsuleExt?.durationValidationEnabled?.get()
                    ?: project.findProperty("capsule.validation.durationEnabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.toleranceSecs.set(project.provider {
                capsuleExt?.durationValidationToleranceSecs?.get()
                    ?: project.findProperty("capsule.validation.toleranceSecs")?.toString()?.toDoubleOrNull()
                    ?: 2.0
            })
            task.integrityEnabled.set(project.provider {
                capsuleExt?.integrityValidationEnabled?.get()
                    ?: project.findProperty("capsule.validation.integrityEnabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.ffmpegPath.set(project.provider {
                capsuleExt?.ffmpegExecutablePath?.get() ?: "ffmpeg"
            })
            task.videoFile.convention(
                project.layout.buildDirectory.file(
                    project.provider {
                        val capDir = project.layout.buildDirectory.dir(
                            capsuleExt?.outputDir?.get() ?: "capsule"
                        ).get().asFile
                        capDir.listFiles { f -> f.name.endsWith(".webm") || f.name.endsWith(".mp4") }
                            ?.firstOrNull()
                            ?.let { it.absolutePath }
                            ?: (capsuleExt?.outputDir?.get() ?: "capsule") + "/video-not-found"
                    }
                )
            )
            task.audioFiles.from(project.provider {
                val capDir = project.layout.buildDirectory.dir(
                    capsuleExt?.outputDir?.get() ?: "capsule"
                ).get().asFile
                val audioDirs = capDir.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
                audioDirs.flatMap { dir ->
                    dir.listFiles { f -> f.name.endsWith(".mp3") }?.toList() ?: emptyList()
                }
            })
        }
    }

    private fun Project.registerGenerateCapsuleChaptersTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register(
            "generateCapsuleChapters",
            capsule.chapters.GenerateCapsuleChaptersTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleChapters.description", lang)
            task.enabled.convention(project.provider {
                capsuleExt?.chaptersEnabled?.get()
                    ?: project.findProperty("capsule.chapters.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.introText.convention(project.provider {
                capsuleExt?.chaptersIntroText?.orNull
                    ?: project.findProperty("capsule.chapters.introText")?.toString()
                    ?: ""
            })
            task.outroText.convention(project.provider {
                capsuleExt?.chaptersOutroText?.orNull
                    ?: project.findProperty("capsule.chapters.outroText")?.toString()
                    ?: ""
            })
            task.slideSegmentsJson.set("[]")
            task.outputDir.convention(project.layout.buildDirectory.dir("capsule/chapters"))
        }
    }
}
