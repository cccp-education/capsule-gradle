package capsule.scenarios

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Dedicated Cucumber runner for the storyboard context (CAP-CONTEXT US-2/US-3).
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`).
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/features/capsule_storyboard_context.feature"],
    glue = ["capsule.scenarios"],
    plugin = ["pretty", "html:build/reports/tests/cucumberTestStoryboardContext.html"],
    monochrome = true,
)
class CapsuleStoryboardContextCucumberRunner
