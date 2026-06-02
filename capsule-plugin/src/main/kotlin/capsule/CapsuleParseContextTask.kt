package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

@DisableCachingByDefault(because = "Filesystem-bound: reads capsule context JSON and writes parsed results")
open class CapsuleParseContextTask : DefaultTask() {

    @get:Internal
    val contextFile: RegularFileProperty = project.objects.fileProperty()

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty()

    @TaskAction
    fun execute() {
        val input = contextFile.asFile.get()
        val output = outputFile.asFile.get()
        output.parentFile.mkdirs()

        if (!input.exists()) {
            logger.warn("capsule-context.json not found at {}, returning empty list", input.absolutePath)
            val mapper = jacksonObjectMapper()
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, emptyList<Map<String, Any>>())
            logger.lifecycle("CAPSULE PARSE CONTEXT -> {} (0 decks, no input file)", output.absolutePath)
            return
        }

        val mapper = jacksonObjectMapper()
        val root: Map<String, Any> = mapper.readValue(input)

        @Suppress("UNCHECKED_CAST")
        val entries = root["entries"] as? List<Map<String, Any>> ?: emptyList()

        val results = entries.map { entry ->
            mapOf<String, Any>(
                "source" to "capsule",
                "deckName" to (entry["deckName"]?.toString() ?: ""),
                "slideCount" to ((entry["slideCount"] as? Number)?.toInt() ?: 0),
                "originalVideo" to (entry["originalVideo"]?.toString() ?: ""),
                "distribVideo" to (entry["distribVideo"]?.toString() ?: ""),
                "ttsEngine" to (entry["ttsEngine"]?.toString() ?: ""),
                "ttsVoice" to (entry["ttsVoice"]?.toString() ?: "")
            )
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(output, results)
        logger.lifecycle("CAPSULE PARSE CONTEXT -> {} ({} decks)", output.absolutePath, results.size)
    }
}
