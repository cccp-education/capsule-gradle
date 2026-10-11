package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_voice_quality.feature` (CAP-QC-VOICE US-1).
 *
 * Pure-domain — no Playwright, no ffmpeg, no ASR. Scoped via
 * `@SelectClasspathResource` so it never runs the full Playwright suite (S-082).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_voice_quality.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-voice-quality.html, json:build/reports/cucumber-voice-quality.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@voice and not @wip and not @integration"
)
class CapsuleVoiceQualityCucumberRunner
