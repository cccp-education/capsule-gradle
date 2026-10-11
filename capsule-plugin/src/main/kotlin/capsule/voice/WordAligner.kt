package capsule.voice

/** Kind of one [AlignmentOp]. */
enum class OpKind { MATCH, SUBSTITUTE, DELETE, INSERT }

/**
 * One word-level operation of an edit alignment.
 *
 * @param kind       the operation.
 * @param reference  the reference word (null for an [OpKind.INSERT]).
 * @param hypothesis the hypothesis word (null for an [OpKind.DELETE]).
 */
data class AlignmentOp(val kind: OpKind, val reference: String?, val hypothesis: String?)

/**
 * A word-level edit alignment: the [distance] plus the ordered [operations].
 */
data class Alignment(val distance: Int, val operations: List<AlignmentOp>)

/**
 * Word-level edit-distance aligner (CAP-QC-VOICE US-1).
 *
 * Ported from `prepa_electricite_complet/verif_voix.py` (`alignement`): a
 * Levenshtein dynamic program over words that also returns the operation list,
 * so a reviewer can read the differences word by word (`~`/`+`/`-`).
 *
 * Pure — no I/O.
 */
object WordAligner {

    /**
     * Aligns [reference] with [hypothesis].
     *
     * @return the [Alignment] (distance + ordered operations); an empty pair
     *         yields distance 0 and no operation.
     */
    fun align(reference: List<String>, hypothesis: List<String>): Alignment {
        val n = reference.size
        val m = hypothesis.size
        val d = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) d[i][0] = i
        for (j in 0..m) d[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                val substitution = d[i - 1][j - 1] + if (reference[i - 1] == hypothesis[j - 1]) 0 else 1
                d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, substitution)
            }
        }

        val operations = ArrayDeque<AlignmentOp>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            val substitution = if (i > 0 && j > 0) {
                d[i - 1][j - 1] + if (reference[i - 1] == hypothesis[j - 1]) 0 else 1
            } else Int.MAX_VALUE

            when {
                i > 0 && j > 0 && d[i][j] == substitution -> {
                    val kind = if (reference[i - 1] == hypothesis[j - 1]) OpKind.MATCH else OpKind.SUBSTITUTE
                    operations.addFirst(AlignmentOp(kind, reference[i - 1], hypothesis[j - 1]))
                    i--
                    j--
                }
                i > 0 && d[i][j] == d[i - 1][j] + 1 -> {
                    operations.addFirst(AlignmentOp(OpKind.DELETE, reference[i - 1], null))
                    i--
                }
                else -> {
                    operations.addFirst(AlignmentOp(OpKind.INSERT, null, hypothesis[j - 1]))
                    j--
                }
            }
        }
        return Alignment(d[n][m], operations.toList())
    }
}
