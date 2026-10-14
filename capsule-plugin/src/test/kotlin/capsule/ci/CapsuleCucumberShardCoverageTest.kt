package capsule.ci

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * CI shard coverage guard (CI sharding).
 *
 * The monolithic `CucumberTestRunner` (~14 min) is split in `full-tests.yml` into
 * exhaustive, disjoint feature shards, each run on its own CI machine. This guard
 * kills the "test fantôme" risk (S-269): a feature that belongs to no shard (or to
 * two) would silently stop being executed.
 *
 * It asserts, from the repository sources (no Gradle model, pure file parsing):
 * 1. the shard runners form a **disjoint** partition whose union is **every**
 *    feature file under `src/test/features/`;
 * 2. every shard runner is registered as a Gradle task **and** invoked by the CI
 *    workflow (`full-tests.yml`).
 */
class CapsuleCucumberShardCoverageTest {

    private val repoRoot: File = run {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null && !dir.resolve(".github/workflows/full-tests.yml").isFile) {
            dir = dir.parentFile
        }
        check(dir.resolve(".github/workflows/full-tests.yml").isFile) {
            "full-tests.yml not found above ${System.getProperty("user.dir")}"
        }
        dir
    }

    private val scenarioDir = repoRoot.resolve("capsule-plugin/src/test/kotlin/capsule/scenarios")
    private val featuresDir = repoRoot.resolve("capsule-plugin/src/test/features")
    private val buildScript = repoRoot.resolve("capsule-plugin/build.gradle.kts")
    private val workflow = repoRoot.resolve(".github/workflows/full-tests.yml")

    private fun allFeatures(): Set<String> =
        featuresDir.listFiles { f -> f.isFile && f.name.endsWith(".feature") }!!
            .map { it.name }
            .toSet()

    private fun shardRunners(): List<File> =
        scenarioDir.listFiles { f -> f.isFile && f.name.startsWith("CucumberShard") && f.name.endsWith("Runner.kt") }!!
            .sortedBy { it.name }

    private fun runnerFeatures(runner: File): List<String> =
        Regex("""@SelectClasspathResource\("features/([^"]+\.feature)"\)""")
            .findAll(runner.readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `the shards form an exhaustive and disjoint partition of every feature file`() {
        val runners = shardRunners()
        assertThat(runners)
            .describedAs("CI shard runners must exist and be at least 5")
            .hasSizeGreaterThanOrEqualTo(5)

        val perRunner = runners.associate { it.name to runnerFeatures(it) }
        perRunner.forEach { (name, features) ->
            assertThat(features)
                .describedAs("%s must select at least one feature", name)
                .isNotEmpty()
        }

        val all = perRunner.values.flatten()
        val duplicates = all.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertThat(duplicates)
            .describedAs("a feature must belong to exactly one shard")
            .isEmpty()

        assertThat(all.toSet())
            .describedAs("the shards must cover every feature file")
            .containsExactlyInAnyOrderElementsOf(allFeatures())
    }

    @Test
    fun `every shard runner is a Gradle task and is run by the CI workflow`() {
        val runnerClasses = shardRunners().map { it.nameWithoutExtension }
        val build = buildScript.readText()
        val ci = workflow.readText()

        runnerClasses.forEach { runner ->
            assertThat(build)
                .describedAs("%s must be registered as a CucumberTaskSpec", runner)
                .contains(runner)
        }

        val buildShardTasks = Regex("""name = "(cucumberTestShard[A-Za-z0-9]+)""")
            .findAll(build).map { it.groupValues[1] }.toSet()
        val ciShardTasks = Regex("""cucumberTestShard[A-Za-z0-9]+""")
            .findAll(ci).map { it.value }.toSet()

        assertThat(buildShardTasks)
            .describedAs("shard CucumberTaskSpec tasks")
            .hasSize(5)
        assertThat(ciShardTasks)
            .describedAs("full-tests.yml must run every shard task")
            .containsExactlyInAnyOrderElementsOf(buildShardTasks)
    }

    @Test
    fun `the functional shards form an exhaustive and disjoint partition of the functional tests`() {
        val functionalDir = repoRoot.resolve("capsule-plugin/src/functionalTest/kotlin")
        val all = functionalDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith("FunctionalTest.kt") }
            .map { file ->
                val pkg = Regex("""(?m)^package\s+([A-Za-z0-9_.]+)""").find(file.readText())!!.groupValues[1]
                "$pkg.${file.nameWithoutExtension}"
            }
            .toSet()

        val sharded = Regex("""--tests\s+(capsule\.[A-Za-z0-9_.]+)""")
            .findAll(workflow.readText())
            .map { it.groupValues[1] }
            .toList()

        assertThat(all).describedAs("functionalTest classes found").isNotEmpty()
        val duplicates = sharded.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertThat(duplicates)
            .describedAs("a functional test must belong to exactly one shard")
            .isEmpty()
        assertThat(sharded.toSet())
            .describedAs("the functional shards must cover every functional test")
            .containsExactlyInAnyOrderElementsOf(all)
    }
}
