package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_duration_validation.feature`
 * (CAP-CR3-1 US-3).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full
 * Playwright suite — the `cucumberTestDurationValidation` Gradle task
 * filters on this runner class (pattern S-082
 * `CapsuleFormatCucumberRunner`).
 *
 * Uses NoOp probe (no real ffprobe — the task probes fake files that
 * return 0.0 duration, so the validator sees delta=0.0 which is within
 * any tolerance).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_duration_validation.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-duration-validation.html, json:build/reports/cucumber-duration-validation.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@validation and @duration and not @wip and not @integration"
)
class CapsuleDurationValidationCucumberRunner