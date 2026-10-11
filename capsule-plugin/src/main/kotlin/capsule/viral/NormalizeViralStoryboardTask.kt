package capsule.viral

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `normalizeViralStoryboard` (CAP-VIRAL-E2E follow-up).
 *
 * Reads a hand-authored storyboard AsciiDoc and writes a **normalized** copy in
 * which every beat carries an entering transition (filled from
 * [ViralStoryboardNormalizer]'s role-based default policy), so the result passes
 * the editorial gate. Reproducible, tested replacement for the ad-hoc transform
 * used during the CAP-SHORT US-4 dogfooding.
 *
 * The transform is deterministic and pure ([ViralStoryboardNormalizer]); this task
 * is a thin adapter. An explicit transition is preserved; only the missing ones are
 * filled.
 *
 * Inputs:
 *   - [storyboardFile] the hand-authored storyboard AsciiDoc.
 *
 * Outputs:
 *   - [normalizedOutput] `build/capsule/viral/storyboard.normalized.adoc` — feed it
 *     back via `-Pcapsule.viral.storyboardFile`.
 *
 * Usage:
 *   ./gradlew normalizeViralStoryboard -Pcapsule.viral.storyboardFile=storyboard.adoc
 */
@DisableCachingByDefault(because = "Storyboard normalization — pure text transform of an input document")
abstract class NormalizeViralStoryboardTask : DefaultTask() {

    /** Hand-authored storyboard AsciiDoc to normalize. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val storyboardFile: RegularFileProperty

    /** Normalized storyboard output (every beat carries a transition). */
    @get:OutputFile
    abstract val normalizedOutput: RegularFileProperty

    @TaskAction
    fun run() {
        val input = storyboardFile.orNull?.asFile
        if (input == null || !input.exists()) {
            logger.warn(
                "CAPSULE VIRAL STORYBOARD → no storyboard to normalize " +
                    "(set -Pcapsule.viral.storyboardFile=<path>).",
            )
            return
        }

        val storyboard = ViralStoryboardParser.parse(input.readText())
            ?: error(
                "CAPSULE VIRAL STORYBOARD → '${input.path}' is not a validable storyboard " +
                    "(expected the '= Storyboard — <message>' header, beats and the short-form duration).",
            )

        val normalized = ViralStoryboardNormalizer.normalize(storyboard)
        val out = normalizedOutput.get().asFile
        out.parentFile.mkdirs()
        out.writeText(ViralStoryboardBuilder.build(normalized))

        val filled = storyboard.beats.count { it.enteringTransition.isBlank() }
        logger.lifecycle(
            "CAPSULE VIRAL STORYBOARD → normalized ($filled transition(s) filled) → ${out.absolutePath}",
        )
    }
}
