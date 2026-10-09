package capsule.viral

import capsule.feed.SlideType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD unit tests for the viral storyboard gate (CAP-VIRAL US-13):
 * [StoryboardBeat], [ViralStoryboard], [ViralStoryboardBuilder],
 * [ViralStoryboardValidator].
 *
 * A campaign is first a document carrying a message / point of view, whose
 * function is to propose a validable storyboard transformable into a deployable
 * video. The validator is the editorial gate (mirror of rule A9).
 */
class ViralStoryboardTest {

    private fun beat(
        role: String,
        intent: String = "intent",
        duration: Double = 3.0,
        type: SlideType = SlideType.HTML,
        manimScene: String? = null,
        asset: String = "",
    ) = StoryboardBeat(role, intent, duration, type, manimScene, asset)

    private fun storyboard(
        beats: List<StoryboardBeat> = listOf(
            beat("hook", "Et si 30 secondes suffisaient ?", 3.0),
            beat("development", "Les 4 activités types", 10.0),
            beat("development", "Le rôle du formateur", 10.0),
            beat("cta", "Découvrez FPA", 7.0),
        ),
        target: Int = 30,
    ) = ViralStoryboard(
        message = "Devenir formateur professionnel",
        angle = "Le métier en 30 secondes",
        beats = beats,
        platform = ViralPlatform.TIKTOK,
        language = "fr",
        targetDurationSeconds = target,
    )

    // ─── Model invariants ─────────────────────────────────────────────

    @Test
    fun `storyboard exposes hook and cta beats`() {
        val sb = storyboard()
        assertEquals("hook", sb.hookBeat?.role)
        assertEquals("cta", sb.ctaBeat?.role)
        assertEquals(30.0, sb.totalBeatSeconds, 0.001)
    }

    @Test
    fun `storyboard rejects empty beats or blank message`() {
        assertTrue(runCatching { storyboard(beats = emptyList()) }.isFailure)
        assertTrue(
            runCatching {
                ViralStoryboard("", "angle", listOf(beat("hook")), ViralPlatform.TIKTOK, "fr", 30)
            }.isFailure,
        )
    }

    @Test
    fun `beat requires a manim scene when type is MANIM`() {
        assertTrue(runCatching { beat("development", type = SlideType.MANIM) }.isFailure)
        assertTrue(runCatching { beat("development", type = SlideType.MANIM, manimScene = "Scene") }.isSuccess)
    }

    // ─── Builder ──────────────────────────────────────────────────────

    @Test
    fun `builder renders a deterministic asciidoc document`() {
        val doc = ViralStoryboardBuilder.build(storyboard())
        assertTrue(doc.contains("= Storyboard — Devenir formateur professionnel"))
        assertTrue(doc.contains(":platform: TIKTOK"))
        assertTrue(doc.contains(":target-duration: 30"))
        assertTrue(doc.contains("=== Beat 1 — hook"))
        assertTrue(doc.contains("=== Beat 4 — cta"))
        assertEquals(doc, ViralStoryboardBuilder.build(storyboard()), "builder must be deterministic")
    }

    @Test
    fun `builder renders the manim scene when present`() {
        val sb = storyboard(
            beats = listOf(
                beat("hook", "h", 3.0),
                beat("development", "d", 24.0, type = SlideType.MANIM, manimScene = "MoveSquare"),
                beat("cta", "c", 3.0),
            ),
        )
        assertTrue(ViralStoryboardBuilder.build(sb).contains("Manim scene: MoveSquare"))
    }

    // ─── Validator (editorial gate) ───────────────────────────────────

    @Test
    fun `validator accepts a complete storyboard`() {
        assertEquals(StoryboardValidationResult.Valid, ViralStoryboardValidator.validate(storyboard()))
    }

    @Test
    fun `validator rejects a storyboard without hook or cta`() {
        val noHook = storyboard(beats = listOf(beat("development", duration = 30.0)))
        val noCta = storyboard(beats = listOf(beat("hook", duration = 30.0)))
        val rNoHook = ViralStoryboardValidator.validate(noHook)
        val rNoCta = ViralStoryboardValidator.validate(noCta)
        assertTrue(rNoHook is StoryboardValidationResult.Invalid)
        assertTrue(rNoCta is StoryboardValidationResult.Invalid)
        assertTrue((rNoHook as StoryboardValidationResult.Invalid).reasons.any { it.contains("hook") })
        assertTrue((rNoCta as StoryboardValidationResult.Invalid).reasons.any { it.contains("cta") })
    }

    @Test
    fun `validator rejects a total duration too far from target`() {
        val tooShort = storyboard(
            beats = listOf(beat("hook", duration = 1.0), beat("cta", duration = 1.0)),
            target = 30,
        )
        val r = ViralStoryboardValidator.validate(tooShort)
        assertTrue(r is StoryboardValidationResult.Invalid)
        assertTrue((r as StoryboardValidationResult.Invalid).reasons.any { it.contains("deviates") })
    }

    @Test
    fun `validator rejects a beat longer than 10 seconds`() {
        val sb = storyboard(
            beats = listOf(beat("hook", duration = 3.0), beat("development", duration = 20.0), beat("cta", duration = 7.0)),
        )
        val r = ViralStoryboardValidator.validate(sb)
        assertTrue(r is StoryboardValidationResult.Invalid)
        assertTrue((r as StoryboardValidationResult.Invalid).reasons.any { it.contains("(> 10.0s)") })
    }
}
