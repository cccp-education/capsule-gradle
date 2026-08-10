package capsule

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for [VideoDestinationResolver] — pure resolution of the
 * versioned video destination path from [OutputConfig].
 *
 * CAP-ARCH-7 : post-capture distribution copies the final WebM to a versioned
 * subdirectory under the configured [OutputConfig.videoDestinationDir], so
 * the user can compare capsule versions side-by-side.
 */
class VideoDestinationResolverTest {

    @Test
    fun `resolves timestamp versioned destination dir`() {
        val config = OutputConfig(
            videoDestinationDir = "/workspace/office/videos",
            versioning = VersioningStrategy.TIMESTAMP,
            versionPrefix = "v",
        )

        val destination = VideoDestinationResolver.resolveDestinationDir(config)

        assertNotNull(destination)
        assertEquals(File("/workspace/office/videos"), destination.parentFile)
        assertTrue(destination.name.startsWith("v"), "Timestamp version should start with prefix 'v', got: ${destination.name}")
    }

    @Test
    fun `timestamp version is ISO-8601 compact`() {
        val config = OutputConfig(versionPrefix = "v", versioning = VersioningStrategy.TIMESTAMP)

        val versionLabel = VideoDestinationResolver.resolveVersionLabel(config, emptyList())

        assertTrue(versionLabel.startsWith("v"), "Version label should start with prefix, got: $versionLabel")
        val timestampPart = versionLabel.removePrefix("v")
        assertTrue(timestampPart.length >= 12, "Timestamp should be at least 12 chars (yyyyMMddHHmm), got: $timestampPart")
    }

    @Test
    fun `incremental version starts at 1 when destination empty`() {
        val config = OutputConfig(versionPrefix = "v", versioning = VersioningStrategy.INCREMENTAL)

        val versionLabel = VideoDestinationResolver.resolveVersionLabel(config, existingVersions = emptyList())

        assertEquals("v1", versionLabel, "First incremental version should be v1")
    }

    @Test
    fun `incremental version increments from highest existing`() {
        val config = OutputConfig(versionPrefix = "v", versioning = VersioningStrategy.INCREMENTAL)
        val existing = listOf("v1", "v3", "v2")

        val versionLabel = VideoDestinationResolver.resolveVersionLabel(config, existing)

        assertEquals("v4", versionLabel, "Should increment from highest existing (v3 -> v4)")
    }

    @Test
    fun `incremental version handles non-numeric siblings`() {
        val config = OutputConfig(versionPrefix = "rev", versioning = VersioningStrategy.INCREMENTAL)
        val existing = listOf("rev1", "rev5", "not-a-version", "rev")

        val versionLabel = VideoDestinationResolver.resolveVersionLabel(config, existing)

        assertEquals("rev6", versionLabel, "Should increment from highest numeric rev (rev5 -> rev6)")
    }

    @Test
    fun `resolveDestinationDir with incremental strategy and existing versions`() {
        val config = OutputConfig(
            videoDestinationDir = "/videos",
            versioning = VersioningStrategy.INCREMENTAL,
            versionPrefix = "v",
        )

        val destination = VideoDestinationResolver.resolveDestinationDir(config, existingVersions = listOf("v1", "v2"))

        assertEquals(File("/videos/v3"), destination)
    }

    @Test
    fun `resolveDestinationFile returns the video file inside the versioned dir`() {
        val config = OutputConfig(
            videoDestinationDir = "/videos",
            versioning = VersioningStrategy.INCREMENTAL,
            versionPrefix = "v",
        )

        val destFile = VideoDestinationResolver.resolveDestinationFile(config, "my-deck.webm", existingVersions = listOf("v1"))

        assertEquals(File("/videos/v2/my-deck.webm"), destFile)
    }

    @Test
    fun `resolveDestinationDir creates parent when missing`() {
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "vdr-test-${System.currentTimeMillis()}")
        try {
            val config = OutputConfig(
                videoDestinationDir = tempRoot.absolutePath,
                versioning = VersioningStrategy.INCREMENTAL,
                versionPrefix = "v",
            )

            val destination = VideoDestinationResolver.resolveDestinationDir(config, existingVersions = emptyList())

            assertTrue(destination.parentFile.exists(), "Parent directory should be created if missing")
            assertEquals(File(tempRoot, "v1"), destination)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `collectExistingVersions lists versioned subdirectories matching prefix`() {
        val tempRoot = File(System.getProperty("java.io.tmpdir"), "vdr-list-${System.currentTimeMillis()}").also { it.mkdirs() }
        try {
            File(tempRoot, "v1").mkdirs()
            File(tempRoot, "v2").mkdirs()
            File(tempRoot, "not-a-version").mkdirs()
            File(tempRoot, "v1-deck.webm").writeText("fake")

            val versions = VideoDestinationResolver.collectExistingVersions(tempRoot, "v")

            assertTrue(versions.containsAll(listOf("v1", "v2")), "Should list v1 and v2, got: $versions")
            assertTrue(!versions.contains("not-a-version"), "Should not list non-matching dirs")
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    @Test
    fun `collectExistingVersions returns empty list when dir does not exist`() {
        val versions = VideoDestinationResolver.collectExistingVersions(File("/nonexistent/path"), "v")

        assertTrue(versions.isEmpty(), "Should return empty list for nonexistent dir")
    }
}