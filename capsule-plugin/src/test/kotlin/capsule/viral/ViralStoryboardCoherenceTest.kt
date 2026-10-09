package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the storyboard coherence gate (CAP-CONTEXT US-3):
 * [ViralStoryboardValidator] extended with the context coherence rules of the
 * `CAPSULE_VIDEO.adoc` Gate ("fil conducteur : arc complet + transition par
 * séquence").
 *
 * The coherence rules apply **only when the storyboard carries context** — a
 * context-less storyboard keeps the exact CAP-VIRAL verdict (backward compat).
 */
class ViralStoryboardCoherenceTest {

    private fun qqoqcp() = Qqoqcp(
        audience = "Développeur solo",
        subject = "Créer un site perso",
        context = "Dépôt local via bakery + opencode",
        moment = "Au démarrage d'un projet",
        modality = "Scaffold → rédaction → bake",
        purpose = "Mettre un site en ligne",
    )

    private fun visualIdentity() = VisualIdentity(
        brand = "cheroliv.com",
        format = "9:16 (1080x1920)",
        tokens = mapOf("--accent-color" to "#0d6efd"),
    )

    private fun narrativeThread() = NarrativeThread(
        thesis = "Un site perso, ça se génère",
        recurringMotif = "le terminal",
        arc = "accroche → démonstration → résolution → CTA",
    )

    private fun storyboard(
        qqoqcp: Qqoqcp? = null,
        visualIdentity: VisualIdentity? = null,
        narrativeThread: NarrativeThread? = null,
        transitions: List<String> = listOf("— (ouverture)", "cut", "fade"),
    ) = ViralStoryboard(
        message = "Créer son site perso avec bakery + opencode",
        angle = "L'agent écrit, Gradle bake",
        beats = listOf(
            StoryboardBeat("hook", "Un site perso, sans HTML", 3.0, enteringTransition = transitions[0]),
            StoryboardBeat("development", "Scaffold bakery", 10.0, enteringTransition = transitions[1]),
            StoryboardBeat("cta", "bakery-gradle + opencode", 7.0, enteringTransition = transitions[2]),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 20,
        qqoqcp = qqoqcp,
        visualIdentity = visualIdentity,
        narrativeThread = narrativeThread,
    )

    @Test
    fun `coherent contextualised storyboard is valid`() {
        val sb = storyboard(qqoqcp(), visualIdentity(), narrativeThread())
        assertEquals(StoryboardValidationResult.Valid, ViralStoryboardValidator.validate(sb))
    }

    @Test
    fun `context-less storyboard keeps the cap-viral verdict`() {
        assertEquals(StoryboardValidationResult.Valid, ViralStoryboardValidator.validate(storyboard()))
    }

    @Test
    fun `partial context is incoherent`() {
        val sb = storyboard(qqoqcp = qqoqcp())
        val r = ViralStoryboardValidator.validate(sb)
        assertTrue(r is StoryboardValidationResult.Invalid)
        assertTrue((r as StoryboardValidationResult.Invalid).reasons.any { it.contains("partial") })
    }

    @Test
    fun `narrative thread requires an entering transition per sequence`() {
        val sb = storyboard(
            qqoqcp(), visualIdentity(), narrativeThread(),
            transitions = listOf("— (ouverture)", "", "fade"),
        )
        val r = ViralStoryboardValidator.validate(sb)
        assertTrue(r is StoryboardValidationResult.Invalid)
        assertTrue((r as StoryboardValidationResult.Invalid).reasons.any { it.contains("transition") })
    }
}
