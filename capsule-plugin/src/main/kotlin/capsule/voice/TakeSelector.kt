package capsule.voice

/**
 * The take selected for one slide among several candidates (CAP-QC-VOICE US-3).
 *
 * Ported from `prepa_electricite_complet/compare_prises.py`: when a slide was
 * synthesised more than once, the best take is kept. "Best" means the lowest
 * word error rate, then the lowest surplus (fewer hallucinated words), then the
 * first candidate in input order.
 *
 * @param chosenAudioFile the file name of the retained take.
 * @param candidateCount  how many takes were compared.
 * @param wordErrorRate   the retained take's word error rate, or `null`.
 * @param surplus         the retained take's surplus, or `null`.
 * @param verdict         the retained take's verdict name (`PASS`/`FAIL`/`DEGRADED`).
 */
data class VoiceSelection(
    val deckName: String,
    val slideIndex: Int,
    val chosenAudioFile: String,
    val candidateCount: Int,
    val wordErrorRate: Double?,
    val surplus: Int?,
    val verdict: String,
)

/**
 * Pure take selector (CAP-QC-VOICE US-3).
 *
 * Given the [TakeVerification]s of the candidate takes of a slide, picks the
 * best one. The preference order is deliberate:
 *
 * . a non-failing take beats a failing one (a wrong `slide-NN.mp3` is worse than
 *    an unverified one);
 * . among non-failing takes, the lowest word error rate wins (a `Degraded` take
 *    has no rate and therefore loses to any verified take);
 * . ties break on the lowest surplus, then on input order (deterministic).
 *
 * Pure — no I/O, no Gradle.
 */
object TakeSelector {

    private val BEST_FIRST: Comparator<TakeVerification> =
        compareBy<TakeVerification> { if (it.verdict.isFail) 1 else 0 }
            .thenBy { it.wordErrorRate ?: Double.MAX_VALUE }
            .thenBy { it.surplus ?: Int.MAX_VALUE }

    /** The best candidate, or `null` when [candidates] is empty. */
    fun selectBest(candidates: List<TakeVerification>): TakeVerification? =
        candidates.minWithOrNull(BEST_FIRST)

    /**
     * Selects the best take per slide, one [VoiceSelection] per (deck, slide),
     * ordered by deck then slide. Slides with no candidate are omitted.
     */
    fun selectBySlide(candidates: List<TakeVerification>): List<VoiceSelection> =
        candidates.groupBy { it.deckName to it.slideIndex }
            .entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .mapNotNull { (key, group) ->
                selectBest(group)?.let { best ->
                    VoiceSelection(
                        deckName = key.first,
                        slideIndex = key.second,
                        chosenAudioFile = best.audioFile,
                        candidateCount = group.size,
                        wordErrorRate = best.wordErrorRate,
                        surplus = best.surplus,
                        verdict = verdictName(best.verdict),
                    )
                }
            }

    /** Stable verdict name for a report ([VoiceVerdict] → `PASS`/`FAIL`/`DEGRADED`). */
    fun verdictName(verdict: VoiceVerdict): String = when (verdict) {
        is VoiceVerdict.Pass -> "PASS"
        is VoiceVerdict.Fail -> "FAIL"
        is VoiceVerdict.Degraded -> "DEGRADED"
    }
}

/**
 * Serializes the take selections to the stable `choix.json` contract
 * (CAP-QC-VOICE US-3).
 *
 * ```json
 * [ { "deckName": "demo", "slideIndex": 1, "chosenAudioFile": "slide-01-2.mp3",
 *     "candidateCount": 2, "wordErrorRate": 0.0, "surplus": 0, "verdict": "PASS" } ]
 * ```
 *
 * Pattern: [VoiceReportJson] (Jackson). Pure — no Gradle, no I/O.
 */
object VoiceSelectionJson {

    private val mapper = com.fasterxml.jackson.databind.ObjectMapper()

    /** Serializes [selections] to a pretty JSON array (empty → `[]`). */
    fun toJson(selections: List<VoiceSelection>): String {
        val array = mapper.createArrayNode()
        selections.forEach { selection ->
            val node = mapper.createObjectNode()
            node.put("deckName", selection.deckName)
            node.put("slideIndex", selection.slideIndex)
            node.put("chosenAudioFile", selection.chosenAudioFile)
            node.put("candidateCount", selection.candidateCount)
            if (selection.wordErrorRate == null) node.putNull("wordErrorRate") else node.put("wordErrorRate", selection.wordErrorRate)
            if (selection.surplus == null) node.putNull("surplus") else node.put("surplus", selection.surplus)
            node.put("verdict", selection.verdict)
            array.add(node)
        }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array)
    }
}
