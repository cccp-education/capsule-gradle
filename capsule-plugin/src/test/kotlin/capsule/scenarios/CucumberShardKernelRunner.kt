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
 * CI shard — kernel / core model / architecture.
 *
 * One of the exhaustive, disjoint feature shards that replace the single monolithic
 * `CucumberTestRunner` invocation in CI (`full-tests.yml`), so the BDD suite runs on
 * several machines in parallel instead of one ~14 min job. Each shard keeps the
 * one-JVM efficiency of the aggregate runner (no per-feature bootstrap).
 *
 * Exhaustiveness/disjointness is guarded by `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_kernel_model.feature")
@SelectClasspathResource("features/capsule_engine_adapter.feature")
@SelectClasspathResource("features/capsule_segment_source.feature")
@SelectClasspathResource("features/capsule_arch_boundary.feature")
@SelectClasspathResource("features/consolidated_capsule_model.feature")
@SelectClasspathResource("features/capsule_pr2_coverage.feature")
@SelectClasspathResource("features/capsule_speaker_notes.feature")
@SelectClasspathResource("features/cucumber_skip.feature")
@SelectClasspathResource("features/parallel_capture_timeout.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/capsule_kernel_model.feature,src/test/features/capsule_engine_adapter.feature," +
        "src/test/features/capsule_segment_source.feature,src/test/features/capsule_arch_boundary.feature," +
        "src/test/features/consolidated_capsule_model.feature,src/test/features/capsule_pr2_coverage.feature," +
        "src/test/features/capsule_speaker_notes.feature,src/test/features/cucumber_skip.feature," +
        "src/test/features/parallel_capture_timeout.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-kernel.html, json:build/reports/cucumber-shard-kernel.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration and not @viral-render")
class CucumberShardKernelRunner
