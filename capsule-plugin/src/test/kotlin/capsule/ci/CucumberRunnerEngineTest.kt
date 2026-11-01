package capsule.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Cucumber runner engine guard (WARN-15).
 *
 * All dedicated runners must use the **JUnit Platform suite engine**
 * (`Suite` + `IncludeEngines("cucumber")`), or the platform marker annotation
 * `io.cucumber.junit.platform.engine.Cucumber`. The legacy JUnit 4 runner
 * (`RunWith(Cucumber::class)` + `CucumberOptions`) is deprecated in Cucumber
 * 7.34 and produced the 31 remaining compiler warnings of `MEM-WARN-FREE`.
 *
 * Pure file parsing (no Gradle model), same shape as
 * [CapsuleCucumberShardCoverageTest].
 */
class CucumberRunnerEngineTest {
    private val repoRoot: File =
        run {
            var dir = File(System.getProperty("user.dir")).absoluteFile
            while (dir.parentFile != null && !dir.resolve(".github/workflows/full-tests.yml").isFile) {
                dir = dir.parentFile
            }
            check(dir.resolve(".github/workflows/full-tests.yml").isFile) {
                "full-tests.yml not found above ${System.getProperty("user.dir")}"
            }
            dir
        }

    private val testSources = repoRoot.resolve("capsule-plugin/src/test/kotlin")

    private fun sourcesWith(pattern: Regex): List<String> =
        testSources
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            // This guard names the forbidden patterns itself: exclude it from the scan.
            .filterNot { it.name == "CucumberRunnerEngineTest.kt" }
            .filter { pattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(repoRoot).path }
            .sorted()
            .toList()

    @Test
    fun `no runner uses the deprecated junit4 cucumber api`() {
        assertThat(sourcesWith(Regex("""io\.cucumber\.junit\.Cucumber\b"""))).isEmpty()
        assertThat(sourcesWith(Regex("""@CucumberOptions"""))).isEmpty()
        assertThat(sourcesWith(Regex("""@RunWith\("""))).isEmpty()
    }

    private fun usesPlatformEngine(text: String): Boolean =
        text.contains("""@IncludeEngines("cucumber")""") ||
            text.contains("io.cucumber.junit.platform.engine.Cucumber")

    @Test
    fun `every cucumber runner declares the junit platform engine`() {
        val runners =
            repoRoot
                .resolve("capsule-plugin/src/test/kotlin/capsule/scenarios")
                .walkTopDown()
                .filter { it.isFile && it.name.endsWith("CucumberRunner.kt") }
                .toList()

        assertThat(runners).isNotEmpty()
        val offenders =
            runners
                .filterNot { usesPlatformEngine(it.readText()) }
                .map { it.name }
                .sorted()
        assertThat(offenders).isEmpty()
    }
}
