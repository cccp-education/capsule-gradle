package capsule.publish

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Functional acceptance for `publishCapsuleToYouTube` (HF-YOUTUBE-PUBLISH US-3b).
 *
 * Hermetic: the task is exercised through Gradle TestKit with publishing
 * **disabled or unconfigured**, so it never reaches the network — the YouTube
 * flow itself is covered by the unit tests + the `@publish` BDD feature (fake
 * transport).
 */
class PublishCapsuleToYouTubeFunctionalTest {
    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    @BeforeEach
    fun setUp() {
        settingsFile.writeText("")
        buildFile.writeText(
            """
            plugins {
                id('education.cccp.capsule')
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `plugin registers the publish task under the deploy group`() {
        val result = runner("tasks", "--group=deploy")

        assertContains(result.output, "publishCapsuleToYouTube")
    }

    @Test
    fun `publishing is disabled by default and writes a skipped report`() {
        val result = runner("publishCapsuleToYouTube")

        assertContains(result.output, "disabled")
        val json = report().readText()
        assertTrue(SKIPPED_TRUE.containsMatchIn(json), "report should be skipped=true: $json")
        assertContains(json, "disabled")
    }

    @Test
    fun `an enabled publish without a credentials file degrades without network`() {
        // A produced video + a title make the publication valid, so the resolver
        // is reached — it degrades (no credentials file) instead of failing.
        renderedVideo().writeText("MOCK-MP4")
        propertiesFile.writeText(
            """
            capsule.publish.enabled=true
            capsule.publish.title=A title
            capsule.publish.categoryId=27
            """.trimIndent(),
        )

        val result = runner("publishCapsuleToYouTube")

        assertContains(result.output, "degraded")
        val json = report().readText()
        assertTrue(SKIPPED_TRUE.containsMatchIn(json), "report should be skipped=true: $json")
        assertContains(json, "credentials")
    }

    private companion object {
        /** Spacing-insensitive match for `"skipped": true` (Jackson pretty-print). */
        val SKIPPED_TRUE = Regex(""""skipped"\s*:\s*true""")
    }

    @Test
    fun `the publish task depends on the chapter generation`() {
        val result = runner("publishCapsuleToYouTube", "--dry-run")

        assertContains(result.output, ":generateCapsuleChapters")
    }

    @Test
    fun `a generated chapters file feeds the publication description`() {
        // A chapter timeline that starts after 0 is invalid: reading it proves the
        // publication consumes the `generateCapsuleChapters` output.
        renderedVideo().writeText("MOCK-MP4")
        projectDir
            .resolve("build/capsule/chapters/chapters.txt")
            .apply { parentFile.mkdirs() }
            .writeText("0:30 Late\n1:30 End\n")
        propertiesFile.writeText(
            """
            capsule.publish.enabled=true
            capsule.publish.title=A title
            capsule.publish.categoryId=27
            """.trimIndent(),
        )

        val result = runner("publishCapsuleToYouTube")

        assertContains(result.output, "publication invalid")
        assertContains(report().readText(), "publication invalid")
    }

    @Test
    fun `a valid generated chapter timeline keeps the publication valid`() {
        renderedVideo().writeText("MOCK-MP4")
        projectDir
            .resolve("build/capsule/chapters/chapters.txt")
            .apply { parentFile.mkdirs() }
            .writeText("0:00 Intro\n1:00 Part\n2:00 End\n")
        propertiesFile.writeText(
            """
            capsule.publish.enabled=true
            capsule.publish.title=A title
            capsule.publish.categoryId=27
            """.trimIndent(),
        )

        val result = runner("publishCapsuleToYouTube")

        assertContains(result.output, "degraded")
        assertContains(report().readText(), "credentials")
    }

    private fun renderedVideo(): File = projectDir.resolve("build/capsule/test-video.mp4").apply { parentFile.mkdirs() }

    private fun report(): File = projectDir.resolve("build/capsule/publish/publication.json")

    private fun runner(vararg args: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*args)
            .build()
}
