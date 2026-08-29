package capsule.ci

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Migration integrity guard for CAP-PR2-COV (PR #2).
 *
 * PR #2 replaced the fragile `ProcessBuilder(...).waitFor()` pattern — which
 * can deadlock on the 64 KiB pipe and silently truncates error output — with
 * [capsule.ProcessRunner], and `File.renameTo()` (whose boolean result was
 * ignored everywhere) with [capsule.FileReplace.moveOver]. This test locks the
 * migration: no production source may reintroduce the old pattern outside the
 * two classes that implement it. A regression (a new `ProcessBuilder` or
 * `.renameTo(` call) fails the build here, loudly, instead of at render time.
 */
class ProcessMigrationGuardTest {

    private val sourceRoot = File("src/main/kotlin")
    private val allowedFiles = setOf("ProcessRunner.kt", "FileReplace.kt")

    private val forbiddenTokens = listOf(
        "ProcessBuilder(" to "use capsule.ProcessRunner instead of ProcessBuilder",
        ".renameTo(" to "use capsule.FileReplace.moveOver instead of File.renameTo",
        ".waitFor(" to "use capsule.ProcessRunner.run/probe instead of Process.waitFor",
    )

    @Test
    fun `no production source uses the pre-migration process pattern`() {
        require(sourceRoot.isDirectory) { "source root not found at ${sourceRoot.absolutePath}" }
        val violations = mutableListOf<String>()
        sourceRoot.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") && it.name !in allowedFiles }
            .forEach { file ->
                val text = file.readText()
                forbiddenTokens.forEach { (token, hint) ->
                    if (text.contains(token)) {
                        violations += "${file.relativeTo(sourceRoot)}: '$token' — $hint"
                    }
                }
            }
        assertThat(violations)
            .describedAs("pre-migration process pattern must not appear outside $allowedFiles")
            .isEmpty()
    }
}
