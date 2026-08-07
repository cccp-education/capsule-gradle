package capsule.ci

/**
 * Guard deciding whether the `cucumberTest` task should run.
 *
 * CR-10 — cucumberTest is expensive (Playwright + FFmpeg, ~15 min) and must
 * be skipped unless the user explicitly opts in via `-PrunCucumber` or the
 * build runs on a CI server (`CI=true`). This object is a pure decision
 * function with no Gradle coupling so it can be unit-tested in isolation.
 */
data class CucumberTestGuard(
    val hasRunCucumberProperty: Boolean,
    val isCi: Boolean
) {
    fun shouldRun(): Boolean = hasRunCucumberProperty || isCi

    fun shouldSkip(): Boolean = !shouldRun()

    fun skipReason(): String =
        if (shouldRun()) ""
        else "cucumberTest skipped (pass -PrunCucumber or set CI=true to enable)"
}