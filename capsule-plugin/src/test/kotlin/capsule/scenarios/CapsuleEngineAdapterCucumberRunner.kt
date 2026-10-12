package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_engine_adapter.feature`
 * (CAP-ENGINE-DECOUPLING US-2).
 *
 * Pure-domain — no Gradle, no ffmpeg, no Playwright (fake capture). Scoped via
 * `@SelectClasspathResource` so it never runs the full suite (S-082).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_engine_adapter.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-engine-adapter.html, json:build/reports/cucumber-engine-adapter.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@engine-adapter and not @wip and not @integration"
)
class CapsuleEngineAdapterCucumberRunner
