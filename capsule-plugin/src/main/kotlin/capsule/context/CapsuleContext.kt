package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ContextChannel

/**
 * Immutable augmented context assembled for capsule content generation.
 *
 * Carries the budgeted, non-empty [ContextChannel]s of a codebase
 * [contracts.context.CompositeContext] together with their prompt-ready
 * [rendered] block (produced by [CapsuleContextBuilder.merge]). This is the
 * consumption contract of the `capsule.pipeline` (CAP-ARCH-3) — content
 * generation feeds on `rendered`, never on the raw contract directly.
 *
 * Invariant: a context holding channels must have a non-blank rendered block
 * (an empty channel list implies an empty rendered block).
 *
 * @property channels   non-empty channels after budget truncation + filtering.
 * @property rendered   prompt-ready sectioned text of [channels].
 */
data class CapsuleContext(
    val channels: List<ContextChannel>,
    val rendered: String,
) {
    init {
        require(rendered.isBlank() == channels.isEmpty()) {
            "CapsuleContext rendered block must be blank iff there are no channels"
        }
    }

    /** `true` when the augmented context carries no usable channel. */
    val isEmpty: Boolean get() = channels.isEmpty()

    /** Number of channels that actually contribute content. */
    val nonEmptyCount: Int get() = channels.count { it.isNotEmpty() }

    /** Rough token estimate of the [rendered] block (N0 heuristic). */
    val tokenEstimate: Int get() = ContextChannel.estimateTokens(rendered)
}
