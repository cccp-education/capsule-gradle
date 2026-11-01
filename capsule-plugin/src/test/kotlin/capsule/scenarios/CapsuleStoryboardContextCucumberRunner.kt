package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber runner for the storyboard context (CAP-CONTEXT US-2/US-3).
 *
 * Pattern S-082 — one runner per feature to avoid the shared-glue collision
 * (bug S-088 `capsule.scenarios`).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_storyboard_context.feature")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/capsule_storyboard_context.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/tests/cucumberTestStoryboardContext.html",
)
class CapsuleStoryboardContextCucumberRunner
