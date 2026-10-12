package capsule.kernel

import contracts.i18n.LanguageCatalog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * TDD — engine-neutral [VideoProductionPlan] (CAP-ENGINE-DECOUPLING US-1).
 */
class VideoProductionPlanTest {

    @TempDir
    lateinit var tempDir: Path

    private fun entry(code: String): VideoProductionEntry {
        val visual = tempDir.resolve("visuals/demo_$code-deck.html").toFile().apply {
            parentFile?.mkdirs()
            writeText("dummy")
        }
        val script = tempDir.resolve("scripts/demo_$code-script.txt").toFile().apply {
            parentFile?.mkdirs()
            writeText("dummy")
        }
        return VideoProductionEntry(
            visual = FileVisualSource(visual),
            scriptFile = script,
            language = LanguageCatalog.findByCode(code)!!,
            outputVideo = tempDir.resolve("out/demo_$code.webm").toFile(),
        )
    }

    @Test
    fun `size counts entries and languages preserves order`() {
        val plan = VideoProductionPlan(listOf(entry("en"), entry("fr")))

        assertThat(plan.size()).isEqualTo(2)
        assertThat(plan.languages().map { it.code }).containsExactly("en", "fr")
        assertThat(plan.isEmpty()).isFalse()
    }

    @Test
    fun `rejects an empty plan`() {
        assertThrows<IllegalArgumentException> {
            VideoProductionPlan(emptyList())
        }
    }
}
