package capsule.remotion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guard for the bundled Capsule composition template (CAP-CONTEXT US-4).
 *
 * The composition used to hardcode its background (`PAGE_BACKGROUND`), so a
 * brand could not dress the capsule: the deck's design tokens (CAP-CONTEXT,
 * injected by `ViralDeckBuilder` as `--capsule-background` / brand tokens like
 * `--accent-color`) were ignored. The composition must instead read those tokens
 * and keep the historical values only as the neutral fallback (no brand book →
 * unchanged look).
 *
 * Resource-contract test: reads the template shipped in the jar. No Node, no
 * browser — mirror of [RemotionRootTemplateTest].
 */
class CapsuleThemeTemplateTest {

    private fun capsuleSource(): String =
        javaClass.classLoader.getResourceAsStream("capsule/remotion/src/Capsule.jsx")
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("capsule/remotion/src/Capsule.jsx missing from the test classpath")

    @Test
    fun `capsule composition reads the brand background token`() {
        assertTrue(
            capsuleSource().contains("var(--capsule-background"),
            "Capsule.jsx must read the deck's --capsule-background token",
        )
    }

    @Test
    fun `capsule composition reads the brand accent token`() {
        assertTrue(
            capsuleSource().contains("var(--accent-color"),
            "Capsule.jsx must read the brand --accent-color token",
        )
    }

    @Test
    fun `capsule composition keeps the historical look as the fallback only`() {
        val source = capsuleSource()
        assertFalse(
            source.contains("PAGE_BACKGROUND"),
            "the hardcoded PAGE_BACKGROUND must be replaced by the token read",
        )
        assertTrue(
            source.contains("radial-gradient(120% 90%"),
            "the neutral fallback gradient must remain for the context-less case",
        )
    }
}
