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
 * CI shard — media / capture / format / distribution.
 *
 * Exhaustive, disjoint feature shard (see `CucumberShardKernelRunner`). Guarded by
 * `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_capture_strategy.feature")
@SelectClasspathResource("features/capsule_format.feature")
@SelectClasspathResource("features/multi_language_capsule.feature")
@SelectClasspathResource("features/capsule_audio_post.feature")
@SelectClasspathResource("features/capsule_podcast.feature")
@SelectClasspathResource("features/capsule_preview.feature")
@SelectClasspathResource("features/capsule_chapters.feature")
@SelectClasspathResource("features/capsule_remotion.feature")
@SelectClasspathResource("features/capsule_remotion_anim.feature")
@SelectClasspathResource("features/capsule_distrib.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/capsule_capture_strategy.feature,src/test/features/capsule_format.feature," +
        "src/test/features/multi_language_capsule.feature,src/test/features/capsule_audio_post.feature," +
        "src/test/features/capsule_podcast.feature,src/test/features/capsule_preview.feature," +
        "src/test/features/capsule_chapters.feature,src/test/features/capsule_remotion.feature," +
        "src/test/features/capsule_remotion_anim.feature,src/test/features/capsule_distrib.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-media.html, json:build/reports/cucumber-shard-media.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration and not @viral-render")
class CucumberShardMediaRunner
