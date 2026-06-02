package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD Baby Step — HtmlSectionParser unit tests.
 *
 * Extracts top-level <section> elements from reveal.js slides HTML,
 * handling nested sections (vertical stacks) by tracking depth.
 */
class HtmlSectionParserTest {

    @Test
    fun `extractTopLevelSections extracts simple flat sections`() {
        val html = """
            <section><h2>Slide 1</h2></section>
            <section><h2>Slide 2</h2></section>
            <section><h2>Slide 3</h2></section>
        """.trimIndent()

        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(3, sections.size)
        assertTrue(sections[0].contains("Slide 1"))
        assertTrue(sections[1].contains("Slide 2"))
        assertTrue(sections[2].contains("Slide 3"))
    }

    @Test
    fun `extractTopLevelSections handles nested sections`() {
        val html = """
            <section>
                <section><h2>Sub A</h2></section>
                <section><h2>Sub B</h2></section>
            </section>
            <section><h2>Slide 2</h2></section>
        """.trimIndent()

        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(2, sections.size)
        assertTrue(sections[0].contains("Sub A"))
        assertTrue(sections[0].contains("Sub B"))
        assertTrue(sections[1].contains("Slide 2"))
    }

    @Test
    fun `extractTopLevelSections returns empty list for no sections`() {
        val html = "<div><h2>No sections here</h2></div>"
        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(0, sections.size)
    }

    @Test
    fun `extractTopLevelSections handles single section`() {
        val html = "<section><h2>Only</h2></section>"
        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(1, sections.size)
        assertTrue(sections[0].contains("Only"))
    }

    @Test
    fun `extractTopLevelSections skips self-closing tags`() {
        val html = """
            <section />
            <section><h2>Real</h2></section>
        """.trimIndent()

        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(1, sections.size)
        assertTrue(sections[0].contains("Real"))
    }

    @Test
    fun `extractTopLevelSections handles sections with attributes`() {
        val html = """
            <section data-capsule-slide="1" class="intro"><h2>One</h2></section>
            <section id="two" data-background="red"><h2>Two</h2></section>
        """.trimIndent()

        val sections = HtmlSectionParser.extractTopLevelSections(html)
        assertEquals(2, sections.size)
        assertTrue(sections[0].contains("data-capsule-slide=\"1\""))
        assertTrue(sections[1].contains("id=\"two\""))
    }
}
