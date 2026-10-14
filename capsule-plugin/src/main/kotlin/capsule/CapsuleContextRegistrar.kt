package capsule

import capsule.ai.CapsuleLlmService.registerLlmBuildService
import capsule.i18n.CapsuleMessages
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import java.io.File

/**
 * Registrar of the context / content tasks (CAP-DECOMPOSE BS-4).
 *
 * Pure extraction from [CapsuleManager]: the six `register*` functions below (plus
 * the `resolveGlobFiles` helper) were moved verbatim (no logic change) so
 * `CapsuleManager.registerTasks()` becomes a thin orchestrator. Mirrors the pattern
 * proven by slider `SLD-6` and the in-house `CapsuleViralRegistrar` / `CapsuleQcRegistrar`.
 */
internal object CapsuleContextRegistrar {

    /** Registers every context / content task on [project]. */
    fun register(project: Project) {
        project.registerCollectCapsuleContextTask()
        project.registerTransformCapsuleContextTask()
        project.registerScaffoldCapsuleContextTask()
        project.registerCollectAugmentedContextTask()
        project.registerGenerateCapsuleContentTask()
        project.registerGenerateTranscriptTask()
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
}
