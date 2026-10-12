package capsule.adapter.revealjs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * TDD — reveal.js deck HTML adapter (CAP-ENGINE-DECOUPLING US-2).
 */
class RevealJsDeckHtmlTest {

    private val deck = """
        <!DOCTYPE html><html><head><style>.reveal{}</style></head><body>
        <div class="reveal"><div class="slides">
        <section data-capsule-slide="1">
          <h1>One</h1>
        </section>
        <section data-capsule-slide="2">
          <h1>Two</h1>
        </section>
        <section data-capsule-slide="3">
          <h1>Three</h1>
        </section>
        </div></div>
        </body></html>
    """.trimIndent()

    @Test
    fun `singleSlideHtml extracts the requested section as a standalone document`() {
        val html = RevealJsDeckHtml.singleSlideHtml(deck, 1)

        assertThat(html).contains("<h1>Two</h1>")
        assertThat(html).doesNotContain("<h1>One</h1>")
        assertThat(html).doesNotContain("<h1>Three</h1>")
        assertThat(html).contains(".reveal{")
        assertThat(html).contains(RevealJsDeckHtml.SINGLE_SLIDE_VISIBILITY_CSS)
    }

    @Test
    fun `singleSlideHtml returns the deck unchanged for an out-of-range index`() {
        assertThat(RevealJsDeckHtml.singleSlideHtml(deck, 99)).isEqualTo(deck)
    }

    @Test
    fun `hasDataCapsuleSlide detects the marker`() {
        assertThat(RevealJsDeckHtml.hasDataCapsuleSlide(deck)).isTrue()
        assertThat(RevealJsDeckHtml.hasDataCapsuleSlide("<html><body></body></html>")).isFalse()
    }

    @Test
    fun `injectDataSlideAudio attaches the audio path to the matching section`() {
        val html = RevealJsDeckHtml.injectDataSlideAudio(
            deck,
            mapOf(1 to "/audio/slide-01.mp3", 2 to "/audio/slide-02.mp3"),
        )

        assertThat(html).contains("""<section data-audio="file:///audio/slide-01.mp3" data-capsule-slide="1">""")
        assertThat(html).contains("""<section data-audio="file:///audio/slide-02.mp3" data-capsule-slide="2">""")
        assertThat(html).contains("""<section data-capsule-slide="3">""")
    }

    @Test
    fun `injectAudioScript appends the autoplay script before the body close`() {
        val html = RevealJsDeckHtml.injectAudioScript(deck)

        assertThat(html).contains("CAPSULE-GRADLE: Autoplay audio injection")
        assertThat(html.indexOf("Autoplay audio injection")).isLessThan(html.indexOf("</body>"))
    }

    @Test
    fun `injectAudioScript marks the sequential fallback`() {
        val html = RevealJsDeckHtml.injectAudioScript(deck, markerSuffix = "sequential fallback")

        assertThat(html).contains("Autoplay audio injection (sequential fallback)")
    }

    @Test
    fun `injectSequentialAudio attaches audio positionally to top-level sections`() {
        val html = RevealJsDeckHtml.injectSequentialAudio(
            deck,
            listOf("/audio/slide-01.mp3", "/audio/slide-02.mp3"),
        )

        assertThat(html).contains("""<section data-audio="file:///audio/slide-01.mp3" data-capsule-slide="1">""")
        assertThat(html).contains("""<section data-audio="file:///audio/slide-02.mp3" data-capsule-slide="2">""")
        assertThat(html).contains("""<section data-capsule-slide="3">""")
    }
}
