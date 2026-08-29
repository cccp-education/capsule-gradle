package capsule.scenarios

import io.cucumber.junit.platform.engine.Cucumber

/**
 * CAP-PR2-COV US-4 — dedicated Cucumber runner for the PR #2 coverage regression
 * (pattern S-082: one runner per epic/scope, avoids global glue collisions).
 */
@Cucumber
class CapsulePr2CoverageCucumberRunner
