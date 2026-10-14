package capsule

import capsule.ai.CapsuleLlmService.registerLlmBuildService
import capsule.i18n.CapsuleMessages
import org.gradle.api.Project

/**
 * Registrar of the viral-campaign tasks (CAP-DECOMPOSE BS-2).
 *
 * Pure extraction from [CapsuleManager]: the six `register*` functions below were
 * moved verbatim (no logic change) so `CapsuleManager.registerTasks()` stays a thin
 * orchestrator that delegates to per-domain registrars. Mirrors the pattern proven
 * by slider `SLD-6` and the in-house `capsule.feed.CapsuleFeedTaskRegistrar`.
 */
internal object CapsuleViralRegistrar {

    /** Registers every viral-campaign task on [project]. */
    fun register(project: Project) {
        project.registerGenerateViralCampaignTask()
        project.registerRenderViralVariantsTask()
        project.registerRenderViralThumbnailsTask()
        project.registerRenderViralSubtitlesTask()
        project.registerValidateViralCampaignTask()
        project.registerNormalizeViralStoryboardTask()
    }

    private fun Project.registerGenerateViralCampaignTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        val llmServiceProvider = registerLlmBuildService()
        tasks.register("generateViralCampaign", capsule.viral.GenerateViralCampaignTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateViralCampaign.description", lang)
            task.viralEnabled.convention(project.provider {
                capsuleExt?.viralEnabled?.get()
                    ?: project.findProperty("capsule.viral.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.storyboardRequired.convention(project.provider {
                capsuleExt?.viralStoryboardRequired?.get()
                    ?: project.findProperty("capsule.viral.storyboardRequired")?.toString()?.toBoolean()
                    ?: true
            })
            task.platform.convention(project.provider {
                capsuleExt?.viralPlatform?.get()
                    ?: project.findProperty("capsule.viral.platform")?.toString()
                    ?: capsule.viral.ViralPlatform.TIKTOK.name
            })
            task.targetDurationSeconds.convention(project.provider {
                capsuleExt?.viralTargetDurationSeconds?.get()
                    ?: project.findProperty("capsule.viral.targetDurationSeconds")?.toString()?.toIntOrNull()
                    ?: capsule.viral.ViralConfig.DEFAULT_DURATION_SECONDS
            })
            task.configuredHook.convention(project.provider {
                capsuleExt?.viralHook?.get()
                    ?: project.findProperty("capsule.viral.hook")?.toString().orEmpty()
            })
            task.configuredCta.convention(project.provider {
                capsuleExt?.viralCta?.get()
                    ?: project.findProperty("capsule.viral.cta")?.toString().orEmpty()
            })
            task.languages.convention(
                project.provider {
                    val ext = capsuleExt?.viralLanguages?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.languages")?.toString()
                        ?.let { it.split(",").map { code -> code.trim() }.filter { code -> code.isNotBlank() } }
                        ?: listOf(project.findProperty("deck.language")?.toString() ?: "fr")
                },
            )
            task.platforms.convention(
                project.provider {
                    val ext = capsuleExt?.viralPlatforms?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.platforms")?.toString()
                        ?.let { it.split(",").map { name -> name.trim() }.filter { name -> name.isNotBlank() } }
                        ?: listOf(task.platform.get())
                },
            )
            task.storyboardFile.convention(
                project.layout.file(project.provider {
                    val configured = capsuleExt?.viralStoryboardFile?.get().orEmpty()
                    val path = configured.ifBlank {
                        project.findProperty("capsule.viral.storyboardFile")?.toString().orEmpty()
                    }
                    if (path.isBlank()) null else project.file(path)
                }),
            )
            task.augmentedContextFile.convention(
                project.layout.file(project.provider {
                    val f = project.layout.buildDirectory.file("capsule/augmented-context.txt").get().asFile
                    if (f.exists()) f else null
                }),
            )
            task.copyFiles.from(
                project.provider {
                    val dir = project.layout.buildDirectory.dir("capsule/viral/copy").get().asFile
                    if (dir.isDirectory) dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.toList() ?: emptyList()
                    else emptyList()
                },
            )
            task.renderedVideoDir.convention("build/capsule/viral/videos")
            task.storyboardOutput.convention(project.layout.buildDirectory.file("capsule/viral/storyboard.adoc"))
            task.deckOutput.convention(project.layout.buildDirectory.file("capsule/viral/viral-deck.html"))
            task.thumbnailOutput.convention(project.layout.buildDirectory.file("capsule/viral/viral-thumbnail.html"))
            task.manifestOutput.convention(project.layout.buildDirectory.file("capsule/viral/viral-campaign.json"))
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }
    }

    private fun Project.registerRenderViralVariantsTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("renderViralVariants", capsule.viral.RenderViralVariantsTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = "Render the viral campaign: one short-form MP4 per variant (FFmpeg socle)."
            task.dependsOn("generateViralCampaign")
            task.viralEnabled.convention(project.provider {
                capsuleExt?.viralEnabled?.get()
                    ?: project.findProperty("capsule.viral.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.languages.convention(
                project.provider {
                    val ext = capsuleExt?.viralLanguages?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.languages")?.toString()
                        ?.let { it.split(",").map { code -> code.trim() }.filter { code -> code.isNotBlank() } }
                        ?: listOf(project.findProperty("deck.language")?.toString() ?: "fr")
                },
            )
            task.platforms.convention(
                project.provider {
                    val ext = capsuleExt?.viralPlatforms?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.platforms")?.toString()
                        ?.let { it.split(",").map { name -> name.trim() }.filter { name -> name.isNotBlank() } }
                        ?: listOf(capsuleExt?.viralPlatform?.get() ?: capsule.viral.ViralPlatform.TIKTOK.name)
                },
            )
            task.targetDurationSeconds.convention(project.provider {
                capsuleExt?.viralTargetDurationSeconds?.get()
                    ?: project.findProperty("capsule.viral.targetDurationSeconds")?.toString()?.toIntOrNull()
                    ?: capsule.viral.ViralConfig.DEFAULT_DURATION_SECONDS
            })
            task.renderedVideoDir.convention("build/capsule/viral/videos")
            task.ffmpegPath.convention(project.provider {
                capsuleExt?.ffmpegExecutablePath?.get() ?: "ffmpeg"
            })
            task.strict.convention(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
            task.captureTimeoutMs.convention(project.provider {
                capsuleExt?.playwrightTimeout?.get() ?: 120_000.0
            })
            task.toleranceSecs.convention(project.provider {
                project.findProperty("capsule.viral.durationToleranceSecs")?.toString()?.toDoubleOrNull() ?: 1.0
            })
            task.storyboardFile.convention(
                project.layout.file(project.provider {
                    val configured = capsuleExt?.viralStoryboardFile?.get().orEmpty()
                    val path = configured.ifBlank {
                        project.findProperty("capsule.viral.storyboardFile")?.toString().orEmpty()
                    }
                    if (path.isBlank()) null else project.file(path)
                }),
            )
            task.deckFile.convention(
                project.layout.file(project.provider {
                    val f = project.layout.buildDirectory.file("capsule/viral/viral-deck.html").get().asFile
                    if (f.exists()) f else null
                }),
            )
        }
    }

    private fun Project.registerRenderViralThumbnailsTask() {
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("renderViralThumbnails", capsule.viral.RenderViralThumbnailsTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", CapsuleMessages.resolveLanguage(this))
            task.description = "Render the viral campaign covers: one portrait PNG per variant (FFmpeg screenshot socle)."
            task.dependsOn("generateViralCampaign")
            task.viralEnabled.convention(project.provider {
                capsuleExt?.viralEnabled?.get()
                    ?: project.findProperty("capsule.viral.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.languages.convention(
                project.provider {
                    val ext = capsuleExt?.viralLanguages?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.languages")?.toString()
                        ?.let { it.split(",").map { code -> code.trim() }.filter { code -> code.isNotBlank() } }
                        ?: listOf(project.findProperty("deck.language")?.toString() ?: "fr")
                },
            )
            task.platforms.convention(
                project.provider {
                    val ext = capsuleExt?.viralPlatforms?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.platforms")?.toString()
                        ?.let { it.split(",").map { name -> name.trim() }.filter { name -> name.isNotBlank() } }
                        ?: listOf(capsuleExt?.viralPlatform?.get() ?: capsule.viral.ViralPlatform.TIKTOK.name)
                },
            )
            task.targetDurationSeconds.convention(project.provider {
                capsuleExt?.viralTargetDurationSeconds?.get()
                    ?: project.findProperty("capsule.viral.targetDurationSeconds")?.toString()?.toIntOrNull()
                    ?: capsule.viral.ViralConfig.DEFAULT_DURATION_SECONDS
            })
            task.renderedVideoDir.convention("build/capsule/viral/videos")
            task.ffmpegPath.convention(project.provider {
                capsuleExt?.ffmpegExecutablePath?.get() ?: "ffmpeg"
            })
            task.strict.convention(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
            task.captureTimeoutMs.convention(project.provider {
                capsuleExt?.playwrightTimeout?.get() ?: 120_000.0
            })
            task.coverFile.convention(
                project.layout.file(project.provider {
                    val f = project.layout.buildDirectory.file("capsule/viral/viral-thumbnail.html").get().asFile
                    if (f.exists()) f else null
                }),
            )
        }
    }

    private fun Project.registerRenderViralSubtitlesTask() {
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("renderViralSubtitles", capsule.viral.RenderViralSubtitlesTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", CapsuleMessages.resolveLanguage(this))
            task.description = "Render the viral campaign karaoke captions: one <variant.id>.ass per variant."
            task.dependsOn("generateViralCampaign")
            task.viralEnabled.convention(project.provider {
                capsuleExt?.viralEnabled?.get()
                    ?: project.findProperty("capsule.viral.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.languages.convention(
                project.provider {
                    val ext = capsuleExt?.viralLanguages?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.languages")?.toString()
                        ?.let { it.split(",").map { code -> code.trim() }.filter { code -> code.isNotBlank() } }
                        ?: listOf(project.findProperty("deck.language")?.toString() ?: "fr")
                },
            )
            task.platforms.convention(
                project.provider {
                    val ext = capsuleExt?.viralPlatforms?.orNull
                    if (!ext.isNullOrEmpty()) ext
                    else project.findProperty("capsule.viral.platforms")?.toString()
                        ?.let { it.split(",").map { name -> name.trim() }.filter { name -> name.isNotBlank() } }
                        ?: listOf(capsuleExt?.viralPlatform?.get() ?: capsule.viral.ViralPlatform.TIKTOK.name)
                },
            )
            task.targetDurationSeconds.convention(project.provider {
                capsuleExt?.viralTargetDurationSeconds?.get()
                    ?: project.findProperty("capsule.viral.targetDurationSeconds")?.toString()?.toIntOrNull()
                    ?: capsule.viral.ViralConfig.DEFAULT_DURATION_SECONDS
            })
            task.renderedVideoDir.convention("build/capsule/viral/videos")
            task.storyboardFile.convention(
                project.layout.file(project.provider {
                    val f = project.layout.buildDirectory.file("capsule/viral/storyboard.adoc").get().asFile
                    if (f.exists()) f else null
                }),
            )
            task.wordTimingsFile.convention(
                project.layout.file(project.provider {
                    val path = project.findProperty("capsule.viral.wordTimingsFile")?.toString().orEmpty()
                    if (path.isBlank()) null else project.file(path).takeIf { it.isFile }
                }),
            )
        }
    }

    private fun Project.registerValidateViralCampaignTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("validateViralCampaign", capsule.viral.ValidateViralCampaignTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.verification", lang)
            task.description = "Verifies that every asset referenced by the viral campaign manifest exists (delivery contract)."
            task.dependsOn("renderViralVariants", "renderViralThumbnails", "renderViralSubtitles")
            task.viralEnabled.convention(project.provider {
                capsuleExt?.viralEnabled?.get()
                    ?: project.findProperty("capsule.viral.enabled")?.toString()?.toBoolean()
                    ?: false
            })
            task.strict.convention(project.provider {
                capsuleExt?.strictMode?.get() ?: false
            })
            task.manifestFile.convention(
                 project.layout.file(project.provider {
                     val f = project.layout.buildDirectory.file("capsule/viral/viral-campaign.json").get().asFile
                     if (f.exists()) f else null
                 }),
             )
         }
     }

    private fun Project.registerNormalizeViralStoryboardTask() {
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register("normalizeViralStoryboard", capsule.viral.NormalizeViralStoryboardTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", CapsuleMessages.resolveLanguage(this))
            task.description = "Normalize a hand-authored viral storyboard: fill every missing per-beat entering transition (editorial gate)."
            task.storyboardFile.convention(
                project.layout.file(project.provider {
                    val configured = capsuleExt?.viralStoryboardFile?.get().orEmpty()
                    val path = configured.ifBlank {
                        project.findProperty("capsule.viral.storyboardFile")?.toString().orEmpty()
                    }
                    if (path.isBlank()) null else project.file(path)
                }),
            )
            task.normalizedOutput.convention(
                project.layout.buildDirectory.file("capsule/viral/storyboard.normalized.adoc"),
            )
        }
    }
}
