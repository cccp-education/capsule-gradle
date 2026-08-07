package capsule.scenarios

import capsule.ci.CucumberTestGuard
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Cucumber BDD — CR-10 cucumberTest skip guard.
 *
 * Exercises the pure domain decision (capsule.ci.CucumberTestGuard) end-to-end
 * through the BDD layer: the cucumberTest task must be skipped unless the user
 * opts in via -PrunCucumber or the build runs on a CI server (CI=true).
 */
class CucumberSkipSteps {

    private var guard: CucumberTestGuard? = null

    @Given("a cucumber test guard with runCucumber {string} and CI {string}")
    fun aCucumberTestGuardWith(runCucumber: String, ci: String) {
        val hasRunCucumber = runCucumber == "active"
        val isCi = ci == "active"
        guard = CucumberTestGuard(
            hasRunCucumberProperty = hasRunCucumber,
            isCi = isCi
        )
    }

    @Then("the guard should skip the cucumber test")
    fun theGuardShouldSkipTheCucumberTest() {
        val g = guard ?: error("Guard not initialised")
        assertTrue(g.shouldSkip(), "Guard should skip when neither flag is active")
        assertFalse(g.shouldRun(), "shouldRun must be false when shouldSkip is true")
    }

    @Then("the guard should run the cucumber test")
    fun theGuardShouldRunTheCucumberTest() {
        val g = guard ?: error("Guard not initialised")
        assertTrue(g.shouldRun(), "Guard should run when at least one flag is active")
        assertFalse(g.shouldSkip(), "shouldSkip must be false when shouldRun is true")
    }

    @And("the skip reason should mention {string}")
    fun theSkipReasonShouldMention(token: String) {
        val g = guard ?: error("Guard not initialised")
        val reason = g.skipReason()
        assertTrue(
            reason.contains(token),
            "Skip reason '$reason' should mention '$token'"
        )
    }

    @And("the skip reason should be blank")
    fun theSkipReasonShouldBeBlank() {
        val g = guard ?: error("Guard not initialised")
        assertTrue(g.skipReason().isBlank(), "Skip reason should be blank when guard should run")
    }
}