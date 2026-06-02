package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Filesystem-bound: reads slider output and produces TTS artifacts")
open class CapsuleScriptTask : DefaultTask() {

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Internal
    lateinit var capsuleExtension: CapsuleExtension

    init {
        outputDir.convention(project.layout.buildDirectory.dir("capsule"))
    }

    @TaskAction
    fun execute() {
        val scriptDir = CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleManager.readScriptFiles(scriptDir)

        if (scripts.isEmpty()) {
            logger.warn("No *-script.txt files found in {}", scriptDir.absolutePath)
            logger.warn("Run 'asciidocCapsule' from slider-gradle first.")
            return
        }

        for (script in scripts) {
            val parsed = CapsuleManager.parseScript(script)
            logger.lifecycle(
                "Capsule script '{}' → {} slides", parsed.deckName, parsed.slides.size
            )
            for (seg in parsed.slides) {
                logger.lifecycle(
                    "  [{}] {} ({} chars)", seg.index, seg.title, seg.speakerNote.length
                )
            }
        }
    }
}
