package capsule

import capsule.i18n.CapsuleMessages
import org.gradle.api.Project

/**
 * Registrar of the voice-quality tasks (CAP-DECOMPOSE BS-3).
 *
 * Pure extraction from [CapsuleManager]: the two `register*` functions below were
 * moved verbatim (no logic change) so `CapsuleManager.registerTasks()` stays a thin
 * orchestrator that delegates to per-domain registrars. Mirrors the pattern proven
 * by slider `SLD-6` and the in-house `CapsuleViralRegistrar`.
 */
internal object CapsuleVoiceRegistrar {

    /** Registers every voice-quality task on [project]. */
    fun register(project: Project) {
        project.registerVerifyCapsuleVoiceTask()
        project.registerSelectBestVoiceTakeTask()
    }

    private fun Project.registerVerifyCapsuleVoiceTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("verifyCapsuleVoice", capsule.voice.VerifyCapsuleVoiceTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = CapsuleMessages.get("task.verifyCapsuleVoice.description", lang)
            task.enabled.set(project.provider {
                capsuleExt?.voiceVerifyEnabled?.get()
                    ?: project.findProperty("capsule.voice.verifyEnabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.maxWordErrorRate.set(project.provider {
                capsuleExt?.voiceMaxWordErrorRate?.get()
                    ?: project.findProperty("capsule.voice.maxWordErrorRate")?.toString()?.toDoubleOrNull()
                    ?: 0.15
            })
            task.minSnrDb.set(project.provider {
                capsuleExt?.voiceMinSnrDb?.get()
                    ?: project.findProperty("capsule.voice.minSnrDb")?.toString()?.toDoubleOrNull()
                    ?: 66.0
            })
            task.surplusThreshold.set(project.provider {
                capsuleExt?.voiceSurplusThreshold?.get()
                    ?: project.findProperty("capsule.voice.surplusThreshold")?.toString()?.toIntOrNull()
                    ?: 3
            })
            task.asrExecutablePath.set(project.provider {
                capsuleExt?.voiceAsrExecutablePath?.get()
                    ?: project.findProperty("capsule.voice.asrExecutablePath")?.toString()
                    ?: "whisper"
            })
            task.asrModel.set(project.provider {
                capsuleExt?.voiceAsrModel?.get()
                    ?: project.findProperty("capsule.voice.asrModel")?.toString()
                    ?: ""
            })
            task.language.set(project.provider {
                capsuleExt?.voiceLanguage?.get()
                    ?: project.findProperty("capsule.voice.language")?.toString()
                    ?: "fr"
            })
            task.strict.set(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
            task.ffmpegPath.set(project.provider {
                capsuleExt?.ffmpegExecutablePath?.get() ?: "ffmpeg"
            })
            task.audioFiles.from(project.provider {
                val capDir = project.layout.buildDirectory.dir(
                    capsuleExt?.outputDir?.get() ?: "capsule"
                ).get().asFile
                val audioDirs = capDir.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
                audioDirs.flatMap { dir ->
                    dir.listFiles { f -> f.name.endsWith(".mp3") }?.toList() ?: emptyList()
                }
            })
            task.reportFile.convention(project.layout.buildDirectory.file("capsule/voice-report.json"))
        }
    }

    private fun Project.registerSelectBestVoiceTakeTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("selectBestVoiceTake", capsule.voice.SelectBestVoiceTakeTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = CapsuleMessages.get("task.selectBestVoiceTake.description", lang)
            task.enabled.set(project.provider {
                capsuleExt?.voiceSelectBestTake?.get()
                    ?: project.findProperty("capsule.voice.selectBestTake")?.toString()?.toBoolean()
                    ?: false
            })
            task.maxWordErrorRate.set(project.provider {
                capsuleExt?.voiceMaxWordErrorRate?.get() ?: 0.15
            })
            task.minSnrDb.set(project.provider {
                capsuleExt?.voiceMinSnrDb?.get() ?: 66.0
            })
            task.surplusThreshold.set(project.provider {
                capsuleExt?.voiceSurplusThreshold?.get() ?: 3
            })
            task.asrExecutablePath.set(project.provider {
                capsuleExt?.voiceAsrExecutablePath?.get() ?: "whisper"
            })
            task.asrModel.set(project.provider {
                capsuleExt?.voiceAsrModel?.get() ?: ""
            })
            task.language.set(project.provider {
                capsuleExt?.voiceLanguage?.get() ?: "fr"
            })
            task.strict.set(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
            task.ffmpegPath.set(project.provider {
                capsuleExt?.ffmpegExecutablePath?.get() ?: "ffmpeg"
            })
            task.audioFiles.from(project.provider {
                val capDir = project.layout.buildDirectory.dir(
                    capsuleExt?.outputDir?.get() ?: "capsule"
                ).get().asFile
                val audioDirs = capDir.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
                audioDirs.flatMap { dir ->
                    dir.listFiles { f -> f.name.endsWith(".mp3") }?.toList() ?: emptyList()
                }
            })
            task.selectionFile.convention(project.layout.buildDirectory.file("capsule/voice-selection.json"))
        }
    }
}
