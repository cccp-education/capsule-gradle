package capsule

import capsule.multilang.CapsuleVideoOutputRenamer
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * CAP-PR2-COV US-3 — [capsule.FileReplace.moveOver] is the single chokepoint for
 * any file relocation in the plugin (PR #2 anti-loss-of-data seam), and the
 * multi-language pipeline renames the rendered WebM through it.
 *
 * These tests prove the delegation seam works end-to-end: with an interceptor
 * installed, [capsule.FileReplace.moveOver] stops doing real I/O and routes
 * through the injected lambda (so tests can fake failures and assert args),
 * and [CapsuleVideoOutputRenamer] forwards to it without bypassing the seam.
 */
class FileReplaceDelegationTest {

    @AfterEach
    fun tearDown() {
        FileReplace.moveOverInterceptor = null
    }

    private fun temp(prefix: String): File =
        File(System.getProperty("java.io.tmpdir"), "${prefix}-${System.nanoTime()}").also { it.mkdirs() }

    private fun file(dir: File, name: String, content: String = "x"): File =
        File(dir, name).also { it.writeText(content) }

    @Test
    fun `moveOver delegates to interceptor and skips real move`() {
        var invoked: Pair<File, File>? = null
        FileReplace.moveOverInterceptor = { src, tgt ->
            invoked = src to tgt
            true
        }
        val dir = temp("fr-move")
        val src = file(dir, "src.webm")
        val tgt = File(dir, "tgt.webm")
        val result = FileReplace.moveOver(src, tgt)
        assertThat(result).isTrue()
        assertThat(invoked).isEqualTo(src to tgt)
        // No real filesystem move happened: source kept, target untouched.
        assertThat(src.exists()).isTrue()
        assertThat(tgt.exists()).isFalse()
    }

    @Test
    fun `moveOver honours a false interceptor result`() {
        FileReplace.moveOverInterceptor = { _, _ -> false }
        val dir = temp("fr-move-false")
        val src = file(dir, "src.webm")
        val tgt = File(dir, "tgt.webm")
        assertThat(FileReplace.moveOver(src, tgt)).isFalse()
        assertThat(src.exists()).isTrue()
        assertThat(tgt.exists()).isFalse()
    }

    @Test
    fun `CapsuleVideoOutputRenamer delegates to FileReplace moveOver`() {
        var invoked: Pair<File, File>? = null
        FileReplace.moveOverInterceptor = { src, tgt ->
            invoked = src to tgt
            true
        }
        val dir = temp("renamer")
        val rendered = file(dir, "capsule-feed-demo-fr-deck.webm")
        val target = File(dir, "capsule-feed-demo_fr.webm")
        val result = CapsuleVideoOutputRenamer.rename(rendered, target)
        assertThat(invoked).isEqualTo(rendered to target)
        assertThat(result.absolutePath).isEqualTo(target.absolutePath)
    }

    @Test
    fun `CapsuleVideoOutputRenamer skips the seam when source already matches target`() {
        var calls = 0
        FileReplace.moveOverInterceptor = { _, _ -> calls++; true }
        val dir = temp("renamer-skip")
        val same = file(dir, "capsule-feed-demo_fr.webm")
        val result = CapsuleVideoOutputRenamer.rename(same, same)
        assertThat(calls).isEqualTo(0)
        assertThat(result.absolutePath).isEqualTo(same.absolutePath)
    }
}
