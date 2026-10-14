package capsule.ci

import contracts.i18n.LanguageCatalog
import contracts.i18n.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Unit guard for [MultiLangShardPartition] — the deterministic N-way split of the
 * `LanguageCatalog` used by the heavy multi-language generation CI shards.
 *
 * The former 2-way split (`@multilang-gen-a` / `@multilang-gen-b`, ~260 s each)
 * was split again into 4 language groups so no shard dominates the CI wall-clock.
 * This test pins the partition invariants at the source: disjointness,
 * exhaustiveness, balance and determinism — an unbalanced or overlapping split
 * would silently re-create the CI pole it exists to remove.
 */
class MultiLangShardPartitionTest {

    private val all: List<SupportedLanguage> = LanguageCatalog.ALL

    @Test
    fun `the four groups form an exhaustive and disjoint partition of the catalog`() {
        val buckets = MultiLangShardPartition.GROUP_LETTERS.map { MultiLangShardPartition.partition(it, all) }

        assertThat(buckets.map { it.map(SupportedLanguage::code) }.flatten())
            .describedAs("every catalog language appears exactly once across the groups")
            .containsExactlyInAnyOrderElementsOf(all.map(SupportedLanguage::code))
    }

    @Test
    fun `the groups are balanced within one language`() {
        val sizes = MultiLangShardPartition.GROUP_LETTERS.map { MultiLangShardPartition.partition(it, all).size }

        assertThat(sizes).describedAs("each group renders a comparable amount of work").allMatch { it > 0 }
        assertThat(sizes.max() - sizes.min())
            .describedAs("the heaviest and lightest group differ by at most one language")
            .isLessThanOrEqualTo(1)
    }

    @Test
    fun `the partition is deterministic`() {
        MultiLangShardPartition.GROUP_LETTERS.forEach { letter ->
            assertThat(MultiLangShardPartition.partition(letter, all))
                .describedAs("group '%s' is stable across calls", letter)
                .isEqualTo(MultiLangShardPartition.partition(letter, all))
        }
    }

    @Test
    fun `an unknown group letter fails fast`() {
        assertThatThrownBy { MultiLangShardPartition.partition("e", all) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("e")
    }
}
