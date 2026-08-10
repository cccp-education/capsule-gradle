package capsule.context

import contracts.context.ChannelBudget
import contracts.context.CompositeContext
import contracts.context.ContextChannel

/**
 * Pure assembler of the capsule augmented context.
 *
 * EPIC CAP-ARCH (S-085): capsule consumes the N0 `codebase-contracts` context
 * contract ([CompositeContext] + [ContextChannel] + [ChannelBudget]) and
 * renders it into the prompt-ready block that will feed the CAP-ARCH-3 koog
 * pipeline. This object is pure — no I/O, no Gradle, no network — which keeps
 * the assemble logic unit-testable and reusable by the future
 * [capsule.pipeline] domain.
 *
 * The canonical channels come from [CompositeContext.toChannels] (EAGER → RAG
 * → GRAPHIFY → DOCS → RESOURCE), the token budgeting from
 * [CompositeContext.channelsWithBudget], and the rendering from [merge].
 */
object CapsuleContextBuilder {

    /** The 5 canonical channels of [composite], in contract order. */
    fun toChannels(composite: CompositeContext): List<ContextChannel> = composite.toChannels()

    /** The [composite] channels truncated to their per-channel token budget. */
    fun channelsWithBudget(
        composite: CompositeContext,
        budget: ChannelBudget,
    ): List<ContextChannel> = composite.channelsWithBudget(budget)

    /**
     * Merges the non-empty [channels] into a prompt-ready sectioned block.
     *
     * Each channel renders as `==== <sectionHeader> (<name>)` followed by its
     * content. Blank channels are dropped — an empty or all-blank input yields
     * a blank block.
     */
    fun merge(channels: List<ContextChannel>): String =
        channels
            .filter { it.isNotEmpty() }
            .joinToString("\n\n") { channel ->
                val header = channel.sectionHeader
                if (header.isBlank()) {
                    channel.content
                } else {
                    "==== $header (${channel.name})\n${channel.content}"
                }
            }

    /**
     * Assembles the augmented context for a generation intent.
     *
     * Budgets the composite channels via [channelsWithBudget], drops the empty
     * ones and renders the survivors via [merge]. When [scenarioSection] is
     * non-blank, it is appended after the channel blocks (CAP-SPD-2 — the
     * pedagogical scenario payload is capsule-local and does not extend the
     * sealed N0 [ContextChannel] contract).
     */
    fun build(
        composite: CompositeContext,
        budget: ChannelBudget = defaultBudget(),
        scenarioSection: String = "",
    ): CapsuleContext {
        val channels = composite.channelsWithBudget(budget).filter { it.isNotEmpty() }
        val channelBlock = merge(channels)
        val trimmedScenario = scenarioSection.trim()
        val rendered = when {
            channelBlock.isBlank() && trimmedScenario.isBlank() -> ""
            channelBlock.isBlank() -> trimmedScenario
            trimmedScenario.isBlank() -> channelBlock
            else -> "$channelBlock\n\n$trimmedScenario"
        }
        return CapsuleContext(channels = channels, rendered = rendered, scenarioSection = trimmedScenario)
    }

    /** The N0 default token budget (8000 tokens, 40/30/20/10/0). */
    fun defaultBudget(): ChannelBudget = ChannelBudget()
}
