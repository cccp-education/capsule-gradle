package capsule.scenarios

import io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME
import io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

/**
 * Dedicated Cucumber suite for `capsule_arch_boundary.feature` (CAP-ARCH-6).
 *
 * Scoped via `@SelectClasspathResource` so it never runs the full Playwright
 * suite — the `cucumberTestArchBoundary` Gradle task filters on this runner
 * class (pattern `CapsuleContextCucumberRunner`, `CapsuleContentAndVideosCucumberRunner`).
 *
 * Exercises the CAP-ARCH boundary end-to-end (single-language, French): the
 * koog content pipeline (`generateCapsuleContent`, mock LLM) feeds the CAP-29
 * video backend (`generateCapsuleVideoAllLanguages`, NoOp engines). The 10-language
 * translation coverage belongs to `multi_language_capsule.feature` (CAP-29.5).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/capsule_arch_boundary.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "capsule.scenarios")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber-arch-boundary.html, json:build/reports/cucumber-arch-boundary.json"
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "@cross-borough and not @wip and not @integration"
)
class CapsuleArchBoundaryCucumberRunner