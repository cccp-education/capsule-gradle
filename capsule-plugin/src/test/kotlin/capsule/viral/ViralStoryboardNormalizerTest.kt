package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TDD unit tests for the viral storyboard normalizer: fills the missing per-beat
 * entering transitions from the role-based default policy, preserving the explicit
 * ones — so a hand-authored storyboard passes the editorial gate.
 */
class ViralStoryboardNormalizerTest {

    private fun storyboard(vararg beats: StoryboardBeat) = ViralStoryboard(
        message = "Maîtriser opencode",
        angle = "L'agent écrit, Gradle assemble",
        beats = beats.toList(),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 30,
    )

    private fun beat(role: String, intent: String = "Intent", transition: String = "") =
        StoryboardBeat(role = role, intent = intent, durationSeconds = 5.0, enteringTransition = transition)

    @Test
    fun `fills a missing transition from the role default`() {
        val normalized = ViralStoryboardNormalizer.normalize(
            storyboard(beat("hook"), beat("development"), beat("cta")),
        )
        assertEquals("— (ouverture)", normalized.beats[0].enteringTransition)
        assertEquals("cut", normalized.beats[1].enteringTransition)
        assertEquals("fade", normalized.beats[2].enteringTransition)
    }

    @Test
    fun `preserves an explicit transition`() {
        val normalized = ViralStoryboardNormalizer.normalize(
            storyboard(beat("development", transition = "dissolve")),
        )
        assertEquals("dissolve", normalized.beats[0].enteringTransition)
    }

    @Test
    fun `falls back to cut for an unknown role`() {
        val normalized = ViralStoryboardNormalizer.normalize(storyboard(beat("teaser")))
        assertEquals(ViralStoryboardNormalizer.FALLBACK_TRANSITION, normalized.beats[0].enteringTransition)
    }

    @Test
    fun `resolves the default transition case-insensitively`() {
        assertEquals("fade", ViralStoryboardNormalizer.defaultFor("CTA"))
        assertEquals("cut", ViralStoryboardNormalizer.defaultFor("  Development "))
    }

    @Test
    fun `touches no other field`() {
        val source = storyboard(beat("hook", intent = "Et si ?"))
        val normalized = ViralStoryboardNormalizer.normalize(source)
        assertEquals("Et si ?", normalized.beats[0].intent)
        assertEquals(5.0, normalized.beats[0].durationSeconds)
        assertEquals(source.message, normalized.message)
        assertEquals(source.angle, normalized.angle)
        assertEquals(source.platform, normalized.platform)
    }

    @Test
    fun `is idempotent`() {
        val once = ViralStoryboardNormalizer.normalize(storyboard(beat("hook"), beat("development"), beat("cta")))
        assertEquals(once, ViralStoryboardNormalizer.normalize(once))
    }
}
