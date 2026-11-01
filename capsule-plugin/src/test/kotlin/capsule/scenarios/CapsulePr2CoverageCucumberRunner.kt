package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * CAP-PR2-COV US-4 — dedicated Cucumber runner for the PR #2 coverage regression
 * (pattern S-082: one runner per epic/scope, avoids global glue collisions).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_pr2_coverage.feature")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/features/capsule_pr2_coverage.feature")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@pr2-cov")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
class CapsulePr2CoverageCucumberRunner
