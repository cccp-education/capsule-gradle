package capsule.multilang

import contracts.i18n.SupportedLanguage
import java.io.File

/**
 * Stateless builder of [CapsuleVideoPlan] from the translated deck + script
 * artifacts produced upstream by the slider pipeline.
 *
 * For each target language, the planner looks for a translated deck
 * (`<deckName>_<lang>-deck.html` in [deckDir]) and its matching script
 * (`<deckName>_<lang>-script.txt` in [scriptDir]). A language is skipped
 * silently when either artifact is missing — the upstream `translateDeck` or
 * `extractSpeakerNotes` task may have legitimately not produced it (partial
 * translation, source equals target). When no language yields a matching
 * pair, the planner fails fast: an empty plan is a misconfiguration.
 *
 * ## Script naming tolerance (real slider→capsule chain)
 *
 * The real chain names translated decks `<slug>_<lang>-deck.adoc`; the capsule
 * `extractSpeakerNotes` task derives the script name from the AsciiDoc file
 * name *without extension*, so it emits `<slug>_<lang>-deck-script.txt` — the
 * `-deck` suffix survives. The planner therefore accepts either the exact
 * `<deckName>_<lang>-script.txt` (preferred, hand-named artifacts and tests)
 * or the `-deck` suffixed `<deckName>_<lang>-deck-script.txt` (real chain).
 * This mirrors, on the input side, the S-082 fix that already aligned the
 * output WebM.
 *
 * Output naming convention: `<outputDir>/<deckName>_<lang>.webm`.
 */
object CapsuleVideoPlanner {

    /**
     * Build a [CapsuleVideoPlan] from the translated deck + script directories.
     *
     * @param deckDir        directory holding `<deckName>_<lang>-deck.html` files.
     * @param scriptDir      directory holding `<deckName>_<lang>-script.txt` files.
     * @param outputDir      directory where `<deckName>_<lang>.webm` will be written.
     * @param targetLanguages target languages to include (N0 [SupportedLanguage]).
     * @return a non-empty [CapsuleVideoPlan].
     * @throws IllegalArgumentException when no language yields a matching
     *         deck + script pair.
     */
    fun plan(
        deckDir: File,
        scriptDir: File,
        outputDir: File,
        targetLanguages: List<SupportedLanguage>,
    ): CapsuleVideoPlan {
        val entries = mutableListOf<CapsuleVideoEntry>()

        for (language in targetLanguages) {
            val deckFiles = deckDir.listFiles { f ->
                f.isFile && f.name.endsWith("_${language.code}-deck.html")
            } ?: emptyArray()

            for (deckFile in deckFiles) {
                val deckName = deckNameOf(deckFile, language.code)
                val scriptFile = resolveScript(scriptDir, deckName, language.code) ?: continue

                val outputVideo = outputDir.resolve("${deckName}_${language.code}.webm")
                entries.add(
                    CapsuleVideoEntry(
                        deckFile = deckFile,
                        scriptFile = scriptFile,
                        language = language,
                        outputVideo = outputVideo,
                    )
                )
            }
        }

        require(entries.isNotEmpty()) {
            "no matching deck + script pair found for target languages: " +
                targetLanguages.joinToString(", ") { it.code }
        }
        return CapsuleVideoPlan(entries)
    }

    /**
     * Resolves the script file for a deck name + language code, tolerating the
     * `-deck` suffix produced by the real `extractSpeakerNotes` chain.
     *
     * The exact `<deckName>_<code>-script.txt` is preferred; when absent, the
     * `<deckName>_<code>-deck-script.txt` variant is accepted. Returns `null`
     * when neither exists.
     */
    private fun resolveScript(scriptDir: File, deckName: String, code: String): File? {
        val exact = scriptDir.resolve("${deckName}_${code}-script.txt")
        if (exact.exists()) return exact
        val deckSuffixed = scriptDir.resolve("${deckName}_${code}-deck-script.txt")
        return deckSuffixed.takeIf { it.exists() }
    }

    /**
     * Extract the deck name prefix from a translated deck file name.
     *
     * `kotlin-basics_fr-deck.html` → `kotlin-basics`.
     */
    private fun deckNameOf(deckFile: File, code: String): String {
        val name = deckFile.name
        val suffix = "_$code-deck.html"
        return if (name.endsWith(suffix)) name.removeSuffix(suffix) else name.removeSuffix(".html")
    }
}