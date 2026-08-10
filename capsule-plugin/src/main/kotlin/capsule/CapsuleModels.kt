package capsule

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

open class CapsuleExtension @Inject constructor(objects: ObjectFactory) {
    /** Path to the capsule-context.yml configuration file. Defaults to "capsule-context.yml" in the project root. */
    val configPath: Property<String> = objects.property(String::class.java)
        .convention("capsule-context.yml")

    /** Active UI language for task descriptions and log messages. Defaults to "en". */
    val language: Property<String> = objects.property(String::class.java)
        .convention("en")

    /** CAP-CR3-2 — strict mode: fail build instead of NoOp fallback when a tool is missing. Defaults to false. */
    val strictMode: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /** CAP-DOCCONTEXT-1 — glob patterns for documentary corpus feeding the Docs channel. Defaults to empty. */
    val docsGlobs: ListProperty<String> = objects.listProperty(String::class.java)
        .convention(emptyList())

    /** CAP-SPD-3 — path to a pedagogical scenario directory (metadata.json + .adoc) or direct .adoc. Defaults to empty. */
    val scenarioFile: Property<String> = objects.property(String::class.java)
        .convention("")

    val ttsEngine: Property<String> = objects.property(String::class.java)
        .convention("piper")

    val ttsVoice: Property<String> = objects.property(String::class.java)
        .convention("fr_FR-siwis-medium")

    val piperExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("piper")

    val ttsFallbackEnabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(true)

    val outputDir: Property<String> = objects.property(String::class.java)
        .convention("capsule")

    val sliderScriptDir: Property<String> = objects.property(String::class.java)
        .convention("capsule")

    val viewportWidth: Property<Int> = objects.property(Int::class.java)
        .convention(1408)

    val viewportHeight: Property<Int> = objects.property(Int::class.java)
        .convention(792)

    val playwrightTimeout: Property<Double> = objects.property(Double::class.java)
        .convention(120_000.0)

    val chromiumExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("")

    val deckSourceDir: Property<String> = objects.property(String::class.java)
        .convention("docs/asciidocRevealJs")

    val ffmpegExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("ffmpeg")

    val distribOutputWidth: Property<Int> = objects.property(Int::class.java)
        .convention(1080)

    val distribOutputHeight: Property<Int> = objects.property(Int::class.java)
        .convention(1920)

    val compositeContextOutputFile: Property<String> = objects.property(String::class.java)
        .convention("capsule/capsule-context.json")

    val slideDurationSeconds: Property<Double> = objects.property(Double::class.java)
        .convention(5.0)

    val espeakVoice: Property<String> = objects.property(String::class.java)
        .convention("fr")

    val espeakSpeed: Property<Int> = objects.property(Int::class.java)
        .convention(150)

    val ttsLanguage: Property<String> = objects.property(String::class.java)
        .convention("fr")

    val manimExecutablePath: Property<String> = objects.property(String::class.java)
        .convention("manim")

    val manimQuality: Property<String> = objects.property(String::class.java)
        .convention("l")

    val manimScriptsDir: Property<String> = objects.property(String::class.java)
        .convention("src/manim")

    val manimOutputDir: Property<String> = objects.property(String::class.java)
        .convention("build/capsule/manim")

    val manimParallelRender: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val manimParallelRenderThreads: Property<Int> = objects.property(Int::class.java)
        .convention(4)

    val parallelCaptureEnabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val parallelCaptureThreads: Property<Int> = objects.property(Int::class.java)
        .convention(4)

    val captureTimeoutMinutes: Property<Int> = objects.property(Int::class.java)
        .convention(5)

    val subtitleEnabled: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val subtitleFormat: Property<String> = objects.property(String::class.java)
        .convention("srt")

    val subtitleBurnIn: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    val subtitleBurnInFontSize: Property<Int> = objects.property(Int::class.java)
        .convention(24)

    val subtitleBurnInFontColor: Property<String> = objects.property(String::class.java)
        .convention("&H00FFFFFF")

    val subtitleBurnInOutlineColor: Property<String> = objects.property(String::class.java)
        .convention("&H00000000")

    val subtitleBurnInPosition: Property<String> = objects.property(String::class.java)
        .convention("bottom")

    internal val conventions: CapsuleConventions = CapsuleConventions(
        outputDir = "capsule",
        sliderScriptDir = "capsule",
        deckSourceDir = "docs/asciidocRevealJs",
        chromiumExecutablePath = "",
        ttsEngine = "piper",
        ttsVoice = "fr_FR-siwis-medium",
        piperExecutablePath = "piper",
        ttsFallbackEnabled = true,
        espeakVoice = "fr",
        espeakSpeed = 150,
        ttsLanguage = "fr",
        viewportWidth = 1408,
        viewportHeight = 792,
        playwrightTimeout = 120_000.0,
        slideDurationSeconds = 5.0,
        parallelCaptureEnabled = false,
        parallelCaptureThreads = 4,
        captureTimeoutMinutes = 5,
        subtitleEnabled = false,
        subtitleFormat = "srt",
        subtitleBurnIn = false,
        subtitleBurnInFontSize = 24,
        subtitleBurnInFontColor = "&H00FFFFFF",
        subtitleBurnInOutlineColor = "&H00000000",
        subtitleBurnInPosition = "bottom",
        ffmpegExecutablePath = "ffmpeg",
        distribOutputWidth = 1080,
        distribOutputHeight = 1920,
        manimExecutablePath = "manim",
        manimQuality = "l",
        manimScriptsDir = "src/manim",
        manimOutputDir = "build/capsule/manim",
        manimParallelRender = false,
        manimParallelRenderThreads = 4
    )
}

data class CapsuleConventions(
    val outputDir: String,
    val sliderScriptDir: String,
    val deckSourceDir: String,
    val chromiumExecutablePath: String,
    val ttsEngine: String,
    val ttsVoice: String,
    val piperExecutablePath: String,
    val ttsFallbackEnabled: Boolean,
    val espeakVoice: String,
    val espeakSpeed: Int,
    val ttsLanguage: String,
    val viewportWidth: Int,
    val viewportHeight: Int,
    val playwrightTimeout: Double,
    val slideDurationSeconds: Double,
    val parallelCaptureEnabled: Boolean,
    val parallelCaptureThreads: Int,
    val captureTimeoutMinutes: Int,
    val subtitleEnabled: Boolean,
    val subtitleFormat: String,
    val subtitleBurnIn: Boolean,
    val subtitleBurnInFontSize: Int,
    val subtitleBurnInFontColor: String,
    val subtitleBurnInOutlineColor: String,
    val subtitleBurnInPosition: String,
    val ffmpegExecutablePath: String,
    val distribOutputWidth: Int,
    val distribOutputHeight: Int,
    val manimExecutablePath: String,
    val manimQuality: String,
    val manimScriptsDir: String,
    val manimOutputDir: String,
    val manimParallelRender: Boolean,
    val manimParallelRenderThreads: Int
)
