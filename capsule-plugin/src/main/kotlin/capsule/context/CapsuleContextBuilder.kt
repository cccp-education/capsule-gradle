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
     * sealed N0 [ContextChannel] contract). When [glossarySection] is
     * non-blank, it is appended after [scenarioSection] (CAP-GLOSSARY-2 — the
     * official glossary payload is also capsule-local and does not extend the
     * N0 contract).
     *
     * When [tracker] is non-null (CAP-PROVENANCE), it is pruned to the
     * channels that survived the budget + the scenario section + the glossary
     * section: the collector tracks the sources per channel before calling
     * this method, and the builder signals which channel names are still
     * alive via [ProvenanceTracker.retainOnly]. The builder does NOT compute
     * sources — it only validates the surviving channel names (the collector
     * owns the source [File] objects). Backward compatible: a null [tracker]
     * (default) performs no tracking.
     */
    fun build(
        composite: CompositeContext,
        budget: ChannelBudget = defaultBudget(),
        scenarioSection: String = "",
        glossarySection: String = "",
        tracker: ProvenanceTracker? = null,
    ): CapsuleContext {
        val channels = composite.channelsWithBudget(budget).filter { it.isNotEmpty() }
        val channelBlock = merge(channels)
        val trimmedScenario = scenarioSection.trim()
        val trimmedGlossary = glossarySection.trim()
        val rendered = buildRendered(channelBlock, trimmedScenario, trimmedGlossary)
        if (tracker != null) {
            val survivingChannels = channels.map { it.type.name }.toMutableSet()
            if (trimmedScenario.isNotBlank()) {
                survivingChannels += ContextProvenance.SCENARIO_CHANNEL
            }
            if (trimmedGlossary.isNotBlank()) {
                survivingChannels += ContextProvenance.GLOSSARY_CHANNEL
            }
            tracker.retainOnly(survivingChannels)
        }
        return CapsuleContext(
            channels = channels,
            rendered = rendered,
            scenarioSection = trimmedScenario,
            glossarySection = trimmedGlossary,
        )
    }

    private fun buildRendered(channelBlock: String, trimmedScenario: String, trimmedGlossary: String): String {
        val parts = listOf(channelBlock, trimmedScenario, trimmedGlossary).filter { it.isNotBlank() }
        return parts.joinToString("\n\n")
    }

    /** The N0 default token budget (8000 tokens, 40/30/20/10/0). */
    fun defaultBudget(): ChannelBudget = ChannelBudget()
}
