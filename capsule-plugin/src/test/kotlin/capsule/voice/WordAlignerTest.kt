package capsule.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [WordAligner] (CAP-QC-VOICE US-1). Pure — no I/O.
 */
class WordAlignerTest {

    @Test
    fun `identical lists align with zero distance`() {
        val alignment = WordAligner.align(listOf("le", "chat"), listOf("le", "chat"))
        assertEquals(0, alignment.distance)
        assertTrue(alignment.operations.all { it.kind == OpKind.MATCH })
    }

    @Test
    fun `a substitution costs one`() {
        val alignment = WordAligner.align(listOf("a", "b"), listOf("a", "c"))
        assertEquals(1, alignment.distance)
        assertEquals(1, alignment.operations.count { it.kind == OpKind.SUBSTITUTE })
    }

    @Test
    fun `an insertion costs one`() {
        val alignment = WordAligner.align(listOf("a"), listOf("a", "b"))
        assertEquals(1, alignment.distance)
        assertEquals(1, alignment.operations.count { it.kind == OpKind.INSERT })
    }

    @Test
    fun `a deletion costs one`() {
        val alignment = WordAligner.align(listOf("a", "b"), listOf("a"))
        assertEquals(1, alignment.distance)
        assertEquals(1, alignment.operations.count { it.kind == OpKind.DELETE })
    }

    @Test
    fun `empty reference against words is all insertions`() {
        val alignment = WordAligner.align(emptyList(), listOf("x", "y"))
        assertEquals(2, alignment.distance)
        assertTrue(alignment.operations.all { it.kind == OpKind.INSERT })
    }
}
