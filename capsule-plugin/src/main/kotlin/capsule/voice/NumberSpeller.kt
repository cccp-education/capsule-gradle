package capsule.voice

/**
 * Spells integer numerals as words so a reference containing digits compares
 * equal to an ASR transcription that spells them (CAP-QC-VOICE, PRT-02 residual).
 *
 * The `prepa_electricite_complet` voice QC used `num2words` to expand numbers in
 * the reference before comparison (so "il y a 42 pommes" matches "il y a
 * quarante deux pommes"). This re-implements the same idea in pure Kotlin — no
 * external tool, no Python.
 *
 * French only; any other language returns the input unchanged. Pure — no I/O.
 */
object NumberSpeller {

    private val NUMBER = Regex("\\d+")

    /** Upper bound above which a numeral is left untouched (safety, narration numbers are small). */
    private const val MAX = 1_000_000_000_000L

    private val UNITS = arrayOf(
        "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit",
        "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
    )
    private val TENS = arrayOf("", "", "vingt", "trente", "quarante", "cinquante", "soixante")

    /**
     * Replaces every integer token in [text] by its French spelling. Other
     * languages (or numerals ≥ [MAX]) are returned unchanged.
     */
    fun spell(text: String, language: String = "fr"): String {
        if (!language.equals("fr", ignoreCase = true)) return text
        return NUMBER.replace(text) { m ->
            val value = m.value.toLongOrNull()
            if (value != null && value in 0..MAX) french(value) else m.value
        }
    }

    /** French cardinal spelling of [n]. */
    fun french(n: Long): String {
        if (n < 0) return "moins " + french(-n)
        if (n == 0L) return UNITS[0]

        val parts = mutableListOf<String>()
        val milliards = n / 1_000_000_000
        val millions = (n / 1_000_000) % 1_000
        val milliers = (n / 1_000) % 1_000
        val reste = (n % 1_000).toInt()

        if (milliards > 0) {
            parts += below1000(milliards.toInt()) + " milliard" + (if (milliards > 1) "s" else "")
        }
        if (millions > 0) {
            parts += below1000(millions.toInt()) + " million" + (if (millions > 1) "s" else "")
        }
        if (milliers > 0) {
            parts += if (milliers == 1L) "mille" else below1000(milliers.toInt()) + " mille"
        }
        if (reste > 0) parts += below1000(reste)

        return parts.joinToString(" ")
    }

    private fun below1000(n: Int): String {
        val hundreds = n / 100
        val rest = n % 100
        val sb = StringBuilder()
        if (hundreds > 0) {
            sb.append(if (hundreds == 1) "cent" else UNITS[hundreds] + " cent")
            if (rest == 0 && hundreds > 1) sb.append("s")
            if (rest > 0) sb.append(" ")
        }
        sb.append(below100(rest))
        return sb.toString().trim()
    }

    private fun below100(n: Int): String {
        if (n == 0) return ""
        if (n < 17) return UNITS[n]
        if (n < 20) return "dix-" + UNITS[n - 10]
        if (n < 70) {
            val tens = n / 10
            val unit = n % 10
            val base = TENS[tens]
            return when {
                unit == 0 -> base
                unit == 1 -> "$base et un"
                else -> "$base-${UNITS[unit]}"
            }
        }
        if (n < 80) {
            if (n == 71) return "soixante et onze"
            return "soixante-" + below100(n - 60)
        }
        if (n == 80) return "quatre-vingts"
        return "quatre-vingt-" + below100(n - 80)
    }
}
