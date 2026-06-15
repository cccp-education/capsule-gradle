package capsule

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

enum class SlideType { HTML, MANIM }

data class SlideSegment(
    val index: Int,
    val title: String,
    val speakerNote: String,
    val type: SlideType = SlideType.HTML,
    val manimScene: String? = null
)

data class CapsuleScript(
    val deckName: String,
    val slides: List<SlideSegment>
)

open class CapsuleExtension @Inject constructor(objects: ObjectFactory) {
    /** Path to the capsule-context.yml configuration file. Defaults to "capsule-context.yml" in the project root. */
    val configPath: Property<String> = objects.property(String::class.java)
        .convention("capsule-context.yml")

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
        viewportWidth = 1408,
        viewportHeight = 792,
        playwrightTimeout = 120_000.0,
        slideDurationSeconds = 5.0,
        parallelCaptureEnabled = false,
        parallelCaptureThreads = 4,
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
    val viewportWidth: Int,
    val viewportHeight: Int,
    val playwrightTimeout: Double,
    val slideDurationSeconds: Double,
    val parallelCaptureEnabled: Boolean,
    val parallelCaptureThreads: Int,
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
