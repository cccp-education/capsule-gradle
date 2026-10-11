package capsule.qc

import capsule.CapsuleExtension
import capsule.CapsuleManager
import capsule.feed.CapsuleScriptReader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Gradle task: `preflightCapsuleContent` (PRT-05).
 *
 * Runs [ContentPreflight] over the capsule scripts BEFORE any metered TTS
 * synthesis, so an incoherent deck never costs a paid TTS call. Behaviour:
 * - when [enabled] is false (default), the task is a no-op;
 * - otherwise every finding is logged; [Severity.ERROR] findings fail the build,
 *   and [Severity.WARNING] findings fail too when [strict] is true.
 *
 * Reads the script directory the same way [capsule.CapsuleBuildTask] does
 * ([CapsuleManager.resolveScriptDir] + [CapsuleManager.readScriptFiles]).
 */
@DisableCachingByDefault(because = "Reads script files; the verdict is a cheap function of their content")
open class PreflightCapsuleContentTask : DefaultTask() {

    /** Master switch — when false, the task is a no-op (opt-in, backward compat). */
    @get:Input
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    /** Maximum characters per speaker note before a `long-note` warning. */
    @get:Input
    val maxNoteChars: Property<Int> = project.objects.property(Int::class.java)
        .convention(PreflightRules.DEFAULT_MAX_NOTE_CHARS)

    /** When true, warnings fail the build too. */
    @get:Input
    val strict: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @TaskAction
    fun run() {
        if (!enabled.get()) {
            logger.lifecycle("CAPSULE PREFLIGHT → skipped (qc.preflightEnabled=false)")
            return
        }

        val scriptDir = CapsuleManager.resolveScriptDir(project, capsuleExtension)
        val scripts = CapsuleManager.readScriptFiles(scriptDir)
        if (scripts.isEmpty()) {
            logger.lifecycle("CAPSULE PREFLIGHT → no scripts found in {}", scriptDir.absolutePath)
            return
        }

        val rules = PreflightRules(maxNoteChars = maxNoteChars.get())
        val findings = mutableListOf<PreflightFinding>()
        for (script in scripts) {
            val parsed = CapsuleScriptReader.read(script)
            findings += ContentPreflight.check(parsed.segments, rules)
        }

        findings.forEach {
            logger.warn("CAPSULE PREFLIGHT → [{}] slide {}: {} — {}", it.severity, it.slideIndex, it.code, it.message)
        }
        val errors = findings.count { it.severity == Severity.ERROR }
        val warnings = findings.count { it.severity == Severity.WARNING }
        logger.lifecycle("CAPSULE PREFLIGHT → {} finding(s) ({} error, {} warning)", findings.size, errors, warnings)

        if (errors > 0 || (strict.get() && warnings > 0)) {
            throw GradleException(
                "Content pre-flight failed: $errors error(s), $warnings warning(s) (strict=${strict.get()})"
            )
        }
    }
}
