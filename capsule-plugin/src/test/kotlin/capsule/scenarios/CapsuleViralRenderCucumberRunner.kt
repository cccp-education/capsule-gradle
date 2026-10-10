package capsule.scenarios

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Dedicated Cucumber runner for the real short-form render (CAP-SHORT US-5).
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`). Needs real `ffmpeg`/`ffprobe` (scenarios
 * pending when absent).
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/features/capsule_viral_render.feature"],
    glue = ["capsule.scenarios"],
    plugin = ["pretty", "html:build/reports/tests/cucumberTestViralRender.html"],
    monochrome = true,
)
class CapsuleViralRenderCucumberRunner
