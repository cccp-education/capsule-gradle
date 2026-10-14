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
 * CI shard — QC / validation / voice / timeline.
 *
 * Exhaustive, disjoint feature shard (see `CucumberShardKernelRunner`). Needs real
 * `ffmpeg` (video-integrity decode). Guarded by `capsule.ci.CapsuleCucumberShardCoverageTest`.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_duration_validation.feature")
@SelectClasspathResource("features/capsule_video_integrity.feature")
@SelectClasspathResource("features/capsule_subtitles_cartons.feature")
@SelectClasspathResource("features/capsule_render_signature.feature")
@SelectClasspathResource("features/capsule_timeline.feature")
@SelectClasspathResource("features/capsule_voice_quality.feature")
@SelectClasspathResource("features/capsule_voice_verification.feature")
@SelectClasspathResource("features/capsule_qc_boards.feature")
@ConfigurationParameter(
    key = FEATURES_PROPERTY_NAME,
    value = "src/test/features/capsule_duration_validation.feature,src/test/features/capsule_video_integrity.feature," +
        "src/test/features/capsule_subtitles_cartons.feature,src/test/features/capsule_render_signature.feature," +
        "src/test/features/capsule_timeline.feature,src/test/features/capsule_voice_quality.feature," +
        "src/test/features/capsule_voice_verification.feature,src/test/features/capsule_qc_boards.feature"
)
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-shard-qc.html, json:build/reports/cucumber-shard-qc.json"
)
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "not @wip and not @integration and not @viral-render")
class CucumberShardQcRunner
