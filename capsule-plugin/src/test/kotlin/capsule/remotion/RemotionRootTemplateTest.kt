package capsule.remotion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guard for the bundled Remotion composition template (CAP-VIRAL US-7).
 *
 * The composition `Root.jsx` used to hardcode the landscape 1408×792 dimensions,
 * so the viral vertical (9:16, 1080×1920) preset resolved by
 * `capsule.viral.ViralViewport` could only be honoured by the render script
 * overriding the composition after selection. The composition must instead be
 * **props-driven** (`calculateMetadata`), so the selected composition already
 * carries the dimensions/preset the plugin planned.
 *
 * This is a resource-contract test: it reads the template shipped in the jar and
 * asserts the composition no longer pins the legacy 1408×792 literal and does
 * drive its metadata from the props. No Node, no browser.
 */
class RemotionRootTemplateTest {

    private fun rootSource(): String =
        javaClass.classLoader.getResourceAsStream("capsule/remotion/src/Root.jsx")
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("capsule/remotion/src/Root.jsx missing from the test classpath")

    @Test
    fun `root composition no longer hardcodes the legacy landscape dimensions`() {
        val source = rootSource()
        assertFalse(source.contains("1408"), "Root.jsx must not hardcode the legacy 1408 width")
        assertFalse(source.contains("792"), "Root.jsx must not hardcode the legacy 792 height")
    }

    @Test
    fun `root composition derives its metadata from the props`() {
        val source = rootSource()
        assertTrue(
            source.contains("calculateMetadata"),
            "Root.jsx must drive width/height/fps/duration from the props via calculateMetadata",
        )
    }

    @Test
    fun `root composition keeps the Capsule composition id`() {
        assertTrue(rootSource().contains("id=\"Capsule\""))
    }
}
