package capsule.scenarios

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then

/**
 * CAP-PR2-COV US-4 — regression steps mirroring the static [capsule.ci.ProcessMigrationGuardTest]
 * guard inside the Cucumber pipeline, so a CI cucumber run flags any new raw
 * ProcessBuilder / waitFor / renameTo leak outside the PR #2 sinks.
 *
 * Steps are prefixed "pr2 coverage" to avoid glue collisions (S-088).
 */
class CapsulePr2CoverageSteps {

    private lateinit var sourceDir: File
    private lateinit var offending: List<String>

    @Given("the capsule plugin production source tree")
    fun theCapsulePluginProductionSourceTree() {
        sourceDir = File("src/main/kotlin")
        assertThat(sourceDir).isDirectory()
    }

    @When("the process execution sinks are audited")
    fun theProcessExecutionSinksAreAudited() {
        offending = scanFor("ProcessBuilder(", ".waitFor(", ".renameTo(")
    }

    @When("the file relocation sinks are audited")
    fun theFileRelocationSinksAreAudited() {
        offending = scanFor(".renameTo(")
    }

    @Then("only {string} and {string} may contain process or rename APIs")
    fun onlyAllowedFilesMayContain(matchA: String, matchB: String) {
        val leaks = offending.filter { path -> !path.endsWith(matchA) && !path.endsWith(matchB) }
        assertThat(leaks)
            .describedAs("process APIs must live only in $matchA / $matchB, found: $leaks")
            .isEmpty()
    }

    @Then("no file outside {string} may call renameTo")
    fun noFileOutsideMayCallRenameTo(allowed: String) {
        val leaks = offending.filter { path -> !path.endsWith(allowed) }
        assertThat(leaks)
            .describedAs("renameTo must live only in $allowed, found: $leaks")
            .isEmpty()
    }

    private fun scanFor(vararg tokens: String): List<String> {
        val hits = mutableListOf<String>()
        sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                if (tokens.any { token -> text.contains(token) }) {
                    hits.add(file.path)
                }
            }
        return hits
    }
}
