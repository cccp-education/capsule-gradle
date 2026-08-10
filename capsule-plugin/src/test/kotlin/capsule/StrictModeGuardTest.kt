package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for StrictModeGuard (CAP-CR3-2 US-1).
 *
 * StrictModeGuard is a pure object that enforces the strict-mode contract:
 * when strict mode is enabled, an engine that is not available must cause
 * a build failure with an actionable message. When strict mode is disabled,
 * the guard is a no-op (the caller falls back to NoOp as before).
 */
class StrictModeGuardTest {

    @Test
    fun `requireAvailable does nothing when strict is false`() {
        // Engine not available, but strict is off — guard is a no-op
        StrictModeGuard.requireAvailable(
            strict = false,
            engineName = "piper",
            isAvailable = false,
            path = "/usr/bin/piper"
        )
    }

    @Test
    fun `requireAvailable does nothing when strict is true and engine is available`() {
        StrictModeGuard.requireAvailable(
            strict = true,
            engineName = "piper",
            isAvailable = true,
            path = "/usr/bin/piper"
        )
    }

    @Test
    fun `requireAvailable throws IllegalStateException when strict is true and engine is not available`() {
        val ex = assertFailsWith<IllegalStateException> {
            StrictModeGuard.requireAvailable(
                strict = true,
                engineName = "piper",
                isAvailable = false,
                path = "/usr/bin/piper"
            )
        }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("piper"), "message should mention the engine name")
    }

    @Test
    fun `requireAvailable message includes the path when provided`() {
        val ex = assertFailsWith<IllegalStateException> {
            StrictModeGuard.requireAvailable(
                strict = true,
                engineName = "ffmpeg",
                isAvailable = false,
                path = "/usr/local/bin/ffmpeg"
            )
        }
        assertTrue(ex.message!!.contains("/usr/local/bin/ffmpeg"), "message should mention the path")
    }

    @Test
    fun `requireAvailable message does not include path segment when path is blank`() {
        val ex = assertFailsWith<IllegalStateException> {
            StrictModeGuard.requireAvailable(
                strict = true,
                engineName = "playwright",
                isAvailable = false,
                path = ""
            )
        }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("playwright"))
        // No dangling " at ''" artifact when path is blank
        assertTrue(!ex.message!!.contains(" at ''"), "message should not contain empty path segment")
    }

    @Test
    fun `requireAvailable message suggests disabling strictMode`() {
        val ex = assertFailsWith<IllegalStateException> {
            StrictModeGuard.requireAvailable(
                strict = true,
                engineName = "manim",
                isAvailable = false,
                path = "manim"
            )
        }
        assertTrue(
            ex.message!!.contains("strictMode"),
            "message should suggest disabling strictMode"
        )
    }

    @Test
    fun `requireAvailable message suggests installing the tool`() {
        val ex = assertFailsWith<IllegalStateException> {
            StrictModeGuard.requireAvailable(
                strict = true,
                engineName = "espeak",
                isAvailable = false,
                path = "espeak"
            )
        }
        assertTrue(
            ex.message!!.contains("install"),
            "message should suggest installing the tool"
        )
    }
}