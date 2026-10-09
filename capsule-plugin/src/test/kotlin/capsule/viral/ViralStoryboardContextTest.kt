package capsule.viral

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD unit tests for the storyboard context model (CAP-CONTEXT US-1):
 * [Qqoqcp], [VisualIdentity], [NarrativeThread] and their additive wiring into
 * [ViralStoryboard] / [StoryboardBeat].
 *
 * The CAP-VIRAL storyboard carries *shape* (hook/CTA/duration) but no *context*.
 * CAP-CONTEXT adds the three context axes of `CAPSULE_VIDEO.adoc` (QQOQCP,
 * visual identity, narrative thread) as optional fields — additive and
 * backward-compatible: a storyboard without context stays valid.
 */
class ViralStoryboardContextTest {

    private fun qqoqcp() = Qqoqcp(
        audience = "Développeur solo",
        subject = "Créer un site perso",
        context = "Dans un dépôt local via bakery + opencode",
        moment = "Au démarrage d'un projet",
        modality = "Scaffold → rédaction → bake → publish",
        purpose = "Mettre un site perso en ligne",
    )

    private fun visualIdentity() = VisualIdentity(
        brand = "cheroliv.com",
        format = "9:16 (1080x1920)",
        tokens = mapOf("--accent-color" to "#0d6efd", "--footer-bg" to "#7952b3"),
        typography = "Inter",
        logo = "marque cheroliv",
        background = "charté (tokens)",
    )

    private fun narrativeThread() = NarrativeThread(
        thesis = "Un site perso, ça se génère",
        recurringMotif = "le terminal qui produit le site",
        arc = "accroche → démonstration → résolution → CTA",
        register = "pédagogique",
        voice = "direct, technique",
    )

    private fun storyboard(
        qqoqcp: Qqoqcp? = null,
        visualIdentity: VisualIdentity? = null,
        narrativeThread: NarrativeThread? = null,
    ) = ViralStoryboard(
        message = "Créer son site perso avec bakery + opencode",
        angle = "L'agent écrit, Gradle bake",
        beats = listOf(
            StoryboardBeat("hook", "Un site perso, sans HTML", 3.0),
            StoryboardBeat("development", "Scaffold bakery", 10.0, asset = "terminal.png"),
            StoryboardBeat("development", "opencode rédige", 10.0),
            StoryboardBeat("cta", "bakery-gradle + opencode", 7.0),
        ),
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = 30,
        qqoqcp = qqoqcp,
        visualIdentity = visualIdentity,
        narrativeThread = narrativeThread,
    )

    // ─── Qqoqcp ───────────────────────────────────────────────────────

    @Test
    fun `qqoqcp keeps its six answers`() {
        val q = qqoqcp()
        assertEquals("Développeur solo", q.audience)
        assertEquals("Créer un site perso", q.subject)
        assertEquals("Dans un dépôt local via bakery + opencode", q.context)
        assertEquals("Au démarrage d'un projet", q.moment)
        assertEquals("Scaffold → rédaction → bake → publish", q.modality)
        assertEquals("Mettre un site perso en ligne", q.purpose)
    }

    @Test
    fun `qqoqcp rejects a blank answer`() {
        assertTrue(runCatching { qqoqcp().copy(audience = "  ") }.isFailure)
        assertTrue(runCatching { qqoqcp().copy(purpose = "") }.isFailure)
    }

    // ─── VisualIdentity ───────────────────────────────────────────────

    @Test
    fun `visual identity keeps brand format and tokens`() {
        val vi = visualIdentity()
        assertEquals("cheroliv.com", vi.brand)
        assertEquals("9:16 (1080x1920)", vi.format)
        assertEquals("#0d6efd", vi.tokens["--accent-color"])
        assertEquals("Inter", vi.typography)
    }

    @Test
    fun `visual identity rejects blank brand format or empty tokens`() {
        assertTrue(runCatching { visualIdentity().copy(brand = "") }.isFailure)
        assertTrue(runCatching { visualIdentity().copy(format = " ") }.isFailure)
        assertTrue(runCatching { visualIdentity().copy(tokens = emptyMap()) }.isFailure)
    }

    // ─── NarrativeThread ──────────────────────────────────────────────

    @Test
    fun `narrative thread keeps thesis motif and arc`() {
        val nt = narrativeThread()
        assertEquals("Un site perso, ça se génère", nt.thesis)
        assertEquals("le terminal qui produit le site", nt.recurringMotif)
        assertEquals("accroche → démonstration → résolution → CTA", nt.arc)
        assertEquals("pédagogique", nt.register)
    }

    @Test
    fun `narrative thread rejects blank thesis motif or arc`() {
        assertTrue(runCatching { narrativeThread().copy(thesis = "") }.isFailure)
        assertTrue(runCatching { narrativeThread().copy(recurringMotif = " ") }.isFailure)
        assertTrue(runCatching { narrativeThread().copy(arc = "") }.isFailure)
    }

    // ─── Additive wiring + backward compatibility ─────────────────────

    @Test
    fun `storyboard carries the three context axes`() {
        val sb = storyboard(qqoqcp(), visualIdentity(), narrativeThread())
        assertEquals(qqoqcp(), sb.qqoqcp)
        assertEquals(visualIdentity(), sb.visualIdentity)
        assertEquals(narrativeThread(), sb.narrativeThread)
    }

    @Test
    fun `storyboard context is optional and backward compatible`() {
        val sb = storyboard()
        assertNull(sb.qqoqcp)
        assertNull(sb.visualIdentity)
        assertNull(sb.narrativeThread)
    }

    @Test
    fun `beat entering transition defaults to blank and round-trips the value`() {
        assertEquals("", StoryboardBeat("hook", "h", 3.0).enteringTransition)
        assertEquals("cut", StoryboardBeat("development", "d", 8.0, enteringTransition = "cut").enteringTransition)
    }
}
