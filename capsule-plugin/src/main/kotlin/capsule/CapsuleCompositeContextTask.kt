package capsule

import capsule.feed.CapsuleScriptReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Filesystem-bound: scans capsule output directory and generates JSON context")
open class CapsuleCompositeContextTask : DefaultTask() {
    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = capsuleExtensionCache ?: project.extensions.getByType(CapsuleExtension::class.java).also { capsuleExtensionCache = it }
        set(value) {
            capsuleExtensionCache = value
        }

    private var capsuleExtensionCache: CapsuleExtension? = null

    init {
        outputFile.convention(
            project.layout.buildDirectory.file("capsule/capsule-context.json"),
        )
    }

    @TaskAction
    fun execute() {
        val capDir =
            project.layout.buildDirectory
                .dir(
                    capsuleExtension.outputDir.get(),
                ).get()
                .asFile
        val distDir =
            project.layout.buildDirectory
                .dir("capsule/distrib")
                .get()
                .asFile
        val scriptDir =
            project.layout.buildDirectory
                .dir(
                    capsuleExtension.sliderScriptDir.get(),
                ).get()
                .asFile

        val scripts = CapsuleFactory.readScriptFiles(scriptDir)

        val capsuleEntries = mutableListOf<Map<String, Any>>()

        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)
            val deckName = parsed.name

            val originalVideo = capDir.resolve("$deckName.webm")
            val distribVideo = distDir.resolve("$deckName.webm")

            val slideInfos =
                parsed.segments.map { seg ->
                    mapOf(
                        "index" to seg.index,
                        "title" to seg.title,
                        "speakerNoteLength" to seg.narration.length,
                    )
                }

            capsuleEntries.add(
                mapOf<String, Any>(
                    "source" to "capsule",
                    "deckName" to deckName,
                    "slideCount" to parsed.segments.size,
                    "originalVideo" to originalVideo.absolutePath,
                    "distribVideo" to (if (distribVideo.exists()) distribVideo.absolutePath else ""),
                    "viewport" to
                        mapOf(
                            "width" to capsuleExtension.viewportWidth.get(),
                            "height" to capsuleExtension.viewportHeight.get(),
                        ),
                    "distribDimensions" to
                        mapOf(
                            "width" to capsuleExtension.distribOutputWidth.get(),
                            "height" to capsuleExtension.distribOutputHeight.get(),
                        ),
                    "slides" to slideInfos,
                    "ttsEngine" to capsuleExtension.ttsEngine.get(),
                    "ttsVoice" to capsuleExtension.ttsVoice.get(),
                ),
            )
        }

        val result =
            mapOf(
                "source" to "capsule",
                "version" to (project.version as String),
                "entries" to capsuleEntries,
                "timestamp" to System.currentTimeMillis(),
            )

        val mapper = jacksonObjectMapper()
        val outFile = outputFile.get().asFile
        outFile.parentFile.mkdirs()
        mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, result)

        logger.lifecycle(
            "CAPSULE COMPOSITE CONTEXT -> {} ({} decks)",
            outFile.absolutePath,
            capsuleEntries.size,
        )
    }
}
