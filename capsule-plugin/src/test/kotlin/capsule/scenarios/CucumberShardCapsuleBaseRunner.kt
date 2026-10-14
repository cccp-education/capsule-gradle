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
 * CI shard — `capsule.feature`, **base** scenarios (tag-priority partition, step 3).
 *
 * See [CucumberShardCapsuleSubtitlesRunner] for the exhaustive/disjoint rationale.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule.feature")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/capsule.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-capsule-base.html, json:build/reports/cucumber-shard-capsule-base.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "not @subtitles and not @manim and not @capsule-core-a and not @capsule-core-b " +
        "and not @wip and not @integration and not @viral-render"
)
class CucumberShardCapsuleBaseRunner
