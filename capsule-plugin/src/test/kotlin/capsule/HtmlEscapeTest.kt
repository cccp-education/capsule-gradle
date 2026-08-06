package capsule

import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlEscapeTest {

    @Test
    fun `escapeHtml encodes ampersand`() {
        assertEquals("a&amp;b", HtmlEscape.escape("a&b"))
    }

    @Test
    fun `escapeHtml encodes double quote`() {
        assertEquals("a&quot;b", HtmlEscape.escape("a\"b"))
    }

    @Test
    fun `escapeHtml encodes less than`() {
        assertEquals("a&lt;b", HtmlEscape.escape("a<b"))
    }

    @Test
    fun `escapeHtml encodes greater than`() {
        assertEquals("a&gt;b", HtmlEscape.escape("a>b"))
    }

    @Test
    fun `escapeHtml neutralizes XSS payload in attribute context`() {
        val malicious = "fr\"><script>alert('XSS')</script>"
        val escaped = HtmlEscape.escape(malicious)
        assert(!escaped.contains("<script>")) { "XSS payload must be neutralized: $escaped" }
    }

    @Test
    fun `escapeHtml leaves plain ascii unchanged`() {
        assertEquals("plain-text-123", HtmlEscape.escape("plain-text-123"))
    }
}