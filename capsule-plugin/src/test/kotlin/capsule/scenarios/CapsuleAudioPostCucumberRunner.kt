package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_audio_post.feature` (CAP-AUDIO US-4).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestAudioPost` Gradle task filters on this runner
 * class (pattern S-082 `CapsuleCaptureStrategyCucumberRunner`).
 *
 * Uses a NoOp processor (no real FFmpeg — `ffmpegExecutablePath = "noop"`
 * routes through `NoOpAudioPostProcessor` which returns `false`, keeping
 * the original video). The scenarios validate the wiring +
 * economy-of-ink guard + factory dispatch in [capsule.CapsuleVideoTask.
 * applyAudioPostIfEnabled], not the audio render itself (covered by unit
 * tests for `AudioPostProcessorImpl` / `AudioPostCommand` from US-2).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_audio_post.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-audio-post.html, json:build/reports/cucumber-audio-post.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@audio and @post and not @wip and not @integration"
)
class CapsuleAudioPostCucumberRunner