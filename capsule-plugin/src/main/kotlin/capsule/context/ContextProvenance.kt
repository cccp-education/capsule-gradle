package capsule.context

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Immutable provenance snapshot of the capsule augmented context
 * (CAP-PROVENANCE).
 *
 * Aggregates the per-channel [ProvenanceChannel]s that survived the token
 * budget, exported as the `context-provenance.json` artefact written by
 * [CollectCapsuleAugmentedContextTask]. The JSON schema is
 * `{ "channels": [...], "totalChars": N, "totalTokens": M }` — pretty
 * printed via Jackson [INDENT_OUTPUT] for diff-ability.
 *
 * An empty provenance is legitimate (empty augmented context) — no fail-fast
 * invariant. The mutable side is [ProvenanceTracker]; this data class is a
 * frozen snapshot.
 *
 * @property channels  the provenance channels that survived the budget.
 */
data class ContextProvenance(
    val channels: List<ProvenanceChannel> = emptyList(),
) {

    /** Sum of the [ProvenanceChannel.totalChars] of all [channels]. */
    @get:JsonProperty("totalChars")
    val totalChars: Int get() = channels.sumOf { it.totalChars() }

    /** Sum of the [ProvenanceChannel.totalTokens] of all [channels]. */
    @get:JsonProperty("totalTokens")
    val totalTokens: Int get() = channels.sumOf { it.totalTokens() }

    /** Serialises this snapshot as pretty-printed JSON (Jackson `INDENT_OUTPUT`). */
    fun toJson(): String = mapper.writeValueAsString(this)

    companion object {
        /** Capsule-local provenance label of the pedagogical scenario section (CAP-SPD). */
        const val SCENARIO_CHANNEL: String = "SCENARIO"

        /** Capsule-local provenance label of the official glossary section (CAP-GLOSSARY). */
        const val GLOSSARY_CHANNEL: String = "GLOSSARY"

        private val mapper: ObjectMapper =
            jacksonObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
    }
}
