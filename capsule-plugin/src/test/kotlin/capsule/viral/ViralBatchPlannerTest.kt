package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral batch matrix (CAP-VIRAL US-6):
 * [ViralVariant] + [ViralBatchPlanner].
 */
class ViralBatchPlannerTest {

    @Test
    fun `plan builds the cartesian product language-major`() {
        val variants = ViralBatchPlanner.plan(
            languages = listOf("fr", "en"),
            platforms = listOf(ViralPlatform.YOUTUBE_SHORT, ViralPlatform.TIKTOK),
            duration = 30,
        )
        assertEquals(4, variants.size)
        assertEquals(listOf("fr", "fr", "en", "en"), variants.map { it.language })
        assertEquals(
            listOf(ViralPlatform.YOUTUBE_SHORT, ViralPlatform.TIKTOK, ViralPlatform.YOUTUBE_SHORT, ViralPlatform.TIKTOK),
            variants.map { it.platform },
        )
    }

    @Test
    fun `variant ids are deterministic language-platform-duration`() {
        val v = ViralVariant("fr", ViralPlatform.TIKTOK, 30)
        assertEquals("fr-tiktok-30", v.id)
    }

    @Test
    fun `plan de-duplicates languages and platforms`() {
        val variants = ViralBatchPlanner.plan(
            languages = listOf("fr", "fr", " en "),
            platforms = listOf(ViralPlatform.TIKTOK, ViralPlatform.TIKTOK),
            duration = 30,
        )
        assertEquals(2, variants.size)
        assertEquals(listOf("fr", "en"), variants.map { it.language }.distinct())
    }

    @Test
    fun `plan returns empty when no language or platform`() {
        assertTrue(ViralBatchPlanner.plan(emptyList(), listOf(ViralPlatform.TIKTOK), 30).isEmpty())
        assertTrue(ViralBatchPlanner.plan(listOf("fr"), emptyList(), 30).isEmpty())
        assertTrue(ViralBatchPlanner.plan(listOf(" ", ""), listOf(ViralPlatform.TIKTOK), 30).isEmpty())
    }

    @Test
    fun `plan coerces the duration into the short-form range`() {
        val low = ViralBatchPlanner.plan(listOf("fr"), listOf(ViralPlatform.TIKTOK), 5)
        val high = ViralBatchPlanner.plan(listOf("fr"), listOf(ViralPlatform.TIKTOK), 120)
        assertEquals(15, low.single().targetDuration)
        assertEquals(60, high.single().targetDuration)
    }

    @Test
    fun `ViralVariant rejects blank language and out-of-range duration`() {
        assertTrue(runCatching { ViralVariant("", ViralPlatform.TIKTOK, 30) }.isFailure)
        assertTrue(runCatching { ViralVariant("fr", ViralPlatform.TIKTOK, 5) }.isFailure)
    }
}
