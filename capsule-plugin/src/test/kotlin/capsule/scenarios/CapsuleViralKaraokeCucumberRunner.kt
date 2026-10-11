package capsule.scenarios

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Dedicated Cucumber runner for the viral karaoke captions (CAP-VIRAL-E2E US-3).
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`).
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/features/capsule_viral_karaoke.feature"],
    glue = ["capsule.scenarios"],
    plugin = ["pretty", "html:build/reports/tests/cucumberTestViralKaraoke.html"],
    monochrome = true,
)
class CapsuleViralKaraokeCucumberRunner
