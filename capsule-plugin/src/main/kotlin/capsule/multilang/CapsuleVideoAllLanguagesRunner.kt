package capsule.multilang

import java.io.File

/**
 * Stateless orchestrator for the per-language capsule video generation loop.
 *
 * Iterates the [CapsuleVideoPlan] entries in order and applies the Economy of
 * Ink rule (AGENT.adoc): an entry whose output WebM already exists AND probes
 * with a duration > 0 is skipped — re-rendering it would waste TTS credits /
 * Playwright CPU on an artifact that is already valid and persistent.
 *
 * The heavy TTS + capture pipeline is injected as a [render] callback so the
 * orchestration (iteration + skip decision) stays a pure, unit-testable domain
 * decision while the Gradle task supplies the filesystem-bound work.
 */
object CapsuleVideoAllLanguagesRunner {

    /**
     * Runs [render] for every [plan] entry that has no valid existing output.
     *
     * @param plan plan of per-language video generations.
     * @param probeDuration probes a video file duration in seconds (0.0 when
     *        the file is missing or unreadable).
     * @param render performs the TTS + capture pipeline for one entry.
     * @return the entries actually rendered (skipped entries excluded), in
     *         plan order.
     */
    fun run(
        plan: CapsuleVideoPlan,
        probeDuration: (File) -> Double,
        render: (CapsuleVideoEntry) -> Unit,
    ): List<CapsuleVideoEntry> {
        val rendered = mutableListOf<CapsuleVideoEntry>()
        for (entry in plan.entries) {
            if (shouldRender(entry, probeDuration)) {
                render(entry)
                rendered.add(entry)
            }
        }
        return rendered
    }

    /**
     * True when the entry output must be (re)generated: the output WebM is
     * missing or probes with a non-positive duration (invalid artifact).
     */
    fun shouldRender(entry: CapsuleVideoEntry, probeDuration: (File) -> Double): Boolean {
        val output = entry.outputVideo
        if (!output.exists()) return true
        return probeDuration(output) <= 0.0
    }
}
