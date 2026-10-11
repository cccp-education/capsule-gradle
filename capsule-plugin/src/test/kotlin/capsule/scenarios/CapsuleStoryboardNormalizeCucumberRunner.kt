package capsule.scenarios

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Dedicated Cucumber runner for the viral storyboard normalization.
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`).
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/features/capsule_viral_normalize.feature"],
    glue = ["capsule.scenarios"],
    plugin = ["pretty", "html:build/reports/tests/cucumberTestViralNormalize.html"],
    monochrome = true,
)
class CapsuleStoryboardNormalizeCucumberRunner
