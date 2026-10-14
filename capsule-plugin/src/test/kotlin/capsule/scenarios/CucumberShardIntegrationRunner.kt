package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CI shard — integration partition A: `@integration` scenarios (all features).
 *
 * The former single `cucumberTestIntegration` job (filter `@integration or @manim`,
 * ~435 s) is split by a mutually-exclusive, exhaustive tag partition shared with
 * [CucumberShardManimRunner]:
 * 1. `@integration`
 * 2. `@manim and not @integration`
 *
 * Requires real `ffmpeg`/Chromium. Guarded by `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-integration.html, json:build/reports/cucumber-shard-integration.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@integration and not @wip and not @viral-render"
)
class CucumberShardIntegrationRunner
