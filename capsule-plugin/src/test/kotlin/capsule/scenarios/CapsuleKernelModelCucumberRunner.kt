package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_kernel_model.feature`
 * (CAP-ENGINE-DECOUPLING US-1).
 *
 * Pure-domain — no Gradle, no ffmpeg, no Playwright. Scoped via
 * `@SelectClasspathResource` so it never runs the full suite (S-082).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_kernel_model.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-kernel-model.html, json:build/reports/cucumber-kernel-model.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@kernel-model and not @wip and not @integration"
)
class CapsuleKernelModelCucumberRunner
