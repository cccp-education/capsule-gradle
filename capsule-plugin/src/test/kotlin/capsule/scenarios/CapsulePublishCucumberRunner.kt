package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_publish.feature`
 * (HF-YOUTUBE-PUBLISH US-3b).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestPublish` Gradle task filters on this runner class
 * (pattern S-082). The fake transport keeps the suite network-free.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_publish.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-publish.html, json:build/reports/cucumber-publish.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@publish and not @wip and not @integration"
)
class CapsulePublishCucumberRunner
