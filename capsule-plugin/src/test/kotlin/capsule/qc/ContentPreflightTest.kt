package capsule.qc

import capsule.feed.SlideSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for [ContentPreflight] (PRT-05). Pure — no I/O.
 */
class ContentPreflightTest {

    private fun slide(index: Int, note: String) = SlideSegment(index = index, title = "s$index", narration = note)

    @Test
    fun `clean slides yield no finding`() {
        assertTrue(ContentPreflight.check(listOf(slide(1, "hello"), slide(2, "world"))).isEmpty())
    }

    @Test
    fun `a long note warns`() {
        val findings = ContentPreflight.check(listOf(slide(1, "x".repeat(600))))
        assertEquals(1, findings.size)
        assertEquals("long-note", findings.single().code)
        assertEquals(Severity.WARNING, findings.single().severity)
    }

    @Test
    fun `a duplicate note warns on the later slide`() {
        val findings = ContentPreflight.check(listOf(slide(1, "same"), slide(2, "same")))
        assertEquals(1, findings.size)
        assertEquals("duplicate-note", findings.single().code)
        assertEquals(2, findings.single().slideIndex)
    }

    @Test
    fun `duplicate detection is case and whitespace insensitive`() {
        val findings = ContentPreflight.check(listOf(slide(1, "Bonjour"), slide(2, "  bonjour ")))
        assertEquals(1, findings.size)
        assertEquals("duplicate-note", findings.single().code)
    }

    @Test
    fun `markdown emphasis is a forbidden form error`() {
        val findings = ContentPreflight.check(listOf(slide(1, "des **classes**")))
        assertEquals(1, findings.size)
        assertEquals("forbidden-form", findings.single().code)
        assertEquals(Severity.ERROR, findings.single().severity)
    }

    @Test
    fun `custom forbidden rules apply`() {
        val rules = PreflightRules(forbidden = listOf(ForbiddenForm(Regex("""\d\.\d"""), "decimal point")))
        val findings = ContentPreflight.check(listOf(slide(1, "value 1.5")), rules)
        assertEquals("forbidden-form", findings.single().code)
        assertTrue(findings.single().message.contains("decimal point"))
    }

    @Test
    fun `empty forbidden list disables that check`() {
        val rules = PreflightRules(forbidden = emptyList())
        assertTrue(ContentPreflight.check(listOf(slide(1, "des **classes**")), rules).isEmpty())
    }

    @Test
    fun `max note chars is configurable`() {
        val findings = ContentPreflight.check(listOf(slide(1, "x".repeat(20))), PreflightRules(maxNoteChars = 10))
        assertEquals("long-note", findings.single().code)
    }
}
