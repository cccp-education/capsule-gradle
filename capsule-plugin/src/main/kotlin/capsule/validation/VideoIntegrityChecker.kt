package capsule.validation

/**
 * Integrity of a media file: is it structurally *decodable*, or merely
 * "present with a plausible duration"?
 *
 * Ported technique (PRT-04) from `prepa_electricite_complet/build.py:lisible()`:
 * a broken clip (a duplicated MOOV after two parallel renders) keeps a valid
 * duration yet breaks concatenation. The only check that catches it is to
 * DECODE the file and require both a zero exit code and an empty error output.
 *
 * Pure decision type — no I/O. The I/O half lives in [VideoIntegrityProbe];
 * the verdict is computed by [VideoIntegrityChecker] from the raw
 * `exitCode`/`stderr`, so it is testable without ffmpeg.
 */
sealed interface IntegrityResult {

    /** The file decodes cleanly (exit code 0 and no diagnostic output). */
    data object Clean : IntegrityResult

    /**
     * The file is structurally broken. [stderrTail] carries the decoder's
     * diagnostic (trimmed) — or a probe-level message when the decoder could
     * not even be run. A missing file must never be treated as clean.
     */
    data class Corrupt(val stderrTail: String) : IntegrityResult

    /** True when the media is decodable and therefore safe to reuse. */
    val isClean: Boolean get() = this is Clean
}

/**
 * Pure decision service for [IntegrityResult] (PRT-04).
 *
 * Given the raw outcome of a decoding command, decides whether the media is
 * clean. The rule is deliberately strict: a file is clean **only** when the
 * decoder exited with 0 AND printed nothing. Any diagnostic — even on a zero
 * exit — means the container lied about its content, which is exactly the
 * failure mode that produced a 621 s concatenation instead of 4397 s.
 *
 * Pure — no Gradle, no I/O, no ffmpeg. The caller (the integrity probe or a
 * validation task) runs the decoder and passes the numbers.
 */
object VideoIntegrityChecker {

    /**
     * Decides the integrity verdict from a decoder's raw outcome.
     *
     * @param exitCode the decoder's exit code (0 on a successful decode).
     * @param stderr   the decoder's merged diagnostic output.
     * @return [IntegrityResult.Clean] only when `exitCode == 0` and [stderr]
     *         is blank; [IntegrityResult.Corrupt] otherwise, carrying the
     *         trimmed diagnostic.
     */
    fun verdict(exitCode: Int, stderr: String): IntegrityResult {
        val diagnostic = stderr.trim()
        return if (exitCode == 0 && diagnostic.isEmpty()) {
            IntegrityResult.Clean
        } else {
            IntegrityResult.Corrupt(diagnostic)
        }
    }
}
