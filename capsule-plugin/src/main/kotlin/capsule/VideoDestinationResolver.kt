package capsule

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Pure resolver for the versioned video destination path (CAP-ARCH-7).
 *
 * Resolves the versioned subdirectory under [OutputConfig.videoDestinationDir]
 * where the final WebM is copied after capture, so the user can compare
 * capsule versions side-by-side. Pure value object — no I/O except
 * [collectExistingVersions] which lists existing versioned subdirectories.
 *
 * Versioning strategies:
 * - [VersioningStrategy.TIMESTAMP] — ISO-8601 compact label (`vyyyyMMddHHmm`).
 *   Idempotent, no state to manage.
 * - [VersioningStrategy.INCREMENTAL] — incrementing integer (`v1`, `v2`, ...).
 *   Reads existing versioned siblings to determine the next number.
 */
object VideoDestinationResolver {

    private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    /**
     * Resolves the version label for the next copy, honoring [config.versioning]
     * and [config.versionPrefix].
     *
     * - [VersioningStrategy.TIMESTAMP] — `vyyyyMMddHHmm` (current time).
     * - [VersioningStrategy.INCREMENTAL] — highest existing `<prefix><n>` + 1,
     *   starting at 1 when no existing versions.
     */
    fun resolveVersionLabel(config: OutputConfig, existingVersions: List<String>): String =
        when (config.versioning) {
            VersioningStrategy.TIMESTAMP -> {
                val now = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
                "${config.versionPrefix}$now"
            }
            VersioningStrategy.INCREMENTAL -> {
                val highest = existingVersions
                    .mapNotNull { label ->
                        val stripped = label.removePrefix(config.versionPrefix)
                        stripped.toIntOrNull()
                    }
                    .maxOrNull() ?: 0
                "${config.versionPrefix}${highest + 1}"
            }
        }

    /**
     * Resolves the versioned destination directory for the next copy, creating
     * the parent [OutputConfig.videoDestinationDir] if missing.
     */
    fun resolveDestinationDir(config: OutputConfig, existingVersions: List<String> = emptyList()): File {
        val root = File(config.videoDestinationDir)
        if (!root.exists()) root.mkdirs()
        val label = resolveVersionLabel(config, existingVersions)
        return File(root, label)
    }

    /**
     * Resolves the final destination file (video inside the versioned dir).
     */
    fun resolveDestinationFile(config: OutputConfig, videoFileName: String, existingVersions: List<String> = emptyList()): File {
        val dir = resolveDestinationDir(config, existingVersions)
        return File(dir, videoFileName)
    }

    /**
     * Lists the versioned subdirectories under [destinationRoot] matching the
     * [prefix]. Non-matching entries (files, dirs without the prefix, dirs
     * whose suffix is not numeric for incremental) are ignored. Returns an
     * empty list when [destinationRoot] does not exist.
     */
    fun collectExistingVersions(destinationRoot: File, prefix: String): List<String> {
        if (!destinationRoot.exists() || !destinationRoot.isDirectory) return emptyList()
        return destinationRoot.listFiles { f -> f.isDirectory && f.name.startsWith(prefix) }
            ?.map { it.name }
            ?: emptyList()
    }
}