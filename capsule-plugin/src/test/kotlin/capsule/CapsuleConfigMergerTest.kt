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

    // ─── Capture timeout (CR-2.3) merge tests ─────────────────────

    @Test
    fun `loadFromGradleProperties reads captureTimeoutMinutes`() {
        val projectDir = File(tempDir, "timeout-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.captureTimeoutMinutes=2
        """.trimIndent())

        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)

        assertEquals(2, config.capture.captureTimeoutMinutes, "Props should set capture.captureTimeoutMinutes to 2")
    }

    @Test
    fun `merge handles captureTimeoutMinutes via CLI`() {
        val projectDir = File(tempDir, "timeout-cli").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(capture = CaptureConfig(captureTimeoutMinutes = 5))
        val cliParams = mapOf("capture.captureTimeoutMinutes" to 3)

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(3, merged.capture.captureTimeoutMinutes, "CLI should override YAML for capture.captureTimeoutMinutes")
    }

    @Test
    fun `merge handles captureTimeoutMinutes via YAML`() {
        val projectDir = File(tempDir, "timeout-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(capture = CaptureConfig(captureTimeoutMinutes = 10))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals(10, merged.capture.captureTimeoutMinutes, "YAML capture.captureTimeoutMinutes should be preserved")
    }

    @Test
    fun `loadFromEnvironment resolves captureTimeoutMinutes with default 5`() {
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals(5, config.capture.captureTimeoutMinutes, "Default capture.captureTimeoutMinutes from ENV should be 5")
    }

    @Test
    fun `merge CLI string integer param is parsed as Int`() {
        val projectDir = File(tempDir, "merge-cli-str-int").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig()
        val cliParams = mapOf("capture.viewportWidth" to "1920")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(1920, merged.capture.viewportWidth, "CLI String \"1920\" must be parsed as Int 1920")
    }

    @Test
    fun `merge CLI string double param is parsed as Double`() {
        val projectDir = File(tempDir, "merge-cli-str-double").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig()
        val cliParams = mapOf("capture.slideDurationSeconds" to "7.5")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(7.5, merged.capture.slideDurationSeconds, "CLI String \"7.5\" must be parsed as Double 7.5")
    }

    @Test
    fun `merge CLI string boolean param is parsed as Boolean`() {
        val projectDir = File(tempDir, "merge-cli-str-bool").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig()
        val cliParams = mapOf("capture.parallelCaptureEnabled" to "true")

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, cliParams)

        assertEquals(true, merged.capture.parallelCaptureEnabled, "CLI String \"true\" must be parsed as Boolean true")
    }

    // ─── No-YAML path coverage (CR-5) ────────────────────────────
    //
    // yamlLoaded=false means the YAML file was not found. The merger must
    // fall back to props > ENV, with CLI on top. These tests pin that path
    // for each of the 5 sections so the DRY refactor cannot silently
    // regress the No-YAML branch.

    @Test
    fun `merge NoYaml uses props input section when CLI absent`() {
        val projectDir = File(tempDir, "no-yaml-input").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.input.outputDir=props-output
            capsule.input.deckSourceDir=props-decks
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("props-output", merged.input.outputDir, "NoYaml: props input.outputDir should win over ENV default")
        assertEquals("props-decks", merged.input.deckSourceDir, "NoYaml: props input.deckSourceDir should win over ENV default")
        assertEquals("capsule", merged.input.sliderScriptDir, "NoYaml: input.sliderScriptDir should fall back to default")
    }

    @Test
    fun `merge NoYaml CLI overrides props input section`() {
        val projectDir = File(tempDir, "no-yaml-input-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.input.outputDir=props-output
        """.trimIndent())
        val cliParams = mapOf("input.outputDir" to "cli-output")

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), cliParams, yamlLoaded = false)

        assertEquals("cli-output", merged.input.outputDir, "NoYaml: CLI should override props input.outputDir")
    }

    @Test
    fun `merge NoYaml uses props tts section when CLI absent`() {
        val projectDir = File(tempDir, "no-yaml-tts").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
            capsule.tts.espeakVoice=de
            capsule.tts.espeakSpeed=180
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("espeak", merged.tts.engine, "NoYaml: props tts.engine should win")
        assertEquals("de", merged.tts.espeakVoice, "NoYaml: props tts.espeakVoice should win")
        assertEquals(180, merged.tts.espeakSpeed, "NoYaml: props tts.espeakSpeed should win")
    }

    @Test
    fun `merge NoYaml CLI overrides props tts section`() {
        val projectDir = File(tempDir, "no-yaml-tts-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
        """.trimIndent())
        val cliParams = mapOf("tts.engine" to "piper", "tts.espeakSpeed" to 200)

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), cliParams, yamlLoaded = false)

        assertEquals("piper", merged.tts.engine, "NoYaml: CLI should override props tts.engine")
        assertEquals(200, merged.tts.espeakSpeed, "NoYaml: CLI should override default tts.espeakSpeed")
    }

    @Test
    fun `merge NoYaml uses props capture section when CLI absent`() {
        val projectDir = File(tempDir, "no-yaml-capture").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.viewportWidth=1920
            capsule.capture.viewportHeight=1080
            capsule.capture.parallelCaptureEnabled=true
            capsule.capture.captureTimeoutMinutes=3
            capsule.capture.subtitleFormat=vtt
            capsule.capture.subtitleBurnIn=true
            capsule.capture.subtitleBurnInFontSize=32
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals(1920, merged.capture.viewportWidth, "NoYaml: props capture.viewportWidth should win")
        assertEquals(1080, merged.capture.viewportHeight, "NoYaml: props capture.viewportHeight should win")
        assertEquals(true, merged.capture.parallelCaptureEnabled, "NoYaml: props capture.parallelCaptureEnabled should win")
        assertEquals(3, merged.capture.captureTimeoutMinutes, "NoYaml: props capture.captureTimeoutMinutes should win")
        assertEquals("vtt", merged.capture.subtitleFormat, "NoYaml: props capture.subtitleFormat should win over ENV default")
        assertEquals(true, merged.capture.subtitleBurnIn, "NoYaml: props capture.subtitleBurnIn should win")
        assertEquals(32, merged.capture.subtitleBurnInFontSize, "NoYaml: props capture.subtitleBurnInFontSize should win")
    }

    @Test
    fun `merge NoYaml CLI overrides props capture section`() {
        val projectDir = File(tempDir, "no-yaml-capture-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.viewportWidth=1920
        """.trimIndent())
        val cliParams = mapOf(
            "capture.viewportWidth" to 800,
            "capture.parallelCaptureEnabled" to true,
            "capture.subtitleFormat" to "vtt"
        )

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), cliParams, yamlLoaded = false)

        assertEquals(800, merged.capture.viewportWidth, "NoYaml: CLI should override props capture.viewportWidth")
        assertEquals(true, merged.capture.parallelCaptureEnabled, "NoYaml: CLI should override default capture.parallelCaptureEnabled")
        assertEquals("vtt", merged.capture.subtitleFormat, "NoYaml: CLI should override default capture.subtitleFormat")
    }

    @Test
    fun `merge NoYaml uses props distrib section when CLI absent`() {
        val projectDir = File(tempDir, "no-yaml-distrib").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.distrib.ffmpegExecutablePath=/usr/bin/ffmpeg
            capsule.distrib.outputWidth=720
            capsule.distrib.outputHeight=1280
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("/usr/bin/ffmpeg", merged.distrib.ffmpegExecutablePath, "NoYaml: props distrib.ffmpegExecutablePath should win")
        assertEquals(720, merged.distrib.outputWidth, "NoYaml: props distrib.outputWidth should win")
        assertEquals(1280, merged.distrib.outputHeight, "NoYaml: props distrib.outputHeight should win")
    }

    @Test
    fun `merge NoYaml CLI overrides props distrib section`() {
        val projectDir = File(tempDir, "no-yaml-distrib-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.distrib.outputWidth=720
        """.trimIndent())
        val cliParams = mapOf("distrib.outputWidth" to 1080, "distrib.outputHeight" to 1920)

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), cliParams, yamlLoaded = false)

        assertEquals(1080, merged.distrib.outputWidth, "NoYaml: CLI should override props distrib.outputWidth")
        assertEquals(1920, merged.distrib.outputHeight, "NoYaml: CLI should override default distrib.outputHeight")
    }

    @Test
    fun `merge NoYaml uses props manim section when CLI absent`() {
        val projectDir = File(tempDir, "no-yaml-manim").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.manim.executablePath=/opt/manim
            capsule.manim.quality=h
            capsule.manim.scriptsDir=custom/manim
            capsule.manim.parallelRender=true
            capsule.manim.parallelRenderThreads=8
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("/opt/manim", merged.manim.executablePath, "NoYaml: props manim.executablePath should win")
        assertEquals("h", merged.manim.quality, "NoYaml: props manim.quality should win")
        assertEquals("custom/manim", merged.manim.scriptsDir, "NoYaml: props manim.scriptsDir should win")
        assertEquals(true, merged.manim.parallelRender, "NoYaml: props manim.parallelRender should win")
        assertEquals(8, merged.manim.parallelRenderThreads, "NoYaml: props manim.parallelRenderThreads should win")
    }

    @Test
    fun `merge NoYaml CLI overrides props manim section`() {
        val projectDir = File(tempDir, "no-yaml-manim-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.manim.quality=h
        """.trimIndent())
        val cliParams = mapOf("manim.quality" to "k", "manim.parallelRender" to true)

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), cliParams, yamlLoaded = false)

        assertEquals("k", merged.manim.quality, "NoYaml: CLI should override props manim.quality")
        assertEquals(true, merged.manim.parallelRender, "NoYaml: CLI should override default manim.parallelRender")
    }

    @Test
    fun `merge NoYaml with no props and no CLI returns defaults`() {
        val projectDir = File(tempDir, "no-yaml-defaults").also { it.mkdirs() }

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("piper", merged.tts.engine, "NoYaml: default tts.engine should be piper")
        assertEquals(1408, merged.capture.viewportWidth, "NoYaml: default capture.viewportWidth should be 1408")
        assertEquals("ffmpeg", merged.distrib.ffmpegExecutablePath, "NoYaml: default distrib.ffmpegExecutablePath should be ffmpeg")
        assertEquals("manim", merged.manim.executablePath, "NoYaml: default manim.executablePath should be manim")
        assertEquals("capsule", merged.input.outputDir, "NoYaml: default input.outputDir should be capsule")
    }

    @Test
    fun `merge YAML blank string falls back to props then env`() {
        // YAML may explicitly set a blank string. The merger must NOT let a
        // blank YAML override a non-blank props/env value (isNotBlank heuristic).
        val projectDir = File(tempDir, "yaml-blank-fallback").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.tts.engine=espeak
        """.trimIndent())
        val yamlConfig = CapsuleConfig(tts = TtsConfig(engine = ""))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("espeak", merged.tts.engine, "Blank YAML string should fall back to props (isNotBlank heuristic)")
    }

    // ─── OutputConfig (CAP-ARCH-7 video destination versioning) ──────

    @Test
    fun `OutputConfig defaults are resolved when no source provides values`() {
        val projectDir = File(tempDir, "output-defaults").also { it.mkdirs() }

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("office/videos", merged.output.videoDestinationDir, "Default output.videoDestinationDir should be office/videos")
        assertEquals(VersioningStrategy.TIMESTAMP, merged.output.versioning, "Default output.versioning should be TIMESTAMP")
        assertEquals("v", merged.output.versionPrefix, "Default output.versionPrefix should be v")
    }

    @Test
    fun `OutputConfig videoDestinationDir is read from YAML`() {
        val projectDir = File(tempDir, "output-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(output = OutputConfig(videoDestinationDir = "/home/cheroliv/workspace/office/videos"))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("/home/cheroliv/workspace/office/videos", merged.output.videoDestinationDir)
    }

    @Test
    fun `OutputConfig videoDestinationDir is read from gradle properties`() {
        val projectDir = File(tempDir, "output-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.output.videoDestinationDir=/custom/videos
        """.trimIndent())

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)

        assertEquals("/custom/videos", merged.output.videoDestinationDir)
    }

    @Test
    fun `OutputConfig videoDestinationDir is read from env`() {
        // We cannot easily set env vars in tests (see existing loadFromEnvironment tests),
        // so we verify the env mapping produces defaults when no CAPSULE_OUTPUT_* is set.
        val config = CapsuleConfigMerger.loadFromEnvironment()

        assertEquals("office/videos", config.output.videoDestinationDir, "env default should be office/videos")
        assertEquals(VersioningStrategy.TIMESTAMP, config.output.versioning, "env default versioning should be TIMESTAMP")
        assertEquals("v", config.output.versionPrefix, "env default versionPrefix should be v")
    }

    @Test
    fun `OutputConfig CLI overrides YAML`() {
        val projectDir = File(tempDir, "output-cli").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(output = OutputConfig(videoDestinationDir = "/yaml/videos"))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, mapOf("output.videoDestinationDir" to "/cli/videos"))

        assertEquals("/cli/videos", merged.output.videoDestinationDir, "CLI should override YAML for videoDestinationDir")
    }

    @Test
    fun `OutputConfig versioning is read from YAML`() {
        val projectDir = File(tempDir, "output-versioning-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(output = OutputConfig(versioning = VersioningStrategy.INCREMENTAL))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals(VersioningStrategy.INCREMENTAL, merged.output.versioning)
    }

    @Test
    fun `OutputConfig versioning is read from CLI`() {
        val projectDir = File(tempDir, "output-versioning-cli").also { it.mkdirs() }

        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), mapOf("output.versioning" to "incremental"))

        assertEquals(VersioningStrategy.INCREMENTAL, merged.output.versioning)
    }

    @Test
    fun `OutputConfig versionPrefix is read from YAML`() {
        val projectDir = File(tempDir, "output-prefix-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(output = OutputConfig(versionPrefix = "rev"))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("rev", merged.output.versionPrefix)
    }

    @Test
    fun `OutputConfig videoDestinationDir blank YAML falls back to props`() {
        val projectDir = File(tempDir, "output-blank-yaml").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.output.videoDestinationDir=/props/videos
        """.trimIndent())
        val yamlConfig = CapsuleConfig(output = OutputConfig(videoDestinationDir = ""))

        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())

        assertEquals("/props/videos", merged.output.videoDestinationDir, "Blank YAML should fall back to props")
    }

    // ─── StrictModeConfig (CAP-CR3-2) ────────────────────────────

    @Test
    fun `default merge has strictMode disabled`() {
        val projectDir = File(tempDir, "strict-default").also { it.mkdirs() }
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap())
        assertEquals(false, merged.strictMode.enabled, "strictMode should default to false")
    }

    @Test
    fun `strictMode is read from YAML`() {
        val projectDir = File(tempDir, "strict-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(strictMode = StrictModeConfig(enabled = true))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(true, merged.strictMode.enabled, "YAML strictMode should be honored")
    }

    @Test
    fun `strictMode CLI overrides YAML`() {
        val projectDir = File(tempDir, "strict-cli-over-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(strictMode = StrictModeConfig(enabled = true))
        val merged = CapsuleConfigMerger.merge(
            projectDir, yamlConfig, mapOf("strictMode.enabled" to "false")
        )
        assertEquals(false, merged.strictMode.enabled, "CLI false should override YAML true")
    }

    @Test
    fun `strictMode CLI true overrides YAML false`() {
        val projectDir = File(tempDir, "strict-cli-true").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(strictMode = StrictModeConfig(enabled = false))
        val merged = CapsuleConfigMerger.merge(
            projectDir, yamlConfig, mapOf("strictMode.enabled" to "true")
        )
        assertEquals(true, merged.strictMode.enabled, "CLI true should override YAML false")
    }

    @Test
    fun `strictMode is read from gradle properties`() {
        val projectDir = File(tempDir, "strict-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.strictMode.enabled=true
        """.trimIndent())
        // No YAML file → yamlLoaded=false so props can take effect
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)
        assertEquals(true, merged.strictMode.enabled, "props strictMode should be honored when no YAML")
    }

    @Test
    fun `strictMode YAML overrides gradle properties`() {
        val projectDir = File(tempDir, "strict-yaml-over-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.strictMode.enabled=false
        """.trimIndent())
        val yamlConfig = CapsuleConfig(strictMode = StrictModeConfig(enabled = true))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(true, merged.strictMode.enabled, "YAML should override props")
    }

    @Test
    fun `strictMode CLI overrides gradle properties when no YAML`() {
        val projectDir = File(tempDir, "strict-cli-over-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.strictMode.enabled=false
        """.trimIndent())
        val merged = CapsuleConfigMerger.merge(
            projectDir, CapsuleConfig(), mapOf("strictMode.enabled" to "true"),
            yamlLoaded = false
        )
        assertEquals(true, merged.strictMode.enabled, "CLI should override props when no YAML")
    }

    @Test
    fun `loadFromEnvironment reads CAPSULE_STRICT_MODE_ENABLED`() {
        // We cannot set env vars in unit tests, so we verify the default.
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals(false, config.strictMode.enabled, "env default strictMode should be false")
    }

    @Test
    fun `loadFromGradleProperties reads capsule strictMode enabled`() {
        val projectDir = File(tempDir, "strict-props-load").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.strictMode.enabled=true
        """.trimIndent())
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(true, config.strictMode.enabled, "loadFromGradleProperties should read strictMode")
    }

    // ─── ContextConfig / docsGlobs (CAP-DOCCONTEXT-1) ──────────────

    @Test
    fun `default merge has empty docsGlobs`() {
        val projectDir = File(tempDir, "ctx-default").also { it.mkdirs() }
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap())
        assertEquals(emptyList<String>(), merged.context.docsGlobs, "context.docsGlobs should default to empty")
    }

    @Test
    fun `docsGlobs is read from YAML`() {
        val projectDir = File(tempDir, "ctx-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(context = ContextConfig(docsGlobs = listOf("docs/afnor/**/*.adoc")))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(listOf("docs/afnor/**/*.adoc"), merged.context.docsGlobs, "YAML docsGlobs should be honored")
    }

    @Test
    fun `docsGlobs CLI comma-split overrides YAML`() {
        val projectDir = File(tempDir, "ctx-cli-over-yaml").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(context = ContextConfig(docsGlobs = listOf("docs/old/**/*.adoc")))
        val merged = CapsuleConfigMerger.merge(
            projectDir, yamlConfig, mapOf("context.docsGlobs" to "docs/afnor/**/*.adoc,docs/reac/**/*.adoc")
        )
        assertEquals(
            listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"),
            merged.context.docsGlobs,
            "CLI comma-split should override YAML"
        )
    }

    @Test
    fun `docsGlobs single CLI value is read`() {
        val projectDir = File(tempDir, "ctx-cli-single").also { it.mkdirs() }
        val merged = CapsuleConfigMerger.merge(
            projectDir, CapsuleConfig(), mapOf("context.docsGlobs" to "docs/afnor/**/*.adoc")
        )
        assertEquals(listOf("docs/afnor/**/*.adoc"), merged.context.docsGlobs, "CLI single glob")
    }

    @Test
    fun `docsGlobs is read from gradle properties`() {
        val projectDir = File(tempDir, "ctx-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.context.docsGlobs=docs/afnor/**/*.adoc,docs/reac/**/*.adoc
        """.trimIndent())
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap(), yamlLoaded = false)
        assertEquals(
            listOf("docs/afnor/**/*.adoc", "docs/reac/**/*.adoc"),
            merged.context.docsGlobs,
            "props docsGlobs comma-split should be honored when no YAML"
        )
    }

    @Test
    fun `docsGlobs YAML overrides gradle properties`() {
        val projectDir = File(tempDir, "ctx-yaml-over-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.context.docsGlobs=docs/old/**/*.adoc
        """.trimIndent())
        val yamlConfig = CapsuleConfig(context = ContextConfig(docsGlobs = listOf("docs/afnor/**/*.adoc")))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(listOf("docs/afnor/**/*.adoc"), merged.context.docsGlobs, "YAML should override props")
    }

    @Test
    fun `docsGlobs CLI overrides gradle properties when no YAML`() {
        val projectDir = File(tempDir, "ctx-cli-over-props").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.context.docsGlobs=docs/old/**/*.adoc
        """.trimIndent())
        val merged = CapsuleConfigMerger.merge(
            projectDir, CapsuleConfig(), mapOf("context.docsGlobs" to "docs/new/**/*.adoc"),
            yamlLoaded = false
        )
        assertEquals(listOf("docs/new/**/*.adoc"), merged.context.docsGlobs, "CLI should override props when no YAML")
    }

    @Test
    fun `docsGlobs empty CLI string falls back to YAML`() {
        val projectDir = File(tempDir, "ctx-empty-cli").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(context = ContextConfig(docsGlobs = listOf("docs/afnor/**/*.adoc")))
        val merged = CapsuleConfigMerger.merge(
            projectDir, yamlConfig, mapOf("context.docsGlobs" to "")
        )
        assertEquals(listOf("docs/afnor/**/*.adoc"), merged.context.docsGlobs, "Empty CLI should fall back to YAML")
    }

    @Test
    fun `docsGlobs blank CLI string falls back to props`() {
        val projectDir = File(tempDir, "ctx-blank-cli").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.context.docsGlobs=docs/props/**/*.adoc
        """.trimIndent())
        val merged = CapsuleConfigMerger.merge(
            projectDir, CapsuleConfig(), mapOf("context.docsGlobs" to "  "),
            yamlLoaded = false
        )
        assertEquals(listOf("docs/props/**/*.adoc"), merged.context.docsGlobs, "Blank CLI should fall back to props")
    }

    @Test
    fun `loadFromEnvironment default context docsGlobs is empty`() {
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals(emptyList<String>(), config.context.docsGlobs, "env default context docsGlobs should be empty")
    }

    @Test
    fun `loadFromGradleProperties reads capsule context docsGlobs`() {
        val projectDir = File(tempDir, "ctx-props-load").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.context.docsGlobs=docs/afnor/**/*.adoc
        """.trimIndent())
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(listOf("docs/afnor/**/*.adoc"), config.context.docsGlobs, "loadFromGradleProperties should read docsGlobs")
    }

    // ─── capture.strategy (CAP-CR3-3 US-1) ──────────────────────────

    @Test
    fun `capture strategy defaults to PLAYWRIGHT when no source provides it`() {
        val projectDir = File(tempDir, "strat-default").also { it.mkdirs() }
        val merged = CapsuleConfigMerger.merge(projectDir, CapsuleConfig(), emptyMap())
        assertEquals(CaptureStrategy.PLAYWRIGHT, merged.capture.strategy, "default capture strategy should be PLAYWRIGHT")
    }

    @Test
    fun `loadFromGradleProperties reads capsule capture strategy playwright`() {
        val projectDir = File(tempDir, "strat-props-pw").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.strategy=playwright
        """.trimIndent())
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(CaptureStrategy.PLAYWRIGHT, config.capture.strategy, "props should set strategy=PLAYWRIGHT")
    }

    @Test
    fun `loadFromGradleProperties reads capsule capture strategy screenshot`() {
        val projectDir = File(tempDir, "strat-props-ss").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.strategy=screenshot
        """.trimIndent())
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(CaptureStrategy.SCREENSHOT, config.capture.strategy, "props should set strategy=SCREENSHOT")
    }

    @Test
    fun `loadFromGradleProperties invalid capture strategy falls back to PLAYWRIGHT`() {
        val projectDir = File(tempDir, "strat-props-invalid").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.strategy=manim
        """.trimIndent())
        val config = CapsuleConfigMerger.loadFromGradleProperties(projectDir)
        assertEquals(CaptureStrategy.PLAYWRIGHT, config.capture.strategy, "invalid strategy should fall back to PLAYWRIGHT")
    }

    @Test
    fun `merge YAML capture strategy overrides gradle properties`() {
        val projectDir = File(tempDir, "strat-yaml-override").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.strategy=playwright
        """.trimIndent())
        val yamlConfig = CapsuleConfig(capture = CaptureConfig(strategy = CaptureStrategy.SCREENSHOT))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, emptyMap())
        assertEquals(CaptureStrategy.SCREENSHOT, merged.capture.strategy, "YAML should override props")
    }

    @Test
    fun `merge CLI capture strategy overrides YAML and gradle properties`() {
        val projectDir = File(tempDir, "strat-cli-override").also { it.mkdirs() }
        File(projectDir, "gradle.properties").writeText("""
            capsule.capture.strategy=playwright
        """.trimIndent())
        val yamlConfig = CapsuleConfig(capture = CaptureConfig(strategy = CaptureStrategy.PLAYWRIGHT))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, mapOf("capture.strategy" to "screenshot"))
        assertEquals(CaptureStrategy.SCREENSHOT, merged.capture.strategy, "CLI should override YAML and props")
    }

    @Test
    fun `merge CLI blank capture strategy falls back to YAML`() {
        val projectDir = File(tempDir, "strat-cli-blank").also { it.mkdirs() }
        val yamlConfig = CapsuleConfig(capture = CaptureConfig(strategy = CaptureStrategy.SCREENSHOT))
        val merged = CapsuleConfigMerger.merge(projectDir, yamlConfig, mapOf("capture.strategy" to "  "))
        assertEquals(CaptureStrategy.SCREENSHOT, merged.capture.strategy, "blank CLI should fall back to YAML")
    }

    @Test
    fun `loadFromEnvironment default capture strategy is PLAYWRIGHT`() {
        val config = CapsuleConfigMerger.loadFromEnvironment()
        assertEquals(CaptureStrategy.PLAYWRIGHT, config.capture.strategy, "env default capture strategy should be PLAYWRIGHT")
    }
}