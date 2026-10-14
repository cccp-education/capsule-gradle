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
 * CI shard — `multi_language_capsule.feature`, heavy generation partition D.
 *
 * See [CucumberShardMultiLangGenCRunner].
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/multi_language_capsule.feature")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/multi_language_capsule.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-multilang-gend.html, json:build/reports/cucumber-shard-multilang-gend.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@multilang-gen-d and not @wip and not @integration and not @viral-render"
)
class CucumberShardMultiLangGenDRunner
