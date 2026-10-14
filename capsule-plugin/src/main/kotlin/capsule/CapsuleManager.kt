package capsule

import capsule.ai.CapsuleLlmService.registerLlmBuildService
import capsule.i18n.CapsuleMessages
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
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
        project.registerGenerateCapsuleContentTask()
        project.registerGenerateTranscriptTask()
        project.registerGeneratePodcastTask()
        project.registerDistributeCapsuleVideoTask()
        project.registerValidateCapsuleVideoDurationTask()
        project.registerPreflightCapsuleContentTask()
        project.registerRenderQcBoardsTask()
        project.registerVerifyCapsuleVoiceTask()
        project.registerSelectBestVoiceTakeTask()
        project.registerGenerateCapsuleChaptersTask()
        project.registerGenerateViralCampaignTask()
        project.registerRenderViralVariantsTask()
        project.registerRenderViralThumbnailsTask()
        project.registerRenderViralSubtitlesTask()
        project.registerValidateViralCampaignTask()
        project.registerNormalizeViralStoryboardTask()
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

    private fun Project.registerCollectCapsuleContextTask() {        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("collectCapsuleContext", CapsuleCompositeContextTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.collect", lang)
            task.description = CapsuleMessages.get("task.collectCapsuleContext.description", lang)
            task.dependsOn("deployCapsule")
        }
    }

    private fun Project.registerTransformCapsuleContextTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("transformCapsuleContext", CapsuleParseContextTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.transform", lang)
            task.description = CapsuleMessages.get("task.transformCapsuleContext.description", lang)
            task.contextFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-context.json")
            )
            task.outputFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-parse-results.json")
            )
        }

        tasks.register("collectCapsuleRetrieve", CapsuleParseContextTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.collect", lang)
            task.description = CapsuleMessages.get("task.collectCapsuleRetrieve.description", lang)
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
        val lang = CapsuleMessages.resolveLanguage(this)
        tasks.register("scaffoldCapsuleContext", CapsuleScaffoldTask::class.java) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.scaffoldCapsuleContext.description", lang)
        }
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

    private fun Project.registerCollectAugmentedContextTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        tasks.register(
            "collectCapsuleAugmentedContext",
            capsule.context.CollectCapsuleAugmentedContextTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.collect", lang)
            task.description = CapsuleMessages.get("task.collectCapsuleAugmentedContext.description", lang)
            task.eagerFiles.from(
                project.layout.projectDirectory.file(".agents/INDEX.adoc"),
                project.layout.projectDirectory.file("PROMPT_REPRISE.adoc"),
                project.layout.projectDirectory.file("AGENT.adoc"),
            )

            // CAP-DOCCONTEXT-3 — resolve docsGlobs lazily (extension afterEvaluate > CLI).
            // The extension is populated by pushConfigIntoExtension in afterEvaluate,
            // so we use a provider to defer resolution until task execution.
            task.docsGlobs.set(project.provider {
                val extGlobs = capsuleExt?.docsGlobs?.orNull ?: emptyList()
                if (extGlobs.isNotEmpty()) extGlobs
                else project.findProperty("capsule.context.docsGlobs")?.toString()
                    ?.let { CapsuleConfigMerger.splitCommaList(it) }
                    ?: emptyList()
            })
            task.docsFiles.from(project.provider {
                val globs = task.docsGlobs.get()
                if (globs.isEmpty()) emptyList<Any>() else listOf(resolveGlobFiles(globs))
            })

            task.ragContent.set(project.findProperty("context.ragContent")?.toString().orEmpty())
            task.graphifyContent.set(project.findProperty("context.graphifyContent")?.toString().orEmpty())
            task.docsContent.set(project.findProperty("context.docsContent")?.toString().orEmpty())

            // CAP-SCENARIO-3 — resolve scenarioFile lazily (extension afterEvaluate > CLI).
            task.scenarioFile.from(project.provider {
                val extPath = capsuleExt?.scenarioFile?.orNull
                val path = when {
                    !extPath.isNullOrBlank() -> extPath
                    else -> project.findProperty("capsule.context.scenarioFile")?.toString().orEmpty()
                }
                if (path.isBlank()) emptyList<Any>()
                else listOf(project.file(path))
            })

            // CAP-GLOSSARY-2 — resolve glossaryFile lazily (extension afterEvaluate > CLI).
            task.glossaryFile.from(project.provider {
                val extPath = capsuleExt?.glossaryFile?.orNull
                val path = when {
                    !extPath.isNullOrBlank() -> extPath
                    else -> project.findProperty("capsule.context.glossaryFile")?.toString().orEmpty()
                }
                if (path.isBlank()) emptyList<Any>()
                else listOf(project.file(path))
            })

            task.tokenBudget.set(
                project.findProperty("context.tokenBudget")?.toString()?.toIntOrNull()
                    ?: contracts.context.ContextChannel.DEFAULT_TOKEN_BUDGET
            )
            task.outputFile.set(project.layout.buildDirectory.file("capsule/augmented-context.txt"))
            task.provenanceFile.set(project.layout.buildDirectory.file("capsule/context-provenance.json"))
        }
    }

    /**
     * Resolves Ant-style globs into a file collection relative to the project root
     * (CAP-DOCCONTEXT-3).
     */
    private fun Project.resolveGlobFiles(globs: List<String>): ConfigurableFileCollection {
        val collection = files()
        for (glob in globs) {
            val tree = fileTree(project.projectDir).matching { it.include(glob) }
            collection.from(tree)
        }
        return collection
    }

    private fun Project.registerGenerateCapsuleContentTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val llmServiceProvider = registerLlmBuildService()
        tasks.register(
            "generateCapsuleContent",
            capsule.pipeline.GenerateCapsuleContentTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleContent.description", lang)
            val deckProp = findProperty("deck.file") as? String
            if (deckProp != null) {
                task.deckFile.convention(
                    project.layout.file(project.provider { project.file(deckProp) })
                )
            } else {
                task.deckFile.convention(
                    project.layout.file(
                        project.provider {
                            capsule.feed.CapsuleAdocDir(project.projectDir).adocFiles()
                                .firstOrNull()
                                ?.let { project.file(it) }
                                ?: error("No deck found: set -Pdeck.file=<path> or add a .adoc in slides/misc")
                        }
                    )
                )
            }
            task.language.convention(findProperty("deck.language")?.toString() ?: "fr")
            task.augmentedContextFile.convention(
                project.layout.buildDirectory.file(
                    project.provider {
                        val f = project.layout.buildDirectory.file("capsule/augmented-context.txt").get().asFile
                        if (f.exists()) "capsule/augmented-context.txt" else null
                    }
                )
            )
            task.speakerNotesOutput.convention(
                project.layout.buildDirectory.file(
                    task.deckFile.map { deck ->
                        "capsule/${deck.asFile.nameWithoutExtension}-speaker-notes.adoc"
                    }
                )
            )
            task.ttsScriptOutput.convention(
                project.layout.buildDirectory.file(
                    task.deckFile.map { deck ->
                        "capsule/${deck.asFile.nameWithoutExtension}-script.txt"
                    }
                )
            )
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }
    }

    private fun Project.registerGenerateTranscriptTask() {
        val lang = CapsuleMessages.resolveLanguage(this)
        val capsuleExt = project.extensions.findByType(CapsuleExtension::class.java)
        val llmServiceProvider = registerLlmBuildService()
        tasks.register(
            "generateCapsuleTranscript",
            capsule.transcript.GenerateCapsuleTranscriptTask::class.java,
        ) { task ->
            task.group = CapsuleMessages.get("task.group.generate", lang)
            task.description = CapsuleMessages.get("task.generateCapsuleTranscript.description", lang)
            task.dependsOn("generateCapsuleContent")
            task.language.convention(findProperty("deck.language")?.toString() ?: "fr")
            task.strategy.convention(project.provider {
                capsuleExt?.transcriptStrategy?.orNull
                    ?: capsule.transcript.TranscriptStrategy.fromString(
                        findProperty("capsule.transcript.strategy")?.toString()
                    )
            })
            task.speakerNotesFile.convention(
                project.layout.buildDirectory.file(
                    project.provider {
                        val capDir = project.layout.buildDirectory.dir("capsule").get().asFile
                        capDir.listFiles { f -> f.name.endsWith("-speaker-notes.adoc") }
                            ?.firstOrNull()
                            ?.let { it.absolutePath }
                            ?: "capsule/no-speaker-notes.adoc"
                    }
                )
            )
            task.transcriptOutput.convention(
                project.layout.buildDirectory.file(
                    task.speakerNotesFile.map { notes ->
                        val deckName = notes.asFile.nameWithoutExtension
                            .removeSuffix("-speaker-notes")
                        "capsule/$deckName-transcript.adoc"
                    }
                )
            )
            task.llmService.set(llmServiceProvider)
            task.usesService(llmServiceProvider)
        }
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
