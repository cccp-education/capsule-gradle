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
 * CI shard — AI / resilience / viral marketing.
 *
 * Exhaustive, disjoint feature shard (see `CucumberShardKernelRunner`).
 * `capsule_viral_render.feature` is scoped here but its scenarios are tagged
 * `@viral-render` (excluded by the shard tag filter) — the real render runs in the
 * dedicated `integration` CI job. Guarded by `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_ai.feature")
@SelectClasspathResource("features/capsule_fish_audio.feature")
@SelectClasspathResource("features/capsule_llm_resilience.feature")
@SelectClasspathResource("features/capsule_strict_mode.feature")
@SelectClasspathResource("features/capsule_viral.feature")
@SelectClasspathResource("features/capsule_storyboard_context.feature")
@SelectClasspathResource("features/capsule_viral_gate.feature")
@SelectClasspathResource("features/capsule_viral_thumbnail.feature")
@SelectClasspathResource("features/capsule_viral_karaoke.feature")
@SelectClasspathResource("features/capsule_viral_normalize.feature")
@SelectClasspathResource("features/capsule_viral_render.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/capsule_ai.feature,src/test/features/capsule_fish_audio.feature," +
        "src/test/features/capsule_llm_resilience.feature,src/test/features/capsule_strict_mode.feature," +
        "src/test/features/capsule_viral.feature,src/test/features/capsule_storyboard_context.feature," +
        "src/test/features/capsule_viral_gate.feature,src/test/features/capsule_viral_thumbnail.feature," +
        "src/test/features/capsule_viral_karaoke.feature,src/test/features/capsule_viral_normalize.feature," +
        "src/test/features/capsule_viral_render.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-aiviral.html, json:build/reports/cucumber-shard-aiviral.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration and not @viral-render")
class CucumberShardAiViralRunner
