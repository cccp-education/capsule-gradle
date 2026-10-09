package capsule.viral

import capsule.feed.SlideType

/**
 * Pure parser of the storyboard AsciiDoc document (CAP-VIRAL wiring, extended by
 * CAP-CONTEXT US-2).
 *
 * The inverse of [ViralStoryboardBuilder]: it reads the document the pilot
 * validated back into a [ViralStoryboard] so the Gradle task can drive the
 * rendering from a document, not from a set of properties. The round-trip
 * `parse(build(sb)) == sb` holds for every valid storyboard.
 *
 * CAP-CONTEXT parses the three context axes (`== QQOQCP`,
 * `== Identité visuelle (design system)`, `== Fil conducteur narratif`) when
 * present and the per-beat `Transition:` line. Absent sections yield `null`
 * fields — a context-less document parses exactly as before.
 *
 * Degradation (economy of ink): a malformed or incomplete document returns
 * `null` — the caller treats it as "no validable storyboard" and the editorial
 * gate blocks rendering. The parser never throws on bad input.
 *
 * Pure — no Gradle, no I/O. Pattern: [ViralHookParser] (tolerant, returns null).
 */
object ViralStoryboardParser {

    private const val HEADER_PREFIX = "= Storyboard — "
    private const val ANGLE_MARKER = "== Angle"
    private const val BEATS_MARKER = "== Beats"
    private const val QQOQCP_MARKER = "== QQOQCP"
    private const val VISUAL_MARKER = "== Identité visuelle (design system)"
    private const val NARRATIVE_MARKER = "== Fil conducteur narratif"
    private const val BEAT_PREFIX = "=== Beat "
    private const val INTENT_PREFIX = "Intent: "
    private const val DURATION_PREFIX = "Duration: "
    private const val RENDER_PREFIX = "Render: "
    private const val MANIM_PREFIX = "Manim scene: "
    private const val ASSET_PREFIX = "Asset: "
    private const val TRANSITION_PREFIX = "Transition: "
    private const val TOKEN_PREFIX = "Token: "

    /**
     * Parses [document] into a [ViralStoryboard].
     *
     * @param document the storyboard AsciiDoc document.
     * @return the [ViralStoryboard], or `null` when the document is not a
     *         well-formed validable storyboard.
     */
    fun parse(document: String): ViralStoryboard? {
        val lines = document.lines()
        val header = lines.firstOrNull { it.startsWith(HEADER_PREFIX) } ?: return null
        val message = header.removePrefix(HEADER_PREFIX).trim()
        if (message.isBlank()) return null

        val platform = lines.firstOrNull { it.startsWith(":platform:") }
            ?.removePrefix(":platform:")?.trim()
            ?.let { ViralPlatform.fromString(it) } ?: return null

        val language = lines.firstOrNull { it.startsWith(":language:") }
            ?.removePrefix(":language:")?.trim().orEmpty()
        if (language.isBlank()) return null

        val duration = lines.firstOrNull { it.startsWith(":target-duration:") }
            ?.removePrefix(":target-duration:")?.trim()?.toIntOrNull() ?: return null

        val angle = extractAngle(lines) ?: return null
        val beats = extractBeats(lines)
        if (beats.isEmpty()) return null

        return runCatching {
            ViralStoryboard(
                message = message,
                angle = angle,
                beats = beats,
                platform = platform,
                language = language,
                targetDurationSeconds = duration,
                qqoqcp = extractQqoqcp(lines),
                visualIdentity = extractVisualIdentity(lines),
                narrativeThread = extractNarrativeThread(lines),
            )
        }.getOrNull()
    }

    /**
     * Extracts the body of the level-2 [marker] section: the lines after the
     * marker up to the next level-2 section (`== `) or EOF. Level-3 beat headers
     * (`=== Beat …`) do **not** terminate a body (their third char is `=`, not a
     * space, so `startsWith("== ")` is false).
     */
    private fun extractSection(lines: List<String>, marker: String): List<String> {
        val start = lines.indexOfFirst { it.trim() == marker }
        if (start < 0) return emptyList()
        val end = (start + 1 until lines.size)
            .firstOrNull { lines[it].trim().startsWith("== ") }
            ?: lines.size
        return lines.subList(start + 1, end)
    }

    /** Extracts the `== Angle` body (the paragraph that follows the marker). */
    private fun extractAngle(lines: List<String>): String? =
        extractSection(lines, ANGLE_MARKER).joinToString("\n").trim().ifBlank { null }

    /** Parses the `== QQOQCP` section, or `null` when absent/incomplete. */
    private fun extractQqoqcp(lines: List<String>): Qqoqcp? {
        val body = extractSection(lines, QQOQCP_MARKER)
        if (body.isEmpty()) return null
        fun answer(label: String): String? =
            body.firstOrNull { it.startsWith(label) }?.removePrefix(label)?.trim()?.ifBlank { null }

        val audience = answer("Qui (public): ") ?: return null
        val subject = answer("Quoi (sujet): ") ?: return null
        val context = answer("Où (contexte): ") ?: return null
        val moment = answer("Quand (moment): ") ?: return null
        val modality = answer("Comment (modalité): ") ?: return null
        val purpose = answer("Pourquoi (but): ") ?: return null

        return runCatching { Qqoqcp(audience, subject, context, moment, modality, purpose) }.getOrNull()
    }

    /** Parses the `== Identité visuelle` section, or `null` when absent/incomplete. */
    private fun extractVisualIdentity(lines: List<String>): VisualIdentity? {
        val body = extractSection(lines, VISUAL_MARKER)
        if (body.isEmpty()) return null
        fun value(label: String): String =
            body.firstOrNull { it.startsWith(label) }?.removePrefix(label)?.trim().orEmpty()

        val brand = value("Brand: ")
        val format = value("Format: ")
        val tokens = body.filter { it.startsWith(TOKEN_PREFIX) }
            .mapNotNull { row ->
                val content = row.removePrefix(TOKEN_PREFIX).trim()
                val separator = content.indexOf(" = ")
                if (separator < 0) null else content.substring(0, separator).trim() to content.substring(separator + 3).trim()
            }
            .toMap()
        if (brand.isBlank() || format.isBlank() || tokens.isEmpty()) return null

        return runCatching {
            VisualIdentity(
                brand = brand,
                format = format,
                tokens = tokens,
                typography = value("Typography: "),
                logo = value("Logo: "),
                background = value("Background: "),
            )
        }.getOrNull()
    }

    /** Parses the `== Fil conducteur narratif` section, or `null` when absent/incomplete. */
    private fun extractNarrativeThread(lines: List<String>): NarrativeThread? {
        val body = extractSection(lines, NARRATIVE_MARKER)
        if (body.isEmpty()) return null
        fun value(label: String): String =
            body.firstOrNull { it.startsWith(label) }?.removePrefix(label)?.trim().orEmpty()

        val thesis = value("Thesis: ")
        val recurringMotif = value("Recurring motif: ")
        val arc = value("Arc: ")
        if (thesis.isBlank() || recurringMotif.isBlank() || arc.isBlank()) return null

        return runCatching {
            NarrativeThread(
                thesis = thesis,
                recurringMotif = recurringMotif,
                arc = arc,
                register = value("Register: "),
                voice = value("Voice: "),
            )
        }.getOrNull()
    }

    /**
     * Extracts the ordered beats. Each beat block starts at `=== Beat` and ends
     * at the next beat marker (or EOF). Blank lines within a block (e.g. after
     * `Render:`) are skipped when reconstructing a beat.
     */
    private fun extractBeats(lines: List<String>): List<StoryboardBeat> {
        val blocks = mutableListOf<MutableList<String>>()
        var current: MutableList<String>? = null
        for (line in lines) {
            if (line.startsWith(BEAT_PREFIX)) {
                current = mutableListOf(line)
                blocks += current
            } else if (current != null) {
                current += line
            }
        }
        return blocks.mapNotNull { parseBeat(it) }
    }

    private fun parseBeat(block: List<String>): StoryboardBeat? {
        val header = block.first()
        val role = header.removePrefix(BEAT_PREFIX)
            .substringAfter("— ", "")
            .trim()
        if (role.isBlank()) return null

        val intent = block.firstOrNull { it.startsWith(INTENT_PREFIX) }
            ?.removePrefix(INTENT_PREFIX)?.trim().orEmpty()
        if (intent.isBlank()) return null

        val duration = block.firstOrNull { it.startsWith(DURATION_PREFIX) }
            ?.removePrefix(DURATION_PREFIX)?.trim()?.removeSuffix("s")?.toDoubleOrNull() ?: return null

        val typeName = block.firstOrNull { it.startsWith(RENDER_PREFIX) }
            ?.removePrefix(RENDER_PREFIX)?.trim().orEmpty()
        val type = SlideType.entries.firstOrNull { it.name.equals(typeName, ignoreCase = true) }
            ?: SlideType.HTML

        val manimScene = block.firstOrNull { it.startsWith(MANIM_PREFIX) }
            ?.removePrefix(MANIM_PREFIX)?.trim()?.ifBlank { null }

        val asset = block.firstOrNull { it.startsWith(ASSET_PREFIX) }
            ?.removePrefix(ASSET_PREFIX)?.trim().orEmpty()

        val enteringTransition = block.firstOrNull { it.startsWith(TRANSITION_PREFIX) }
            ?.removePrefix(TRANSITION_PREFIX)?.trim().orEmpty()

        return runCatching {
            StoryboardBeat(
                role = role,
                intent = intent,
                durationSeconds = duration,
                type = type,
                manimScene = manimScene,
                asset = asset,
                enteringTransition = enteringTransition,
            )
        }.getOrNull()
    }
}
