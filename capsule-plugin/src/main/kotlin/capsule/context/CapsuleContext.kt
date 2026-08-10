package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ContextChannel

/**
 * Immutable augmented context assembled for capsule content generation.
 *
 * Carries the budgeted, non-empty [ContextChannel]s of a codebase
 * [contracts.context.CompositeContext] together with their prompt-ready
 * [rendered] block (produced by [CapsuleContextBuilder.merge]) and the
 * capsule-local [spdSection] (CAP-SPD-2 — an SPD pedagogical payload rendered
 * by [SpdContextLoader] and appended after the N0 channels). This is the
 * consumption contract of the `capsule.pipeline` (CAP-ARCH-3) — content
 * generation feeds on `rendered`, never on the raw contract directly.
 *
 * Invariant: the rendered block is blank iff there are no channels AND the
 * [spdSection] is blank — an empty channel list alone does not imply an empty
 * rendered block when the SPD section is present.
 *
 * @property channels   non-empty channels after budget truncation + filtering.
 * @property rendered   prompt-ready sectioned text of [channels] + [spdSection].
 * @property spdSection  the capsule-local SPD pedagogical section (CAP-SPD-2),
 *                      blank when no SPD payload was provided.
 */
data class CapsuleContext(
    val channels: List<ContextChannel>,
    val rendered: String,
    val spdSection: String = "",
) {
    init {
        require(rendered.isBlank() == (channels.isEmpty() && spdSection.isBlank())) {
            "CapsuleContext rendered block must be blank iff there are no channels and no spdSection"
        }
    }

    /** `true` when the augmented context carries no usable channel. */
    val isEmpty: Boolean get() = channels.isEmpty()

    /** Number of channels that actually contribute content. */
    val nonEmptyCount: Int get() = channels.count { it.isNotEmpty() }

    /** Rough token estimate of the [rendered] block (N0 heuristic). */
    val tokenEstimate: Int get() = ContextChannel.estimateTokens(rendered)
}
