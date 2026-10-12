package capsule.scenarios

import io.cucumber.junit.Cucumber
import io.cucumber.junit.CucumberOptions
import org.junit.runner.RunWith

/**
 * Dedicated Cucumber runner for the Fish Audio engine feature (CAP-FISH-AUDIO US-4).
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`). Fish Audio is never called: the transport is a fake.
 */
@RunWith(Cucumber::class)
@CucumberOptions(
    features = ["src/test/features/capsule_fish_audio.feature"],
    glue = ["capsule.scenarios"],
    plugin = ["pretty", "html:build/reports/tests/cucumberTestFishAudio.html"],
    monochrome = true,
)
class CapsuleFishAudioCucumberRunner
