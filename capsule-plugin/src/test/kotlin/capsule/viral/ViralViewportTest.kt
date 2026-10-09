package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [ViralViewport] — CAP-VIRAL US-2.
 *
 * The short-form viral capture uses a deterministic 9:16 portrait viewport
 * (1080×1920) instead of the pedagogical 16:9 landscape; the resolver is pure
 * and the crop `deployCapsule` remains the fallback for landscape decks.
 */
class ViralViewportTest {

    @Test
    fun `resolve returns the portrait viewport when viral is enabled`() {
        val (w, h) = ViralViewport.resolve(viralEnabled = true, defaultWidth = 1408, defaultHeight = 792)
        assertEquals(1080, w)
        assertEquals(1920, h)
    }

    @Test
    fun `resolve preserves the landscape default when viral is disabled`() {
        val (w, h) = ViralViewport.resolve(viralEnabled = false, defaultWidth = 1408, defaultHeight = 792)
        assertEquals(1408, w)
        assertEquals(792, h)
    }

    @Test
    fun `isPortrait recognizes the short-form viewport`() {
        assertTrue(ViralViewport.isPortrait(1080, 1920))
        assertFalse(ViralViewport.isPortrait(1920, 1080))
        assertFalse(ViralViewport.isPortrait(1408, 792))
    }

    @Test
    fun `portrait constants are 9 by 16`() {
        assertEquals(1080, ViralViewport.PORTRAIT_WIDTH)
        assertEquals(1920, ViralViewport.PORTRAIT_HEIGHT)
        assertEquals(16, ViralViewport.PORTRAIT_HEIGHT * 9 / ViralViewport.PORTRAIT_WIDTH)
    }
}
