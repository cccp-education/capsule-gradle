package capsule.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Architecture boundary guard — the engine-neutral kernel
 * (CAP-ENGINE-DECOUPLING US-4).
 *
 * `capsule.kernel` is the contract shared with a future HyperFrames engine: it
 * must stay free of any renderer (reveal.js, Playwright, AsciiDoc) or build
 * system (Gradle) dependency, so that a second engine can implement the ports
 * without dragging capsule's reveal coupling along. This guard locks the source
 * set so the coupling cannot reappear silently.
 */
class KernelBoundaryTest {

    @Test
    fun `kernel only imports java, kotlin, contracts or itself`() {
        assertThat(KernelBoundaryAudit.importViolations())
            .describedAs(
                "capsule.kernel must stay engine-agnostic: no Gradle, Playwright, " +
                    "adapter or legacy feed dependency",
            )
            .isEmpty()
    }

    @Test
    fun `kernel code carries no renderer or build-system vocabulary`() {
        assertThat(KernelBoundaryAudit.vocabularyViolations())
            .describedAs(
                "the kernel speaks segment/visual/engine, never " +
                    "slide/deck/reveal/Playwright/AsciiDoc/Gradle",
            )
            .isEmpty()
    }
}
