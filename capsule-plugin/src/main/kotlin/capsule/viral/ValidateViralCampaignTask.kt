package capsule.viral

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `validateViralCampaign` (CAP-VIRAL-E2E US-1).
 *
 * The **last-mile delivery contract** of the viral campaign: it reads the
 * `viral-campaign.json` manifest written by `generateViralCampaign` (whose
 * `videoFile` is a *constructed* path) and verifies that every referenced asset
 * actually exists — after `renderViralVariants` has produced the MP4s.
 *
 * Runs in the `verification` group, `dependsOn renderViralVariants`. The decision
 * lives in the pure [ViralCampaignGate]; this task is a thin adapter.
 *
 * Behavior:
 * - [viralEnabled] false (default) → no-op skip (economy of ink).
 * - manifest absent → warning, or failure when [strict] (the campaign was never assembled).
 * - malformed manifest → warning, or failure when [strict].
 * - missing asset(s) → warning, or failure when [strict] (the delivered campaign is broken).
 *
 * Usage:
 *   ./gradlew validateViralCampaign -Pcapsule.viral.enabled=true
 */
@DisableCachingByDefault(because = "Delivery gate — reads the campaign manifest and probes the produced assets")
abstract class ValidateViralCampaignTask : DefaultTask() {

    /** Master switch — when false (default), the task is a no-op skip. */
    @get:Input
    abstract val viralEnabled: Property<Boolean>

    /** When true, a missing/malformed/mis-referenced campaign fails the build. */
    @get:Input
    abstract val strict: Property<Boolean>

    /** `viral-campaign.json` produced by `generateViralCampaign`. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun run() {
        if (!viralEnabled.get()) {
            logger.lifecycle("CAPSULE VIRAL GATE → skipped (viralEnabled=false)")
            return
        }

        val manifest = manifestFile.orNull?.asFile
        if (manifest == null || !manifest.exists()) {
            failOrWarn("CAPSULE VIRAL GATE → no campaign manifest (run `generateViralCampaign` + `renderViralVariants` first).")
            return
        }

        val bundles = runCatching { CampaignBundleSerializer.parseManifest(manifest.readText()) }
            .getOrElse { e ->
                failOrWarn("CAPSULE VIRAL GATE → malformed manifest '${manifest.absolutePath}': ${e.message}")
                return
            }

        when (val result = ViralCampaignGate.verdict(bundles) { exists(it) }) {
            is ViralCampaignGateResult.Valid -> logger.lifecycle(
                "CAPSULE VIRAL GATE → OK (${bundles.size} bundle(s), all assets present)",
            )
            is ViralCampaignGateResult.Invalid -> failOrWarn(
                "CAPSULE VIRAL GATE → ${result.missing.size} missing asset(s): ${result.missing.joinToString("; ")}",
            )
        }
    }

    /** Fails the build when [strict], otherwise warns (degraded, non-blocking). */
    private fun failOrWarn(message: String) {
        if (strict.get()) error(message) else logger.warn(message)
    }

    /** Resolves a manifest path (absolute or project-relative) to a non-empty file. */
    private fun exists(path: String): Boolean {
        val f = File(path)
        val resolved = if (f.isAbsolute) f else File(project.projectDir, path)
        return resolved.exists() && resolved.length() > 0L
    }
}
