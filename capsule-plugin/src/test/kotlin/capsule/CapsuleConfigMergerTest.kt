package capsule

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for CapsuleConfigMerger.
 *
 * Precedence order: ENV vars < gradle.properties < YAML file < CLI -P params
 * Each higher-priority source overrides the same key from lower-priority sources.
 */
class CapsuleConfigMergerTest {

    @TempDir
    lateinit var tempDir: File

    // ─── loadFromGradleProperties ────────────────────────────────

    @Test
    fun `loadFromGradleProperties reads capsule dot-prefixed properties`() {
        val projectDir = File(tempDir, "project").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
            capsule.tts.espeakVoice=de
            capsule.tts.espeakSpeed=180
            capsule.capture.viewportWidth=1920
            capsule.capture.parallelCaptureEnabled=true
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals("espeak", config.tts.engine)
        assertEquals("de", config.tts.espeakVoice)
        assertEquals(180, config.tts.espeakSpeed)
        assertEquals(1920, config.capture.viewportWidth)
        assertEquals(true, config.capture.parallelCaptureEnabled)
    }

    @Test
    fun `loadFromGradleProperties returns defaults when no gradle properties file`() {
        val projectDir = File(tempDir, "empty-project").also { it.mkdirs() }
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals("piper", config.tts.engine)
        assertEquals(1408, config.capture.viewportWidth)
        assertEquals(false, config.capture.parallelCaptureEnabled)
    }

    @Test
    fun `loadFromGradleProperties ignores non-capsule properties`() {
        val projectDir = File(tempDir, "mixed").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            org.gradle.jvmargs=-Xmx2g
            capsule.tts.engine=espeak
            someOtherProperty=value
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals("espeak", config.tts.engine, "capsule.tts.engine should be read")
        assertEquals("fr_FR-siwis-medium", config.tts.voice, "tts.voice should be default when not in properties")
    }

    @Test
    fun `loadFromGradleProperties ignores commented lines`() {
        val projectDir = File(tempDir, "comments").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            # capsule.tts.engine=piper
            capsule.tts.engine=espeak
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals("espeak", config.tts.engine)
    }

    @Test
    fun `loadFromGradleProperties reads distrib and manim sections`() {
        val projectDir = File(tempDir, "dist-man").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.distrib.ffmpegExecutablePath=/usr/local/bin/ffmpeg
            capsule.distrib.outputWidth=720
            capsule.manim.quality=h
            capsule.manim.scriptsDir=manim/scripts
            capsule.manim.outputDir=custom/manim-output
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals("/usr/local/bin/ffmpeg", config.distrib.ffmpegExecutablePath)
        assertEquals(720, config.distrib.outputWidth)
        assertEquals("h", config.manim.quality)
        assertEquals("manim/scripts", config.manim.scriptsDir)
        assertEquals("custom/manim-output", config.manim.outputDir)
    }

    // ─── loadFromEnvironment ─────────────────────────────────────

    @Test
    fun `loadFromEnvironment reads CAPSULE_ prefixed env vars`() {
        // We cannot easily set env vars in tests, so we test the mapping
        // by verifying default values when env vars are absent
        val config = CapsuleConfigMerger.loadFromEnvironment()

        // When no CAPSULE_* env vars are set, defaults should match CapsuleConfig() defaults
        assertEquals("piper", config.tts.engine)
        assertEquals(1408, config.capture.viewportWidth)
    }

    @Test
    fun `loadFromEnvironment resolves env vars with fallbacks`() {
        // Verify the env mapping pattern is correct by checking defaults
        // when no CAPSULE_* environment variables are set
        val config = CapsuleConfigMerger.loadFromEnvironment()
        // Default fallbacks must match CapsuleConfig() defaults
        assertEquals("fr_FR-siwis-medium", config.tts.voice)
        assertEquals("fr", config.tts.espeakVoice)
        assertEquals(150, config.tts.espeakSpeed)
        assertEquals(792, config.capture.viewportHeight)
        assertEquals(120_000.0, config.capture.playwrightTimeout)
        assertEquals(5.0, config.capture.slideDurationSeconds)
        assertEquals(false, config.capture.parallelCaptureEnabled)
        assertEquals("ffmpeg", config.distrib.ffmpegExecutablePath)
        assertEquals(1080, config.distrib.outputWidth)
        assertEquals(1920, config.distrib.outputHeight)
        assertEquals("manim", config.manim.executablePath)
        assertEquals("l", config.manim.quality)
        assertEquals("src/manim", config.manim.scriptsDir)
        assertEquals("build/capsule/manim", config.manim.outputDir)
        assertEquals("capsule", config.input.outputDir)
        assertEquals("capsule", config.input.sliderScriptDir)
        assertEquals("docs/asciidocRevealJs", config.input.deckSourceDir)
        assertEquals("", config.input.chromiumExecutablePath)
        assertEquals("piper", config.tts.piperExecutablePath)
        assertEquals(true, config.tts.fallbackEnabled)
    }

    // ─── merge (4 sources) ───────────────────────────────────────

    @Test
    fun `merge returns yaml config when no props or cli`() {
        val projectDir = File(tempDir, "merge1").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "espeak", espeakVoice = "de"),
            capture = CaptureConfig(viewportWidth = 1920, viewportHeight = 1080)
        )

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("espeak", merged.tts.engine)
        assertEquals("de", merged.tts.espeakVoice)
        assertEquals(1920, merged.capture.viewportWidth)
        assertEquals(1080, merged.capture.viewportHeight)
    }

    @Test
    fun `merge CLI params override YAML config`() {
        val projectDir = File(tempDir, "merge2").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "espeak")
        )
        val cliParams = mapOf("tts.engine" to "piper")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals("piper", merged.tts.engine, "CLI should override YAML")
    }

    @Test
    fun `merge YAML overrides gradle properties`() {
        val projectDir = File(tempDir, "merge3").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
            capsule.capture.viewportWidth=800
        """.trimIndent())

        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "piper"),
            capture = CaptureConfig(viewportWidth = 1920)
        )

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("piper", merged.tts.engine, "YAML should override gradle.properties")
        assertEquals(1920, merged.capture.viewportWidth, "YAML should override gradle.properties")
    }

    @Test
    fun `merge CLI overrides both YAML and gradle properties`() {
        val projectDir = File(tempDir, "merge4").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
        """.trimIndent())

        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "piper")
        )
        val cliParams = mapOf("tts.engine" to "noop")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals("noop", merged.tts.engine, "CLI should override both")
    }

    @Test
    fun `merge uses defaults when all sources are empty`() {
        val projectDir = File(tempDir, "merge5").also { it.mkdirs() }

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap())

        assertEquals("piper", merged.tts.engine)
        assertEquals("fr_FR-siwis-medium", merged.tts.voice)
        assertEquals(1408, merged.capture.viewportWidth)
        assertEquals(792, merged.capture.viewportHeight)
        assertEquals(false, merged.capture.parallelCaptureEnabled)
    }

    @Test
    fun `merge handles partial CLI override`() {
        val projectDir = File(tempDir, "merge6").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "espeak", espeakVoice = "de", espeakSpeed = 120),
            capture = CaptureConfig(viewportWidth = 1920, parallelCaptureEnabled = true)
        )
        val cliParams = mapOf("tts.engine" to "piper")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals("piper", merged.tts.engine, "CLI overrides tts.engine only")
        assertEquals("de", merged.tts.espeakVoice, "YAML should pass through when CLI doesn't override")
        assertEquals(120, merged.tts.espeakSpeed, "YAML should pass through")
        assertEquals(1920, merged.capture.viewportWidth, "YAML should pass through")
        assertEquals(true, merged.capture.parallelCaptureEnabled, "YAML should pass through")
    }

    @Test
    fun `merge CLI integer param overrides YAML`() {
        val projectDir = File(tempDir, "merge7").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            capture = CaptureConfig(viewportWidth = 800)
        )
        val cliParams = mapOf("capture.viewportWidth" to 1920)

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(1920, merged.capture.viewportWidth)
    }

    @Test
    fun `merge CLI boolean param overrides YAML`() {
        val projectDir = File(tempDir, "merge8").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            capture = CaptureConfig(parallelCaptureEnabled = false)
        )
        val cliParams = mapOf("capture.parallelCaptureEnabled" to true)

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(true, merged.capture.parallelCaptureEnabled)
    }

    @Test
    fun `merge gradle properties override ENV defaults when no YAML loaded`() {
        // When no YAML file exists, gradle.properties is the lowest explicit source.
        // Verify that gradle.properties values come through when yamlLoaded=false
        val projectDir = File(tempDir, "merge9").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.voice=custom-voice
            capsule.tts.espeakSpeed=200
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("custom-voice", merged.tts.voice, "gradle.properties should provide value when no YAML")
        assertEquals(200, merged.tts.espeakSpeed, "gradle.properties should provide value when no YAML")
    }

    @Test
    fun `merge handles all 4 precedence levels for same key`() {
        // ENV < gradle.properties < YAML < CLI
        // We can only test props < YAML < CLI (ENV testing is hard in JVM)
        val projectDir = File(tempDir, "merge10").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
        """.trimIndent())

        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "piper")
        )

        // Level 1: YAML overrides props (no CLI)
        val merged1 = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals("piper", merged1.tts.engine)

        // Level 2: CLI overrides both
        val merged2 = CapsuleConfigMerger.merge(projectDir, yamlConfig, mapOf("tts.engine" to "noop"))
        assertEquals("noop", merged2.tts.engine)
    }

    @Test
    fun `merge preserves non-overridden sections from YAML`() {
        val projectDir = File(tempDir, "merge11").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
        """.trimIndent())

        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "piper", voice = "custom-voice"),
            capture = CaptureConfig(viewportWidth = 1920, viewportHeight = 1080),
            distrib = DistribConfig(outputWidth = 720)
        )

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("piper", merged.tts.engine)
        assertEquals("custom-voice", merged.tts.voice)
        assertEquals(1920, merged.capture.viewportWidth)
        assertEquals(1080, merged.capture.viewportHeight)
        assertEquals(720, merged.distrib.outputWidth)
    }

    @Test
    fun `merge CLI double param overrides YAML`() {
        val projectDir = File(tempDir, "merge12").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(
            capture = CaptureConfig(playwrightTimeout = 60_000.0)
        )
        val cliParams = mapOf("capture.playwrightTimeout" to 30_000.0)

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(30_000.0, merged.capture.playwrightTimeout)
    }

    // ─── Edge cases ──────────────────────────────────────────────

    @Test
    fun `merge with empty gradle properties file uses YAML then defaults`() {
        val projectDir = File(tempDir, "edge1").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("")

        val yamlConfig = CapsuleConfig(
            tts = TtsConfig(engine = "espeak")
        )

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("espeak", merged.tts.engine)
        assertEquals("fr_FR-siwis-medium", merged.tts.voice) // default
    }

    @Test
    fun `merge with malformed gradle properties line skips it`() {
        val projectDir = File(tempDir, "edge2").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
            capsule.tts.espeakSpeed
            capsule.tts.espeakVoice=de
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals("espeak", config.tts.engine)
        assertEquals("de", config.tts.espeakVoice)
        assertEquals(150, config.tts.espeakSpeed) // default (malformed line skipped)
    }

    // ─── Manim outputDir merge tests ────────────────────────────

    @Test
    fun `merge handles manim outputDir via CLI`() {
        val projectDir = File(tempDir, "manim-cli").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(manim = ManimConfig(outputDir = "yaml/manim"))
        val cliParams = mapOf("manim.outputDir" to "cli/manim")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals("cli/manim", merged.manim.outputDir, "CLI should override YAML for manim.outputDir")
    }

    @Test
    fun `merge handles manim outputDir via YAML`() {
        val projectDir = File(tempDir, "manim-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(manim = ManimConfig(outputDir = "custom/manim/output"))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("custom/manim/output", merged.manim.outputDir, "YAML manim.outputDir should be preserved")
    }

    @Test
    fun `loadFromEnvironment resolves manim outputDir with fallbacks`() {
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals("build/capsule/manim", config.manim.outputDir, "Default manim.outputDir should be build/capsule/manim")
    }

    // ─── Manim parallelRender merge tests ─────────────────────────

    @Test
    fun `ManimConfig default parallelRender is false via merger`() {
        val projectDir = File(tempDir, "manim-parallel-default").also { it.mkdirs() }
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap())
        assertEquals(false, merged.manim.parallelRender, "Default manim.parallelRender should be false")
    }

    @Test
    fun `merge handles manim parallelRender via CLI`() {
        val projectDir = File(tempDir, "manim-parallel-cli").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(manim = ManimConfig(parallelRender = false))
        val cliParams = mapOf("manim.parallelRender" to "true")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(true, merged.manim.parallelRender, "CLI should override YAML for manim.parallelRender")
    }

    @Test
    fun `merge handles manim parallelRender via YAML`() {
        val projectDir = File(tempDir, "manim-parallel-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(manim = ManimConfig(parallelRender = true))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals(true, merged.manim.parallelRender, "YAML manim.parallelRender should be preserved")
    }

    @Test
    fun `loadFromEnvironment resolves manim parallelRender with default false`() {
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals(false, config.manim.parallelRender, "Default manim.parallelRender from ENV should be false")
    }

    @Test
    fun `loadFromGradleProperties reads manim parallelRender`() {
        val projectDir = File(tempDir, "manim-parallel-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.manim.parallelRender=true
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(true, config.manim.parallelRender, "Props should set manim.parallelRender to true")
    }
}