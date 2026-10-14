package capsule.adapter.revealjs

import video.kernel.segment.VideoKind
import video.kernel.segment.VideoScript
import video.kernel.segment.VideoSegment
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * TDD — reveal.js `-script.txt` contract (CAP-ENGINE-DECOUPLING US-3).
 */
class RevealJsScriptContractTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `round-trips name, ordering, kinds and scenes`() {
        val script = VideoScript(
            name = "demo",
            segments = listOf(
                VideoSegment(index = 1, title = "Intro", narration = "Hello."),
                VideoSegment(
                    index = 2,
                    title = "Diagram",
                    narration = "Watch.",
                    kind = VideoKind.ANIMATION,
                    scene = "MoveSquare",
                ),
            ),
        )

        val file = tempDir.resolve("demo-script.txt").toFile().apply { writeText(RevealJsScriptContract.write(script)) }
        val read = RevealJsScriptContract.read(file)

        assertThat(read.name).isEqualTo("demo")
        assertThat(read.segments).hasSize(2)
        assertThat(read.segments[0].kind).isEqualTo(VideoKind.VISUAL)
        assertThat(read.segments[1].kind).isEqualTo(VideoKind.ANIMATION)
        assertThat(read.segments[1].scene).isEqualTo("MoveSquare")
    }

    @Test
    fun `write rejects an empty segment list`() {
        assertThatThrownBy { RevealJsScriptContract.write(VideoScript(name = "demo", segments = emptyList())) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `read falls back to the file name when the header is absent`() {
        val file = tempDir.resolve("fallback-script.txt").toFile().apply {
            writeText("--- SLIDE 1 : Intro ---\nHello.\n")
        }

        assertThat(RevealJsScriptContract.read(file).name).isEqualTo("fallback-script")
    }
}
