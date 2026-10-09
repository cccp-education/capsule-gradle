package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for the storyboard context rendering (CAP-CONTEXT US-2):
 * [ViralStoryboardBuilder] and [ViralStoryboardParser] extended with the three
 * context axes (QQOQCP, visual identity, narrative thread) and the beat entering
 * transition.
 *
 * Backward compatibility (economy of ink): when no context is carried, the
 * builder output is unchanged and the round-trip `parse(build(sb)) == sb` still
 * holds. With context present, the round-trip carries the three axes.
 */
class ViralStoryboardContextRoundTripTest {

    private fun fullStoryboard() = ViralStoryboard(
        message = "Créer son site perso avec bakery + opencode",
        angle = "L'agent écrit, Gradle bake",
        beats = listOf(
            StoryboardBeat("hook", "Un site perso, sans HTML", 3.0, enteringTransition = "— (ouverture)"),
            StoryboardBeat("development", "Scaffold bakery", 10.0, asset = "terminal.png", enteringTransition = "cut"),
            StoryboardBeat("cta", "bakery-gradle + opencode", 7.0, enteringTransition = "fade"),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 20,
        qqoqcp = Qqoqcp(
            audience = "Développeur solo",
            subject = "Créer un site perso",
            context = "Dans un dépôt local via bakery + opencode",
            moment = "Au démarrage d'un projet",
            modality = "Scaffold → rédaction → bake → publish",
            purpose = "Mettre un site perso en ligne",
        ),
        visualIdentity = VisualIdentity(
            brand = "cheroliv.com",
            format = "9:16 (1080x1920)",
            tokens = linkedMapOf("--accent-color" to "#0d6efd", "--footer-bg" to "#7952b3"),
            typography = "Inter",
            logo = "marque cheroliv",
            background = "charté (tokens)",
        ),
        narrativeThread = NarrativeThread(
            thesis = "Un site perso, ça se génère",
            recurringMotif = "le terminal qui produit le site",
            arc = "accroche → démonstration → résolution → CTA",
            register = "pédagogique",
            voice = "direct, technique",
        ),
    )

    private fun plainStoryboard() = fullStoryboard().copy(
        beats = listOf(
            StoryboardBeat("hook", "h", 3.0),
            StoryboardBeat("development", "d", 10.0),
            StoryboardBeat("cta", "c", 7.0),
        ),
        qqoqcp = null,
        visualIdentity = null,
        narrativeThread = null,
    )

    // ─── Builder — back-compat + context sections ─────────────────────

    @Test
    fun `builder omits the context sections when no context is carried`() {
        val doc = ViralStoryboardBuilder.build(plainStoryboard())
        assertFalse(doc.contains("== QQOQCP"))
        assertFalse(doc.contains("Identité visuelle"))
        assertFalse(doc.contains("Fil conducteur"))
        assertFalse(doc.contains("Transition: "))
    }

    @Test
    fun `builder renders the qqoqcp section`() {
        val doc = ViralStoryboardBuilder.build(fullStoryboard())
        assertTrue(doc.contains("== QQOQCP"))
        assertTrue(doc.contains("Qui (public): Développeur solo"))
        assertTrue(doc.contains("Pourquoi (but): Mettre un site perso en ligne"))
    }

    @Test
    fun `builder renders the visual identity section`() {
        val doc = ViralStoryboardBuilder.build(fullStoryboard())
        assertTrue(doc.contains("== Identité visuelle (design system)"))
        assertTrue(doc.contains("Brand: cheroliv.com"))
        assertTrue(doc.contains("Format: 9:16 (1080x1920)"))
        assertTrue(doc.contains("Token: --accent-color = #0d6efd"))
        assertTrue(doc.contains("Token: --footer-bg = #7952b3"))
    }

    @Test
    fun `builder renders the narrative thread section`() {
        val doc = ViralStoryboardBuilder.build(fullStoryboard())
        assertTrue(doc.contains("== Fil conducteur narratif"))
        assertTrue(doc.contains("Thesis: Un site perso, ça se génère"))
        assertTrue(doc.contains("Recurring motif: le terminal qui produit le site"))
        assertTrue(doc.contains("Arc: accroche → démonstration → résolution → CTA"))
    }

    @Test
    fun `builder renders the beat entering transition`() {
        val doc = ViralStoryboardBuilder.build(fullStoryboard())
        assertTrue(doc.contains("Transition: cut"))
        assertTrue(doc.contains("Transition: fade"))
    }

    // ─── Parser — round-trip ──────────────────────────────────────────

    @Test
    fun `round-trip carries the three context axes`() {
        val source = fullStoryboard()
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source, parsed)
    }

    @Test
    fun `round-trip carries a partial context (qqoqcp only)`() {
        val source = fullStoryboard().copy(visualIdentity = null, narrativeThread = null)
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source.qqoqcp, parsed.qqoqcp)
        assertEquals(null, parsed.visualIdentity)
        assertEquals(null, parsed.narrativeThread)
    }

    @Test
    fun `round-trip is unchanged when no context is carried`() {
        val source = plainStoryboard()
        val parsed = assertNotNull(ViralStoryboardParser.parse(ViralStoryboardBuilder.build(source)))
        assertEquals(source, parsed)
    }
}
