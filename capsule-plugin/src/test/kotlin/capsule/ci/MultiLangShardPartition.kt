package capsule.ci

import contracts.i18n.SupportedLanguage

/**
 * Deterministic N-way partition of the `LanguageCatalog` for the heavy
 * multi-language generation CI shards.
 *
 * The multi-language TestKit scenario renders one WebM per language; a single
 * "all languages" scenario dominated the CI wall-clock, so it is split by
 * language group across disjoint CI machines. The split started as 2 groups
 * (`@multilang-gen-a` / `@multilang-gen-b`, ~260 s each) and is split again into
 * [GROUP_LETTERS] groups so no shard remains the pole.
 *
 * The partition is deterministic, exhaustive, disjoint and balanced within one
 * language — pinned by [MultiLangShardPartitionTest]. The heavy generation
 * scenario stays in `multi_language_capsule.feature` (tag `@multilang-gen-<letter>`)
 * and the shard coverage is guarded by [CapsuleCucumberShardCoverageTest].
 */
object MultiLangShardPartition {

    /** The language-group letters, in partition order. */
    val GROUP_LETTERS: List<String> = listOf("a", "b", "c", "d")

    /**
     * Returns the languages assigned to [group] (one of [GROUP_LETTERS])
     * out of [all], distributing any remainder over the first groups so no two
     * groups differ by more than one language.
     *
     * @throws IllegalArgumentException when [group] is not a known letter.
     */
    fun partition(group: String, all: List<SupportedLanguage>): List<SupportedLanguage> {
        val index = GROUP_LETTERS.indexOf(group.lowercase())
        require(index >= 0) { "Unknown language group: '$group' (expected one of ${GROUP_LETTERS.joinToString(", ")})" }

        val groupCount = GROUP_LETTERS.size
        val base = all.size / groupCount
        val remainder = all.size % groupCount

        var start = 0
        repeat(groupCount) { i ->
            val size = base + if (i < remainder) 1 else 0
            if (i == index) return all.subList(start, start + size)
            start += size
        }
        // Unreachable: the loop returns for the requested index.
        return emptyList()
    }
}
