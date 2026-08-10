package capsule.support

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for [CapsuleOutputDirResolver] — pure parsing of the `outputDir`
 * DSL property from the Gradle project build script content.
 *
 * Reproduces the burn-in E2E path mismatch (S-087): the step
 * `aSubtitleFileIsGeneratedInTheCapsuleOutputDirectory` hardcoded
 * `build/capsule/` while the E2E scenario configures `outputDir = "capsules"`,
 * producing `build/capsules/` — the subtitle file was never found.
 *
 * The resolver centralises outputDir extraction so steps resolve the actual
 * configured directory instead of hardcoding a single path.
 */
class CapsuleOutputDirResolverTest {

    @Test
    fun `resolves custom outputDir from capsule block`() {
        val buildGradle = """
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
                subtitleEnabled = true
                subtitleFormat = "srt"
                subtitleBurnIn = true
                ffmpegExecutablePath = "ffmpeg"
                outputDir = "capsules"
            }
        """.trimIndent()

        assertEquals("capsules", CapsuleOutputDirResolver.resolveOutputDir(buildGradle))
    }

    @Test
    fun `returns default when outputDir absent`() {
        val buildGradle = """
            plugins {
                id('education.cccp.capsule')
            }
            capsule {
                ttsEngine = "noop"
            }
        """.trimIndent()

        assertEquals("capsule", CapsuleOutputDirResolver.resolveOutputDir(buildGradle))
    }

    @Test
    fun `returns default when capsule block absent`() {
        val buildGradle = """
            plugins {
                id('education.cccp.capsule')
            }
        """.trimIndent()

        assertEquals("capsule", CapsuleOutputDirResolver.resolveOutputDir(buildGradle))
    }

    @Test
    fun `returns default for blank content`() {
        assertEquals("capsule", CapsuleOutputDirResolver.resolveOutputDir(""))
    }

    @Test
    fun `resolves outputDir with surrounding single quotes`() {
        val buildGradle = """
            capsule {
                outputDir = 'custom-out'
            }
        """.trimIndent()

        assertEquals("custom-out", CapsuleOutputDirResolver.resolveOutputDir(buildGradle))
    }

    @Test
    fun `resolves build output path under build directory`() {
        val buildGradle = """
            capsule {
                outputDir = "capsules"
            }
        """.trimIndent()

        val projectDir = createTempDir(prefix = "resolver-test")
        try {
            val path = CapsuleOutputDirResolver.resolveBuildOutputPath(projectDir, buildGradle)
            assertNotNull(path)
            assertEquals(File(projectDir, "build/capsules"), path)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `resolves default build output path when outputDir absent`() {
        val buildGradle = """
            capsule {
                ttsEngine = "noop"
            }
        """.trimIndent()

        val projectDir = createTempDir(prefix = "resolver-test")
        try {
            val path = CapsuleOutputDirResolver.resolveBuildOutputPath(projectDir, buildGradle)
            assertEquals(File(projectDir, "build/capsule"), path)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `resolves subtitle file path honoring configured outputDir`() {
        val buildGradle = """
            capsule {
                outputDir = "capsules"
            }
        """.trimIndent()

        val projectDir = createTempDir(prefix = "resolver-test")
        try {
            val subtitleFile = CapsuleOutputDirResolver.resolveSubtitleFile(projectDir, buildGradle, "burnin-e2e.srt")
            assertEquals(File(projectDir, "build/capsules/burnin-e2e.srt"), subtitleFile)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `resolves subtitle file path with default outputDir`() {
        val buildGradle = "capsule { ttsEngine = 'noop' }"

        val projectDir = createTempDir(prefix = "resolver-test")
        try {
            val subtitleFile = CapsuleOutputDirResolver.resolveSubtitleFile(projectDir, buildGradle, "deck.srt")
            assertEquals(File(projectDir, "build/capsule/deck.srt"), subtitleFile)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    private fun createTempDir(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), "$prefix-${System.currentTimeMillis()}").also { it.mkdirs() }
}