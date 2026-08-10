package capsule.context

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import contracts.context.ContextChannel
import java.io.File

/**
 * Pure loader for the SPD (Scénario Pédagogique Détaillé) pedagogical payload
 * that feeds the capsule-local `spdSection` of [CapsuleContext] (CAP-SPD-1).
 *
 * Mirrors [DocContextLoader]: pure object (no Gradle, no I/O wiring) that
 * consumes pre-resolved files. The K-2 `metadata.json` envelope is validated
 * for content type (`type == "SPD"`) when present; the pedagogical payload
 * (objectives, duration, prerequisites, modalities, session title, module)
 * is extracted from the companion AsciiDoc — the metadata.json carries no
 * pedagogical fields (K-2 envelope only). The producer borough is NOT
 * validated: capsule is format-agnostic and consumes any SPD-compliant
 * source, public or private (capsule is public OSS and must not couple to
 * any specific producer borough).
 *
 * The rendered section follows the [CapsuleContextBuilder.merge] convention:
 * `==== SPD Pedagogical Context (spd)\nObjectives: ...\nDuration: ...\n
 * Prerequisites: ...\nModalities: ...\nSession: ...\nModule: ...`. The result
 * is truncated to [maxTokens] via the N0 [ContextChannel.Docs.truncateToTokens]
 * method (the Docs variant is semantically close — corpus documentaire — and
 * reusing it avoids extending the sealed N0 contract with a new `Spd` variant).
 *
 * Missing files are skipped silently — a null metadata file or a missing
 * AsciiDoc yields an empty string (backward compatible, no error). A
 * metadata.json whose `type` is not `"SPD"` is also skipped (graceful no-op).
 */
object SpdContextLoader {

    private val mapper: ObjectMapper = ObjectMapper()

    /**
     * Loads the SPD pedagogical section from [metadataFile] (optional K-2
     * envelope) and [adocFile] (companion AsciiDoc), truncated to
     * [maxTokens].
     *
     * @param metadataFile  optional `metadata.json` K-2 envelope. When null or
     *                      missing, the AsciiDoc is still parsed. When present
     *                      but malformed, it is skipped silently.
     * @param adocFile      the companion AsciiDoc SPD file. Missing or empty
     *                      file yields a blank string.
     * @param maxTokens     the SPD channel token budget (computed by the
     *                      wiring layer, e.g. 5% of the total budget).
     * @return the `==== SPD Pedagogical Context (spd)\n...` section, truncated
     *         to [maxTokens]. Blank when no usable payload or zero budget.
     */
    fun load(metadataFile: File?, adocFile: File, maxTokens: Int): String {
        if (maxTokens <= 0) return ""
        if (!adocFile.exists()) return ""

        val metadata = metadataFile?.takeIf { it.exists() }?.let { parseMetadata(it) }
        if (metadata != null && metadata.type.isNotEmpty() && metadata.type != "SPD") return ""

        val adocText = adocFile.readText()
        if (adocText.isBlank()) return ""

        val context = parseAdoc(adocFile.name, adocText)
        val raw = renderSection(context)
        if (raw.isBlank()) return ""

        val truncated = ContextChannel.Docs(raw).truncateToTokens(maxTokens)
        return truncated.content
    }

    /**
     * Parses the K-2 `metadata.json` envelope. Returns `null` when the file
     * is malformed (graceful skip) — never throws.
     */
    private fun parseMetadata(file: File): SpdMetadata? {
        return try {
            val node: JsonNode = mapper.readTree(file)
            SpdMetadata(
                type = node.path("type").asText(""),
                version = node.path("version").asText(""),
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts the pedagogical payload from the companion AsciiDoc:
     * - `sessionTitle` = first `= Title` heading, or filename fallback
     * - `module` = `:module:` document attribute, or empty string
     * - `objectives` = bullet list under `== Objectifs`
     * - `prerequisites` = bullet list under `== Prérequis`
     * - `modalities` = text block under `== Modalités d'évaluation`
     * - `duration` = first line under `== Durée estimée` (when present)
     */
    private fun parseAdoc(filename: String, text: String): SpdContext {
        val lines = text.lines()
        val title = extractTitle(lines) ?: filenameWithoutExtension(filename)
        val module = extractAttribute(lines, "module")
        val objectives = extractBulletSection(lines, "Objectifs")
        val prerequisites = extractBulletSection(lines, "Prérequis")
        val modalities = extractTextSection(lines, "Modalités d'évaluation").ifBlank {
            extractTextSection(lines, "Modalités")
        }
        val duration = extractTextSection(lines, "Durée estimée").ifBlank {
            extractTextSection(lines, "Durée")
        }
        return SpdContext(
            objectives = objectives,
            duration = duration,
            prerequisites = prerequisites,
            modalities = modalities,
            sessionTitle = title,
            module = module,
        )
    }

    /** Extracts the first `= Title` level-1 heading, or null when absent. */
    private fun extractTitle(lines: List<String>): String? {
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("= ") && !trimmed.startsWith("== ")) {
                return trimmed.removePrefix("= ").trim()
            }
        }
        return null
    }

    /** Extracts a `:key: value` document attribute, or empty string. */
    private fun extractAttribute(lines: List<String>, key: String): String {
        val prefix = ":$key:"
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith(prefix)) {
                return trimmed.removePrefix(prefix).trim().trim(':')
            }
        }
        return ""
    }

    /**
     * Extracts a bullet list (`- item`) under a `== <heading>` section,
     * stopping at the next `==` heading or end of file.
     */
    private fun extractBulletSection(lines: List<String>, heading: String): List<String> {
        val items = mutableListOf<String>()
        var inSection = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("== ")) {
                inSection = trimmed.removePrefix("== ").trim().equals(heading, ignoreCase = true)
                continue
            }
            if (inSection && trimmed.startsWith("- ")) {
                items.add(trimmed.removePrefix("- ").trim())
            }
        }
        return items
    }

    /**
     * Extracts the text block under a `== <heading>` section, stopping at the
     * next `==` heading or end of file. Blank lines within the block are
     * collapsed; the result is the trimmed concatenation of non-blank lines.
     */
    private fun extractTextSection(lines: List<String>, heading: String): String {
        val builder = StringBuilder()
        var inSection = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("== ")) {
                inSection = trimmed.removePrefix("== ").trim().equals(heading, ignoreCase = true)
                if (inSection) continue
                if (builder.isNotEmpty()) break
                continue
            }
            if (inSection && trimmed.isNotBlank()) {
                if (builder.isNotEmpty()) builder.append(" ")
                builder.append(trimmed)
            }
        }
        return builder.toString().trim()
    }

    /** Renders the [SpdContext] as the prompt-ready SPD section block. */
    private fun renderSection(context: SpdContext): String {
        val builder = StringBuilder()
        builder.append("==== SPD Pedagogical Context (spd)\n")
        builder.append("Session: ${context.sessionTitle}\n")
        if (context.module.isNotBlank()) builder.append("Module: ${context.module}\n")
        if (context.objectives.isNotEmpty()) {
            builder.append("Objectives: ${context.objectives.joinToString("; ")}\n")
        }
        if (context.duration.isNotBlank()) builder.append("Duration: ${context.duration}\n")
        if (context.prerequisites.isNotEmpty()) {
            builder.append("Prerequisites: ${context.prerequisites.joinToString("; ")}\n")
        }
        if (context.modalities.isNotBlank()) builder.append("Modalities: ${context.modalities}\n")
        return builder.toString().trimEnd()
    }

    private fun filenameWithoutExtension(filename: String): String =
        filename.substringBeforeLast('.')

    /** Internal validated metadata envelope (K-2 subset — type only). */
    private data class SpdMetadata(
        val type: String,
        val version: String,
    )
}