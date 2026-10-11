package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_video_integrity.feature` (PRT-04).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestVideoIntegrity` Gradle task filters on this runner
 * class (pattern S-082 `CapsuleDurationValidationCucumberRunner`).
 *
 * Validates the decode-validity gate wired into `validateCapsuleVideoDuration`:
 * a broken container must fail the build, a disabled flag must be a no-op, and
 * a missing video must skip gracefully.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_video_integrity.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-video-integrity.html, json:build/reports/cucumber-video-integrity.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@validation and @integrity and not @wip and not @integration"
)
class CapsuleVideoIntegrityCucumberRunner
