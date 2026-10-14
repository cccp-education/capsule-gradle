package capsule.scenarios

import capsule.feed.SlideSegment
import capsule.feed.SlideType
import capsule.feed.toVideoKind
import capsule.feed.type
import video.kernel.FileVisualSource
import video.kernel.segment.VideoKind
import video.kernel.VideoProductionEntry
import video.kernel.segment.VideoSegment
import capsule.multilang.deckFile
import contracts.i18n.LanguageCatalog
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

/**
 * Pure-domain BDD steps for `capsule_kernel_model.feature`
 * (CAP-ENGINE-DECOUPLING US-1).
 *
 * Exercises [VideoSegment]/[VideoProductionEntry] and the legacy↔neutral bridge
 * directly. Step texts are prefixed with "kernel"/"legacy" (S-088 — the glue is
 * classpath-wide).
 */
class CapsuleKernelModelSteps {

    private var segment: VideoSegment? = null
    private var legacyType: SlideType? = null
    private var entry: VideoProductionEntry? = null

    @Given("a kernel segment titled {string} narrated {string}")
    fun aKernelSegmentTitledNarrated(title: String, narration: String) {
        segment = VideoSegment(index = 1, title = title, narration = narration)
    }

    @Given("a kernel animation segment titled {string} with scene {string}")
    fun aKernelAnimationSegmentTitledWithScene(title: String, scene: String) {
        segment = VideoSegment(
            index = 1,
            title = title,
            narration = "Body.",
            kind = VideoKind.ANIMATION,
            scene = scene,
        )
    }

    @Then("the kernel segment kind is {string}")
    fun theKernelSegmentKindIs(expected: String) {
        assertEquals(expected, segment?.kind?.name)
    }

    @Then("the kernel segment carries no scene")
    fun theKernelSegmentCarriesNoScene() {
        assertNull(segment?.scene)
    }

    @Then("the kernel segment scene is {string}")
    fun theKernelSegmentSceneIs(expected: String) {
        assertEquals(expected, segment?.scene)
    }

    @Given("a legacy slide type {string}")
    fun aLegacySlideType(raw: String) {
        legacyType = SlideType.valueOf(raw)
    }

    @Then("its neutral video kind is {string}")
    fun itsNeutralVideoKindIs(expected: String) {
        assertEquals(expected, legacyType?.toVideoKind()?.name)
    }

    @Then("the legacy slide type {string} maps to the neutral kind {string}")
    fun theLegacySlideTypeMapsTo(raw: String, expected: String) {
        assertEquals(expected, SlideType.valueOf(raw).toVideoKind().name)
    }

    @Given("a legacy slide segment aliased to the neutral kind {string}")
    fun aLegacySlideSegmentAliasedTo(rawKind: String) {
        segment = SlideSegment(
            index = 1,
            title = "Diagram",
            narration = "Body.",
            kind = VideoKind.valueOf(rawKind),
        )
    }

    @Then("its legacy accessor type is {string}")
    fun itsLegacyAccessorTypeIs(expected: String) {
        @Suppress("DEPRECATION")
        assertEquals(expected, segment?.type?.name)
    }

    @Given("a kernel production entry backed by the visual file {string}")
    fun aKernelProductionEntryBackedByTheVisualFile(fileName: String) {
        val dir = Files.createTempDirectory("capsule-kernel-model").toFile()
        val visual = File(dir, fileName).apply { parentFile?.mkdirs(); writeText("dummy") }
        val script = File(dir, "script.txt").apply { writeText("dummy") }
        entry = VideoProductionEntry(
            visual = FileVisualSource(visual),
            scriptFile = script,
            language = LanguageCatalog.findByCode("en")!!,
            outputVideo = File(dir, "out.webm"),
        )
    }

    @Then("its legacy deck file name is {string}")
    fun itsLegacyDeckFileNameIs(expected: String) {
        @Suppress("DEPRECATION")
        assertEquals(expected, entry?.deckFile?.name)
    }
}
