package capsule.multilang

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CapsuleVideoOutputRenamerTest {

    @TempDir
    lateinit var tmp: File

    @Test
    fun `renames rendered webm to the plan target when names differ`() {
        val rendered = tmp.resolve("capsule-feed-demo-fr-deck.webm").apply { writeText("webm-data") }
        val target = tmp.resolve("capsule-feed-demo_fr.webm")

        val result = CapsuleVideoOutputRenamer.rename(rendered, target)

        assertEquals(target, result)
        assertTrue(target.exists())
        assertFalse(rendered.exists())
        assertEquals("webm-data", target.readText())
    }

    @Test
    fun `returns rendered as-is when the target path is the same`() {
        val rendered = tmp.resolve("demo_fr.webm").apply { writeText("x") }

        val result = CapsuleVideoOutputRenamer.rename(rendered, rendered)

        assertEquals(rendered, result)
        assertTrue(rendered.exists())
    }

    @Test
    fun `returns rendered as-is when no video was produced`() {
        val rendered = tmp.resolve("missing.webm")
        val target = tmp.resolve("demo_fr.webm")

        val result = CapsuleVideoOutputRenamer.rename(rendered, target)

        assertEquals(rendered, result)
        assertFalse(target.exists())
    }

    @Test
    fun `creates the target parent directory before renaming`() {
        val rendered = tmp.resolve("capsule-feed-demo-fr-deck.webm").apply { writeText("data") }
        val target = tmp.resolve("nested").resolve("capsule-feed-demo_fr.webm")

        val result = CapsuleVideoOutputRenamer.rename(rendered, target)

        assertEquals(target, result)
        assertTrue(target.exists())
        assertFalse(rendered.exists())
    }
}
