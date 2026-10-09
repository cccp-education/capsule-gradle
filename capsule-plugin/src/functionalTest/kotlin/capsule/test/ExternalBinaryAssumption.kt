package capsule.test

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

/**
 * JUnit5 helper for functional tests that require an external binary
 * (`espeak`, `ffmpeg`, `ffprobe`…) installed on the host.
 *
 * The fast `Test` CI job runs `./gradlew check` (which includes
 * `functionalTest`) on a runner *without* these binaries; the `Full Tests`
 * job installs them. A test that asserts *real* audio/encoding must be
 * skipped — not failed — when the binary is absent, mirroring the
 * [PlaywrightTestAssumption] pattern (Queens S-269 / bakery S-243).
 *
 * Usage:
 * ```
 * @Test
 * fun `produces real audio`() {
 *     assumeBinaryAvailable("espeak")
 *     // ... test body ...
 * }
 * ```
 */
object ExternalBinaryAssumption {

    /**
     * Skips the calling test (via `assumeTrue`) when [binary] cannot be
     * resolved on the host.
     */
    fun assumeBinaryAvailable(binary: String) {
        assumeTrue(isAvailable(binary)) {
            "'$binary' not installed on this host — test skipped (runs in the Full Tests job)"
        }
    }

    /** `true` when [binary] is an executable found on a `PATH` entry. */
    fun isAvailable(binary: String): Boolean {
        val pathEntries = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
        return pathEntries.any { entry -> File(entry, binary).let { it.isFile && it.canExecute() } }
    }
}
