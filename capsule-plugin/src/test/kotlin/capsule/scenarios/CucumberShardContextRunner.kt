package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CI shard — context / content pipeline.
 *
 * Exhaustive, disjoint feature shard (see `CucumberShardKernelRunner`). Guarded by
 * `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_context.feature")
@SelectClasspathResource("features/capsule_doc_context.feature")
@SelectClasspathResource("features/capsule_scenario_context.feature")
@SelectClasspathResource("features/capsule_glossary_context.feature")
@SelectClasspathResource("features/capsule_provenance.feature")
@SelectClasspathResource("features/capsule_content_and_videos.feature")
@SelectClasspathResource("features/capsule_content_preflight.feature")
@SelectClasspathResource("features/capsule_transcript.feature")
@SelectClasspathResource("features/capsule.feature")
@SelectClasspathResource("features/translate_and_extract_speaker_notes.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-context.html, json:build/reports/cucumber-shard-context.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration and not @viral-render")
class CucumberShardContextRunner
