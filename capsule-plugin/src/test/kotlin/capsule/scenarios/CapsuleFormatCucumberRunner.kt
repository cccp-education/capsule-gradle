package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_format.feature` (CAP-MP4 US-3).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestFormat` Gradle task filters on this runner class
 * (pattern S-082 `CapsuleContextCucumberRunner`).
 *
 * Uses a NoOp converter (no real FFmpeg); scenarios validate that
 * [capsule.DistributeCapsuleVideoTask] filters the distributed files
 * according to [capsule.OutputFormat] (WEBM / MP4 / BOTH).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_format.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-format.html, json:build/reports/cucumber-format.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@distrib and @format and not @wip and not @integration"
)
class CapsuleFormatCucumberRunner