package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for StrictModeConfig (CAP-CR3-2 US-1).
 *
 * StrictModeConfig is the configuration section that toggles strict mode.
 * Default is disabled (retrocompatible with existing behavior).
 */
class StrictModeConfigTest {

    @Test
    fun `default StrictModeConfig has strict disabled`() {
        val config = StrictModeConfig()
        assertFalse(config.enabled)
    }

    @Test
    fun `StrictModeConfig can be enabled`() {
        val config = StrictModeConfig(enabled = true)
        assertTrue(config.enabled)
    }

    @Test
    fun `StrictModeConfig equality holds for same enabled value`() {
        assertEquals(StrictModeConfig(true), StrictModeConfig(true))
        assertEquals(StrictModeConfig(false), StrictModeConfig())
    }
}