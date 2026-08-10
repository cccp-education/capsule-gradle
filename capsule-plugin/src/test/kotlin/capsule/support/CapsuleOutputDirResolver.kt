package capsule.support

import java.io.File

/**
 * Pure resolver for the capsule `outputDir` DSL property from the Gradle build
 * script content of a Cucumber test project.
 *
 * Centralises outputDir extraction so Cucumber steps resolve the *actual*
 * configured directory (`build/<outputDir>/`) instead of hardcoding a single
 * path. This fixes the burn-in E2E path mismatch (S-087): the E2E scenario
 * configures `outputDir = "capsules"` but the step hardcoded `build/capsule/`,
 * so the subtitle file was never found at the expected location.
 *
 * The default `outputDir` is `"capsule"` (see [capsule.CapsuleConfig]).
 *
 * Parsing strategy: regex on the `outputDir = "..."` or `outputDir = '...'`
 * line. Intentionally lightweight — Cucumber test projects have small,
 * controlled build scripts, so a full Gradle DSL parser is overkill.
 */
object CapsuleOutputDirResolver {

    private val OUTPUT_DIR_REGEX = Regex("""outputDir\s*=\s*["']([^"']+)["']""")
    private const val DEFAULT_OUTPUT_DIR = "capsule"

    /**
     * Resolves the raw `outputDir` value from [buildGradleContent], or
     * [DEFAULT_OUTPUT_DIR] when absent.
     */
    fun resolveOutputDir(buildGradleContent: String): String =
        OUTPUT_DIR_REGEX.find(buildGradleContent)?.groupValues?.getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_OUTPUT_DIR

    /**
     * Resolves the build output directory `build/<outputDir>/` under
     * [projectDir], honoring the `outputDir` configured in
     * [buildGradleContent].
     */
    fun resolveBuildOutputPath(projectDir: File, buildGradleContent: String): File =
        File(projectDir, "build/${resolveOutputDir(buildGradleContent)}")

    /**
     * Resolves the subtitle file path `build/<outputDir>/<subtitleFileName>`
     * under [projectDir], honoring the `outputDir` configured in
     * [buildGradleContent].
     */
    fun resolveSubtitleFile(projectDir: File, buildGradleContent: String, subtitleFileName: String): File =
        File(resolveBuildOutputPath(projectDir, buildGradleContent), subtitleFileName)
}