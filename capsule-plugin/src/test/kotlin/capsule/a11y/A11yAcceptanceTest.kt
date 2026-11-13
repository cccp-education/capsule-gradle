package capsule.a11y

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * CAP-AUDIODESC US-4 — accessibility acceptance gate (verdict + declared limits).
 */
class A11yAcceptanceTest {
    private fun inputs(
        subtitles: Boolean = true,
        transcript: Boolean = true,
        requested: Boolean = true,
        present: Boolean = true,
    ) = A11yInputs(
        subtitlesPresent = subtitles,
        transcriptPresent = transcript,
        audioDescriptionRequested = requested,
        audioDescriptionPresent = present,
    )

    @Test
    fun `a complete deliverable is accepted and declares its limits`() {
        val verdict = A11yAcceptance.evaluate(inputs())

        assertIs<A11yVerdict.Accepted>(verdict)
        assertEquals(listOf(A11yAcceptance.EXTENDED_AUDIO_DESCRIPTION_LIMIT), verdict.limitations)
    }

    @Test
    fun `missing subtitles degrade the verdict`() {
        val verdict = A11yAcceptance.evaluate(inputs(subtitles = false))

        assertIs<A11yVerdict.Degraded>(verdict)
        assertTrue(verdict.reasons.any { it.contains("subtitles") })
    }

    @Test
    fun `missing transcription degrades the verdict`() {
        val verdict = A11yAcceptance.evaluate(inputs(transcript = false))

        assertIs<A11yVerdict.Degraded>(verdict)
        assertTrue(verdict.reasons.any { it.contains("transcription") })
    }

    @Test
    fun `a requested but absent audio description degrades the verdict`() {
        val verdict = A11yAcceptance.evaluate(inputs(requested = true, present = false))

        assertIs<A11yVerdict.Degraded>(verdict)
        assertTrue(verdict.reasons.any { it.contains("audio description") })
        assertEquals(listOf(A11yAcceptance.EXTENDED_AUDIO_DESCRIPTION_LIMIT), verdict.limitations)
    }

    @Test
    fun `an absent audio description is fine when it was not requested`() {
        val verdict = A11yAcceptance.evaluate(inputs(requested = false, present = false))

        assertIs<A11yVerdict.Accepted>(verdict)
    }
}
