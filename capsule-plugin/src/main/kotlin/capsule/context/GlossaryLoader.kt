package capsule.context

import contracts.context.ContextChannel
import java.io.File

/**
 * Pure loader for a training referential glossary that feeds the
 * capsule-local [glossarySection][CapsuleContext.glossarySection] of
 * [CapsuleContext] (CAP-GLOSSARY-1).
 *
 * Mirrors [CapsuleScenarioLoader]: pure object (no Gradle, no I/O wiring)
 * that consumes a pre-resolved file. The glossary is an AsciiDoc `== Glossary`
 * section containing a list of bullets `- term: definition` (one per line).
 * The parsing is purely textual (no AsciidoctorJ dependency — the workspace is
 * AsciiDoc-native and the format is trivial: split on `== Glossary`, extract
 * the `- term: definition` lines, split on the first `:`).
 *
 * The rendered section follows the [CapsuleContextBuilder.merge] convention:
 * `==== Official Glossary (glossary)\n<term>: <definition>\n...`. The result
 * is truncated to [maxTokens] via the N0 [ContextChannel.Docs.truncateToTokens]
 * method (the Docs variant is semantically close — corpus documentaire — and
 * reusing it avoids extending the sealed N0 contract with a new variant).
 *
 * Missing files are skipped silently — a missing glossary yields an empty
 * string (backward compatible, no error). Malformed bullets (not
 * `- term: definition`) are skipped. Non-`Glossary` sections are ignored.
 * Glossary absent → video not blocked (fallback no-op, backward compat strict).
 */
object GlossaryLoader {

    /**
     * Loads the glossary section from [glossaryFile], truncated to
     * [maxTokens].
     *
     * @param glossaryFile the glossary AsciiDoc file. Missing or empty file
     *                     yields a blank string.
     * @param maxTokens    the glossary channel token budget (computed by the
     *                     wiring layer, e.g. 5% of the total budget).
     * @return the `==== Official Glossary (glossary)\n...` section, truncated
     *         to [maxTokens]. Blank when no usable payload or zero budget.
     */
    fun load(glossaryFile: File, maxTokens: Int): String {
        if (maxTokens <= 0) return ""
        if (!glossaryFile.exists()) return ""

        val text = glossaryFile.readText()
        if (text.isBlank()) return ""

        val terms = parseGlossary(text)
        if (terms.isEmpty()) return ""

        val raw = renderSection(terms)
        val truncated = ContextChannel.Docs(raw).truncateToTokens(maxTokens)
        return truncated.content
    }

    /**
     * Extracts the [GlossaryEntry] list from the `== Glossary` section of
     * [text]. Returns an empty list when the section is absent, empty, or
     * contains no valid `- term: definition` bullets. Malformed bullets are
     * skipped. Non-`Glossary` sections are ignored.
     */
    private fun parseGlossary(text: String): List<GlossaryEntry> {
        val entries = mutableListOf<GlossaryEntry>()
        var inSection = false
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("== ")) {
                inSection = trimmed.removePrefix("== ").trim().equals("Glossary", ignoreCase = true)
                continue
            }
            if (!inSection) continue
            val bullet = trimmed.removePrefix("- ").trim()
            if (bullet.isEmpty() || bullet == trimmed) continue
            val colonIdx = bullet.indexOf(':')
            if (colonIdx <= 0) continue
            val term = bullet.substring(0, colonIdx).trim()
            val definition = bullet.substring(colonIdx + 1).trim()
            if (term.isBlank() || definition.isBlank()) continue
            entries.add(GlossaryEntry(term = term, definition = definition))
        }
        return entries
    }

    /** Renders the [GlossaryEntry] list as the prompt-ready glossary section block. */
    private fun renderSection(terms: List<GlossaryEntry>): String {
        val builder = StringBuilder()
        builder.append("==== Official Glossary (glossary)\n")
        for (entry in terms) {
            builder.append("${entry.term}: ${entry.definition}\n")
        }
        return builder.toString().trimEnd()
    }
}
