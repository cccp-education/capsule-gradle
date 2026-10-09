package capsule.viral

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Functional tests for the `generateViralCampaign` Gradle task (CAP-VIRAL wiring).
 *
 * Verifies:
 *  - task registration with the `generate` group
 *  - disabled-by-default no-op skip (economy of ink)
 *  - enabled + validated storyboard + explicit hook → storyboard document +
 *    `viral-campaign.json` manifest (bundles for the language × platform matrix)
 *  - enabled + storyboard required + missing storyboard → editorial gate fails
 *
 * The explicit hook short-circuits the LLM (economy of ink): zero network, zero
 * LLM pool, zero ffmpeg/node.
 */
class GenerateViralCampaignFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private fun setupBuild(extraConfig: String = "") {
        projectDir.resolve("settings.gradle").writeText("")
        projectDir.resolve("build.gradle").writeText("""
            plugins {
                id('education.cccp.capsule')
            }
            $extraConfig
        """.trimIndent())
    }

    private fun writeStoryboard() {
        projectDir.resolve("storyboard.adoc").writeText("""
            = Storyboard — Devenir formateur professionnel
            :platform: TIKTOK
            :language: fr
            :target-duration: 30

            == Angle

            Le métier en 30 secondes

            == Beats

            === Beat 1 — hook

            Intent: Et si 30 secondes suffisaient ?

            Duration: 3.0s
            Render: HTML

            === Beat 2 — development

            Intent: Les 4 activités types

            Duration: 10.0s
            Render: HTML

            === Beat 3 — development

            Intent: Le rôle du formateur

            Duration: 10.0s
            Render: HTML

            === Beat 4 — cta

            Intent: Découvrez FPA

            Duration: 7.0s
            Render: HTML
        """.trimIndent())
    }

    @Test
    fun `generateViralCampaign task is registered in the generate group`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("tasks", "--group", "generate")
            .withProjectDir(projectDir)
            .build()
        assertTrue(
            result.output.contains("generateViralCampaign"),
            "Expected generateViralCampaign in generate group, got: ${result.output}"
        )
    }

    @Test
    fun `disabled by default is a no-op skip`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("generateViralCampaign")
            .withProjectDir(projectDir)
            .build()
        assertTrue(
            result.output.contains("skipped") || result.output.contains("viralEnabled=false"),
            "Expected skip log when viralEnabled defaults to false, got: ${result.output}"
        )
    }

    @Test
    fun `enabled with a validated storyboard writes the campaign manifest`() {
        setupBuild()
        writeStoryboard()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateViralCampaign",
                "-Pcapsule.viral.enabled=true",
                "-Pcapsule.viral.storyboardFile=storyboard.adoc",
                "-Pcapsule.viral.hook=Et si 30 secondes suffisaient ?",
                "-Pcapsule.viral.cta=Découvrez FPA",
                "-Pcapsule.viral.languages=fr,en",
                "-Pcapsule.viral.platforms=TIKTOK,YOUTUBE_SHORT",
            )
            .withProjectDir(projectDir)
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateViralCampaign")?.outcome)

        val manifest = projectDir.resolve("build/capsule/viral/viral-campaign.json")
        assertTrue(manifest.exists(), "Expected viral-campaign.json, got: ${projectDir.resolve("build/capsule/viral").listFiles()?.joinToString { it.name }}")
        val content = manifest.readText()
        assertTrue(content.contains("\"language\" : \"fr\""), "Expected fr bundle, got: $content")
        assertTrue(content.contains("\"language\" : \"en\""), "Expected en bundle, got: $content")
        assertTrue(content.contains("TIKTOK"), "Expected TIKTOK platform, got: $content")
        assertTrue(content.contains("YOUTUBE_SHORT"), "Expected YOUTUBE_SHORT platform, got: $content")

        val storyboard = projectDir.resolve("build/capsule/viral/storyboard.adoc")
        assertTrue(storyboard.exists(), "Expected storyboard output document")
        assertTrue(storyboard.readText().contains("=== Beat 1 — hook"), "Expected beats in storyboard doc")
        assertTrue(result.output.contains("CAPSULE VIRAL"), "Expected summary log, got: ${result.output}")
    }

    @Test
    fun `enabled without a storyboard fails the editorial gate`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateViralCampaign",
                "-Pcapsule.viral.enabled=true",
                "-Pcapsule.viral.hook=Et si 30 secondes suffisaient ?",
            )
            .withProjectDir(projectDir)
            .buildAndFail()

        assertTrue(
            result.output.contains("no validated storyboard") || result.output.contains("storyboardRequired"),
            "Expected the editorial gate error, got: ${result.output}"
        )
    }

    @Test
    fun `enabled without storyboard but gate disabled synthesizes a storyboard from the hook`() {
        setupBuild()
        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments(
                "generateViralCampaign",
                "-Pcapsule.viral.enabled=true",
                "-Pcapsule.viral.storyboardRequired=false",
                "-Pcapsule.viral.hook=Et si 30 secondes suffisaient ?",
                "-Pcapsule.viral.cta=Découvrez FPA",
            )
            .withProjectDir(projectDir)
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateViralCampaign")?.outcome)
        val manifest = projectDir.resolve("build/capsule/viral/viral-campaign.json")
        assertTrue(manifest.exists(), "Expected a synthesized campaign manifest")
        assertTrue(manifest.readText().contains("Et si 30 secondes suffisaient ?"), "Expected the hook in the bundle metadata")
    }
}
