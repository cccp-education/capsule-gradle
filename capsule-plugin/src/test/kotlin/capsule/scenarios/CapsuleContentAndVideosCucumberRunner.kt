package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_content_and_videos.feature` (CAP-ARCH-4).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestContentAndVideos` Gradle task filters on this runner
 * class (pattern codebase `EpicV6CucumberRunner`, capsule `CapsuleContextCucumberRunner`).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_content_and_videos.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-content-and-videos.html, json:build/reports/cucumber-content-and-videos.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@architecture and not @wip and not @integration"
)
class CapsuleContentAndVideosCucumberRunner