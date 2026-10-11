package capsule

/**
 * Turns a speaker note into the text that belongs ON SCREEN (PRT-03).
 *
 * The text a subtitle shows is not the text a voice reads: the voice track may
 * carry markup or phonetic spellings, while the cue must be clean, readable
 * prose. Ported from `prepa_electricite_complet/subs.py:lisible`, which
 * stripped markdown, re-collected spelled acronyms and reversed its own
 * pronunciation spellings before writing the `.srt`.
 *
 * Pure — no I/O.
 */
object SubtitleTextNormalizer {

    /** Paired emphasis marker (`**bold**`) that must never reach the screen. */
    private const val EMPHASIS = "**"

    /**
     * A run of two or more single uppercase letters separated by single
     * spaces, e.g. the spelled-out form `A P I` produced for the voice.
     */
    private val SPELLED_ACRONYM = Regex("""\b(?:[A-Z] ){1,6}[A-Z]\b""")

    private val WHITESPACE = Regex("""\s+""")

    /**
     * Normalises [text] for display:
     * - applies [reversals] first (pronunciation → display, e.g. `ène` → `n`);
     * - strips paired markdown emphasis (`**`);
     * - re-collects spelled acronyms (`A P I` → `API`);
     * - collapses runs of whitespace and trims.
     *
     * @param reversals pronunciation→display substitutions; empty by default.
     *        Populated once the pronunciation lexicon (PRT-01) exists, so a
     *        phonetic spelling written for the voice is not shown verbatim.
     */
    fun spokenToDisplay(text: String, reversals: Map<String, String> = emptyMap()): String {
        var out = text
        for ((from, to) in reversals) {
            if (from.isNotEmpty()) out = out.replace(from, to)
        }
        out = out.replace(EMPHASIS, "")
        out = SPELLED_ACRONYM.replace(out) { it.value.replace(" ", "") }
        return WHITESPACE.replace(out, " ").trim()
    }
}
