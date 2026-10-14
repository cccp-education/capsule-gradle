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
 * CI shard — `capsule.feature`, **subtitles** scenarios (tag-priority partition).
 *
 * `capsule.feature` is the single heaviest feature (~400 s of TestKit full builds);
 * it is split by a **tag-priority partition** shared with [CucumberShardCapsuleManimRunner]
 * and [CucumberShardCapsuleBaseRunner] so it runs on 3 machines in parallel. The three
 * filters are mutually exclusive and exhaustive by construction:
 * 1. `@subtitles`
 * 2. `@manim and not @subtitles`
 * 3. `not @subtitles and not @manim`
 *
 * Guarded by `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule.feature")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/capsule.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-capsule-subtitles.html, json:build/reports/cucumber-shard-capsule-subtitles.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@subtitles and not @burnin and not @wip and not @integration and not @viral-render"
)
class CucumberShardCapsuleSubtitlesRunner
