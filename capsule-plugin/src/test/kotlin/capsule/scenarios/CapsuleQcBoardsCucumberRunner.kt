package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_qc_boards.feature` (PRT-08).
 *
 * Pure-domain — no Gradle, no ffmpeg, no Playwright. Scoped via
 * `@SelectClasspathResource` so it never runs the full Playwright suite (S-082).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_qc_boards.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-qc-boards.html, json:build/reports/cucumber-qc-boards.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@qc-boards and not @wip and not @integration"
)
class CapsuleQcBoardsCucumberRunner
