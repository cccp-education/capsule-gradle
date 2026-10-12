package capsule.qc

import capsule.CapsuleExtension
import capsule.CapsuleManager
import capsule.feed.CapsuleScriptReader
import capsule.feed.deckName
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `renderQcBoards` (PRT-08).
 *
 * Renders two visual QC boards per deck, for a human to review a capsule before
 * shipping it:
 * - `<deck>-beats.png` — the final state of every slide (from the capture PNGs);
 * - `<deck>-dit-montre.png` — what each slide showed next to what the voice said.
 *
 * Behaviour:
 * - when [enabled] is false (default), the task is a no-op;
 * - when no script is found, it logs and skips;
 * - otherwise it writes the boards under [outputDir]. Missing pictures degrade
 *   to a placeholder — the board is still rendered.
 *
 * Opt-in (`qc.boardsEnabled`). The dit-vs-montré "frame" is the slide capture PNG
 * (a video-frame probe is a later refinement); the spoken text is the speaker note.
 */
@DisableCachingByDefault(because = "Renders Java2D PNG boards to disk")
abstract class RenderQcBoardsTask : DefaultTask() {

    /** Master switch — when false, the task is a no-op (opt-in, backward compat). */
    @get:Input
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /** Where the boards are written. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @get:Internal
    internal var scriptDirOverride: File? = null

    @TaskAction
    fun run() {
        if (!enabled.get()) {
            logger.lifecycle("CAPSULE QC BOARDS → skipped (qc.boardsEnabled=false)")
            return
        }

        val scriptDir = scriptDirOverride ?: CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleManager.readScriptFiles(scriptDir)
        if (scripts.isEmpty()) {
            logger.lifecycle("CAPSULE QC BOARDS → no script found in {}", scriptDir.absolutePath)
            return
        }

        val outputRoot = project.layout.buildDirectory.dir(capsuleExtension.outputDir.get()).get().asFile
        val qcDir = outputDir.get().asFile
        var boards = 0

        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)
            if (parsed.segments.isEmpty()) continue
            val deckDir = File(outputRoot, parsed.deckName)
            val images = parsed.segments.associate { segment ->
                segment.index to File(deckDir, "slide-%02d.png".format(segment.index)).takeIf { it.isFile }
            }

            QcBoardRenderer.render(
                BeatsBoardPlanner.plan(parsed.deckName, parsed.segments, images.filterValues { it != null }.mapValues { it.value!! }),
                File(qcDir, "${parsed.deckName}-beats.png"),
            )
            QcBoardRenderer.render(
                DitMontreBoardPlanner.plan(
                    parsed.deckName,
                    parsed.segments.map { DitMontre(it.index, it.title, images[it.index], it.speakerNote) },
                ),
                File(qcDir, "${parsed.deckName}-dit-montre.png"),
            )
            boards += 2
        }

        logger.lifecycle("CAPSULE QC BOARDS → {} board(s) rendered in {}", boards, qcDir.absolutePath)
    }
}
