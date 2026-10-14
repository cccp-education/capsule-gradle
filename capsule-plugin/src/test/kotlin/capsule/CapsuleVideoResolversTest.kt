package capsule

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit guard for [CapsuleVideoResolvers] (CAP-DECOMPOSE BS-5, S-199).
 *
 * The resolvers were extracted verbatim from `CapsuleVideoTask` (god-object
 * decomposition). These tests pin the resolution rules the task relied on — the
 * safe-degradation fallbacks (NoOp engines/services, landscape vs portrait
 * viewport, caption generator dispatch, Manim output enumeration) — so the move
 * is proven behaviour-preserving without a real binary or browser.
 */
class CapsuleVideoResolversTest {

    @TempDir
    lateinit var tempDir: File

    private val project: Project by lazy { ProjectBuilder.builder().withProjectDir(tempDir).build() }
    private val ext: CapsuleExtension by lazy { CapsuleExtension(project.objects) }

    @Test
    fun `resolveTtsEngine degrades to NoOp when the configured engine is noop`() {
        ext.ttsEngine.set("noop")
        ext.ttsLanguage.set("fr")

        assertIs<NoOpTtsEngine>(CapsuleVideoResolvers.resolveTtsEngine(project, ext))
    }

    @Test
    fun `resolveViewport keeps the landscape default when viral is disabled`() {
        ext.viralEnabled.set(false)
        ext.viewportWidth.set(1408)
        ext.viewportHeight.set(792)

        assertEquals(1408 to 792, CapsuleVideoResolvers.resolveViewport(ext))
    }

    @Test
    fun `resolveViewport switches to the portrait 9-16 viewport when viral is enabled`() {
        ext.viralEnabled.set(true)
        ext.viewportWidth.set(1408)
        ext.viewportHeight.set(792)

        assertEquals(1080 to 1920, CapsuleVideoResolvers.resolveViewport(ext))
    }

    @Test
    fun `resolveSubtitleGenerator dispatches on the subtitle format`() {
        assertIs<VttGenerator>(CapsuleVideoResolvers.resolveSubtitleGenerator(SubtitleFormat.VTT))
        assertIs<SrtGenerator>(CapsuleVideoResolvers.resolveSubtitleGenerator(SubtitleFormat.SRT))
    }

    @Test
    fun `resolveManimEngine degrades to NoOp when the executable path is noop`() {
        ext.manimExecutablePath.set("noop")

        assertIs<NoOpManimEngine>(CapsuleVideoResolvers.resolveManimEngine(ext, project.logger))
    }

    @Test
    fun `resolveManimParallelRenderer is sequential at parallelism 1 and parallel above`() {
        ext.manimParallelRender.set(false)
        assertIs<NoOpManimParallelRenderer>(CapsuleVideoResolvers.resolveManimParallelRenderer(ext, project.logger))

        ext.manimParallelRender.set(true)
        ext.manimParallelRenderThreads.set(4)
        assertIs<ManimParallelRendererImpl>(CapsuleVideoResolvers.resolveManimParallelRenderer(ext, project.logger))
    }

    @Test
    fun `manimVideoDirs returns an empty list when no Manim output root exists`() {
        ext.manimOutputDir.set("build/nonexistent-manim-dir")

        assertTrue(CapsuleVideoResolvers.manimVideoDirs(project, ext).isEmpty())
    }
}
