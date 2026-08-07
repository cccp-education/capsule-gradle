package capsule

import capsule.ci.CucumberTestGuard
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CucumberTestGuardTest {

    @Test
    fun `shouldRun returns true when runCucumber property is present`() {
        val guard = CucumberTestGuard(
            hasRunCucumberProperty = true,
            isCi = false
        )
        assertTrue(guard.shouldRun())
    }

    @Test
    fun `shouldRun returns true when CI env var is true`() {
        val guard = CucumberTestGuard(
            hasRunCucumberProperty = false,
            isCi = true
        )
        assertTrue(guard.shouldRun())
    }

    @Test
    fun `shouldRun returns true when both runCucumber and CI are active`() {
        val guard = CucumberTestGuard(
            hasRunCucumberProperty = true,
            isCi = true
        )
        assertTrue(guard.shouldRun())
    }

    @Test
    fun `shouldRun returns false when neither runCucumber nor CI is active`() {
        val guard = CucumberTestGuard(
            hasRunCucumberProperty = false,
            isCi = false
        )
        assertFalse(guard.shouldRun())
    }

    @Test
    fun `shouldSkip is the negation of shouldRun`() {
        val active = CucumberTestGuard(hasRunCucumberProperty = true, isCi = false)
        assertFalse(active.shouldSkip())

        val inactive = CucumberTestGuard(hasRunCucumberProperty = false, isCi = false)
        assertTrue(inactive.shouldSkip())
    }

    @Test
    fun `skipReason explains cucumber task is skipped when neither flag is active`() {
        val guard = CucumberTestGuard(hasRunCucumberProperty = false, isCi = false)
        assertTrue(guard.skipReason().contains("cucumberTest"))
        assertTrue(guard.skipReason().contains("-PrunCucumber"))
        assertTrue(guard.skipReason().contains("CI"))
    }

    @Test
    fun `skipReason is blank when guard should run`() {
        val guard = CucumberTestGuard(hasRunCucumberProperty = true, isCi = false)
        assertTrue(guard.skipReason().isBlank())
    }
}