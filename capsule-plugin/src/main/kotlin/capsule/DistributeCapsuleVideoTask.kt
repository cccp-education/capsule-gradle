package capsule

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Gradle task: `distributeCapsuleVideo`
 *
 * Post-capture distribution (CAP-ARCH-7): copies the final WebM videos from
 * the build output directory to a *versioned* subdirectory under the
 * configured [OutputConfig.videoDestinationDir], so the user can compare
 * capsule versions side-by-side.
 *
 * The version label is resolved via [VideoDestinationResolver] honoring
 * [OutputConfig.versioning] (TIMESTAMP default, INCREMENTAL reads existing
 * siblings). The build output directory is read from `CapsuleExtension.outputDir`
 * (resolved from the merged config, default `capsule`).
 *
 * Usage:
 *   ./gradlew distributeCapsuleVideo
 *   ./gradlew distributeCapsuleVideo -Poutput.videoDestinationDir=/custom/videos
 *
 * Economy of ink: if the destination file already exists and is non-empty,
 * the copy is skipped (idempotent re-distribution).
 */
@DisableCachingByDefault(because = "Filesystem copy to versioned destination")
abstract class DistributeCapsuleVideoTask : DefaultTask() {

    @get:Input
    abstract val videoDestinationDir: Property<String>

    @get:Input
    abstract val versioning: Property<String>

    @get:Input
    abstract val versionPrefix: Property<String>

    /** CAP-MP4 — output format filter (WEBM only, MP4 only, or both). */
    @get:Input
    abstract val format: Property<String>

    @get:Internal
    internal var capsuleExtension: CapsuleExtension
        get() = _capsuleExtension ?: project.extensions.getByType(CapsuleExtension::class.java).also { _capsuleExtension = it }
        set(value) { _capsuleExtension = value }

    private var _capsuleExtension: CapsuleExtension? = null

    @TaskAction
    fun run() {
        val capDir = project.layout.buildDirectory.dir(
            capsuleExtension.outputDir.get()
        ).get().asFile

        val resolvedFormat = OutputFormat.fromString(
            (project.findProperty("output.format") as? String) ?: format.get()
        )
        val extensions = when (resolvedFormat) {
            OutputFormat.WEBM -> listOf(".webm")
            OutputFormat.MP4 -> listOf(".mp4")
            OutputFormat.BOTH -> listOf(".webm", ".mp4")
        }

        val videos = capDir.listFiles { f -> extensions.any { f.name.endsWith(it) } }?.toList()
            ?: emptyList()

        if (videos.isEmpty()) {
            logger.warn("No capsule videos found in {}. Run 'generateCapsuleVideo' first.", capDir.absolutePath)
            return
        }

        // CLI -P overrides the values wired from the merged config.
        val cliDestDir = project.findProperty("output.videoDestinationDir")?.toString()
        val cliVersioning = project.findProperty("output.versioning")?.toString()
        val cliPrefix = project.findProperty("output.versionPrefix")?.toString()

        val config = OutputConfig(
            videoDestinationDir = cliDestDir?.takeIf { it.isNotBlank() } ?: videoDestinationDir.get(),
            versioning = VersioningStrategy.fromString(cliVersioning ?: versioning.get()),
            versionPrefix = cliPrefix?.takeIf { it.isNotBlank() } ?: versionPrefix.get(),
        )
        val destinationRoot = File(config.videoDestinationDir)
        val existing = VideoDestinationResolver.collectExistingVersions(destinationRoot, config.versionPrefix)
        val destinationDir = VideoDestinationResolver.resolveDestinationDir(config, existing)

        destinationDir.mkdirs()
        var copied = 0
        var skipped = 0

        for (video in videos) {
            val destFile = File(destinationDir, video.name)
            if (destFile.exists() && destFile.length() > 0) {
                logger.lifecycle("  SKIP → {} (already exists, {} bytes)", destFile.absolutePath, destFile.length())
                skipped++
            } else {
                video.copyTo(destFile, overwrite = false)
                logger.lifecycle("  COPY → {} ({} bytes)", destFile.absolutePath, video.length())
                copied++
            }
        }

        logger.lifecycle(
            "CAPSULE DISTRIB → {} ({} copied, {} skipped, version={})",
            destinationDir.absolutePath, copied, skipped, destinationDir.name,
        )
    }
}