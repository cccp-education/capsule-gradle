package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_distrib.feature` (CAP-ARCH-7 US-4).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestDistrib` Gradle task filters on this runner class
 * (pattern `CapsuleContextCucumberRunner` — contourne bug S-082).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_distrib.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-distrib.html, json:build/reports/cucumber-distrib.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@distrib and not @wip and not @integration"
)
class CapsuleDistribCucumberRunner