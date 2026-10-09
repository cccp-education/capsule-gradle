package capsule.test

import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

/**
 * JUnit5 helper for unit tests that require an external binary
 * (`ffmpeg`, `ffprobe`…) installed on the host.
 *
 * The fast `Test` CI job runs `./gradlew test` on a runner *without* these
 * binaries; the `Full Tests` job installs them. A test that needs a real
 * binary must be skipped — not failed — when it is absent, mirroring the
 * functional-test [ExternalBinaryAssumption] pattern (Queens S-269 /
 * bakery S-243 / capsule S-139).
 */
object BinaryAvailability {

    /** Skips the calling test (via `assumeTrue`) when [binary] is absent. */
    fun assumeAvailable(binary: String) {
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
