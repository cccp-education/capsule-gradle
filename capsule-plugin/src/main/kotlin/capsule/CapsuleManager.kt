package capsule

import org.gradle.api.Project
import java.io.File

class CapsuleManager(private val project: Project) {

    private val capsuleExt = project.extensions.getByType(CapsuleExtension::class.java)

    fun registerTasks() {
        project.registerGenerateCapsuleScriptTask()
        project.registerGenerateCapsuleTask()
        project.registerGenerateCapsuleVideoTask()
        project.registerDeployCapsuleTask()
        project.registerCollectCapsuleContextTask()
        project.registerTransformCapsuleContextTask()
    }

    private fun Project.registerGenerateCapsuleScriptTask() {
        tasks.register("generateCapsuleScript", CapsuleScriptTask::class.java) { task ->
            task.group = "generate"
            task.description = "Reads *-script.txt produced by slider-gradle and validates the capsule script"
            task.capsuleExtension = this@CapsuleManager.capsuleExt
        }
    }

    private fun Project.registerGenerateCapsuleTask() {
        tasks.register("generateCapsule", CapsuleBuildTask::class.java) { task ->
            task.group = "generate"
            task.description = "Generates TTS audio files from capsule scripts (Piper placeholder)"
            task.dependsOn("generateCapsuleScript")
            task.capsuleExtension = this@CapsuleManager.capsuleExt
        }
    }

    private fun Project.registerGenerateCapsuleVideoTask() {
        tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java) { task ->
            task.group = "generate"
            task.description = "Injects TTS audio into deck HTML then captures video via Playwright Java"
            task.dependsOn("generateCapsule")
            task.capsuleExtension = this@CapsuleManager.capsuleExt
        }
    }

    private fun Project.registerDeployCapsuleTask() {
        tasks.register("deployCapsule", CapsuleDistribTask::class.java) { task ->
            task.group = "deploy"
            task.description = "Recadre les capsules en format vertical 9:16 (TikTok/Shorts) via FFmpeg"
            task.dependsOn("generateCapsuleVideo")
            task.capsuleExtension = this@CapsuleManager.capsuleExt
        }
    }

    private fun Project.registerCollectCapsuleContextTask() {
        tasks.register("collectCapsuleContext", CapsuleCompositeContextTask::class.java) { task ->
            task.group = "collect"
            task.description = "Exporte le contexte des capsules (chemins videos + metadonnees) en JSON compatible engine N3"
            task.dependsOn("deployCapsule")
            task.capsuleExtension = this@CapsuleManager.capsuleExt
        }
    }

    private fun Project.registerTransformCapsuleContextTask() {
        tasks.register("transformCapsuleContext", CapsuleParseContextTask::class.java) { task ->
            task.group = "transform"
            task.description = "Parse le fichier capsule-context.json et retourne une liste de decks"
            task.contextFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-context.json")
            )
            task.outputFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-parse-results.json")
            )
        }

        tasks.register("collectCapsuleRetrieve", CapsuleParseContextTask::class.java) { task ->
            task.group = "collect"
            task.description = "Retrieve capsule decks from capsule-context.json (N3 engine contract)"
            val outputFile = project.findProperty("outputFile") as? String
            if (outputFile != null) {
                task.outputFile.set(File(outputFile))
            }
            task.contextFile.convention(
                project.layout.buildDirectory.file("capsule/capsule-context.json")
            )
        }
    }

    companion object {
        fun readScriptFiles(dir: File): List<File> {
            return dir.listFiles { f ->
                f.name.endsWith("-script.txt") &&
                !f.name.startsWith("example-") &&
                !f.name.contains("-context-")
            }
                ?.toList() ?: emptyList()
        }

        fun parseScript(file: File): CapsuleScript {
            val lines = file.readLines()
            val deckName = lines.firstOrNull()
                ?.removePrefix("=== CAPSULE SCRIPT : ")
                ?.removeSuffix(" ===")
                ?.trim() ?: file.nameWithoutExtension

            val slides = mutableListOf<SlideSegment>()
            var currentIndex = -1
            var currentTitle = ""
            var currentType = SlideType.HTML
            var currentManimScene: String? = null
            val noteLines = mutableListOf<String>()

            for (i in 1 until lines.size) {
                val line = lines[i]
                when {
                    line.startsWith("--- SLIDE ") && line.contains(":") -> {
                        if (currentIndex >= 0) {
                            slides.add(
                                SlideSegment(
                                    index = currentIndex,
                                    title = currentTitle,
                                    speakerNote = noteLines.joinToString("\n").trim(),
                                    type = currentType,
                                    manimScene = currentManimScene
                                )
                            )
                            noteLines.clear()
                        }
                        val parts = line.removeSurrounding("--- SLIDE ", " ---")
                        val colonIdx = parts.indexOf(":")
                        currentIndex = parts.substring(0, colonIdx).trim()
                            .toIntOrNull() ?: (slides.size + 1)
                        val rawTitle = parts.substring(colonIdx + 1).trim()
                        val manimMatch = Regex("""\[manim:(\w+)]""").find(rawTitle)
                        currentType = when {
                            manimMatch != null -> SlideType.MANIM
                            rawTitle.contains("[html]") -> SlideType.HTML
                            else -> SlideType.HTML
                        }
                        currentManimScene = manimMatch?.groupValues?.get(1)
                        currentTitle = rawTitle
                            .replace(Regex("""\[manim:\w+]"""), "")
                            .replace("[html]", "")
                            .trim()
                    }
                    line.isNotBlank() && currentIndex >= 0 -> noteLines.add(line)
                }
            }

            if (currentIndex >= 0) {
                slides.add(
                    SlideSegment(
                        index = currentIndex,
                        title = currentTitle,
                        speakerNote = noteLines.joinToString("\n").trim(),
                        type = currentType,
                        manimScene = currentManimScene
                    )
                )
            }

            return CapsuleScript(deckName, slides)
        }

        fun resolveScriptDir(project: Project, capsuleExt: CapsuleExtension): File {
            val configured = capsuleExt.sliderScriptDir.get()
            val candidate = project.layout.buildDirectory.dir(configured).get().asFile
            if (candidate.exists() && candidate.listFiles()
                    ?.any { it.name.endsWith("-script.txt") } == true
            ) {
                return candidate
            }
            val sliderOutput = project.rootProject.projectDir.parentFile
                ?.resolve("slider-plugin")
                ?.resolve("slider")
                ?.resolve("build")
                ?.resolve("capsule")
            if (sliderOutput != null && sliderOutput.exists()) return sliderOutput
            return candidate
        }

        fun resolveDeckDir(project: Project, capsuleExt: CapsuleExtension): File {
            val configured = capsuleExt.deckSourceDir.get()
            val candidate = project.layout.buildDirectory.dir(configured).get().asFile
            if (candidate.exists()) return candidate
            val sliderOutput = project.rootProject.projectDir.parentFile
                ?.resolve("slider-plugin")
                ?.resolve("slider")
                ?.resolve("build")
                ?.resolve("docs")
                ?.resolve("asciidocRevealJs")
            if (sliderOutput != null && sliderOutput.exists()) return sliderOutput
            return candidate
        }
    }
}
