package capsule.pipeline

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [CapsuleState] — the immutable state of the CAP-ARCH-3
 * capsule content pipeline.
 *
 * Baby-step TDD (RED → GREEN): pure value object with fail-fast invariants,
 * pattern `slider.pipeline.DeckStateTest` (SLD-8.3). No Gradle, no LLM, no I/O.
 */
class CapsuleStateTest {

    private val sourceAdoc = "== Slide 1\n\n[NOTE.speaker]\n--\nNarration.\n--"

    private fun initialState(
        deckName: String = "kotlin-coroutines",
        language: String = "fr",
    ): CapsuleState = CapsuleState(
        deckName = deckName,
        language = language,
        sourceAdoc = sourceAdoc,
        augmentedContext = "RAG nugget about grading.",
        contentPlanJson = "",
    )

    // ─── construction ────────────────────────────────────────────────

    @Test
    fun `creates a CapsuleState with defaults for a fresh pipeline run`() {
        val state = initialState()
        assertEquals("kotlin-coroutines", state.deckName)
        assertEquals("fr", state.language)
        assertEquals(sourceAdoc, state.sourceAdoc)
        assertEquals("RAG nugget about grading.", state.augmentedContext)
        assertTrue(state.contentPlanJson.isEmpty())
        assertFalse(state.contextValid)
        assertEquals(null, state.validationError)
        assertTrue(state.speakerNotesAdoc.isEmpty())
        assertTrue(state.ttsScript.isEmpty())
        assertEquals(null, state.error)
        assertEquals(CapsuleStage.INITIALIZED, state.stage)
    }

    @Test
    fun `creates a fully populated CapsuleState`() {
        val state = CapsuleState(
            deckName = "kotlin-coroutines",
            language = "fr",
            sourceAdoc = sourceAdoc,
            augmentedContext = "ctx",
            contentPlanJson = """{"deckName":"kotlin-coroutines","languageCode":"fr"}""",
            contextValid = true,
            validationError = null,
            speakerNotesAdoc = "== Slide 1",
            ttsScript = "=== CAPSULE SCRIPT : kotlin-coroutines ===",
            error = null,
            stage = CapsuleStage.CONTENT_GENERATED,
        )
        assertEquals(CapsuleStage.CONTENT_GENERATED, state.stage)
        assertTrue(state.contextValid)
    }

    // ─── blank invariants ─────────────────────────────────────────────

    @Test
    fun `rejects a blank deckName`() {
        assertFailsWith<IllegalArgumentException> {
            initialState(deckName = "   ")
        }
    }

    @Test
    fun `rejects a blank language`() {
        assertFailsWith<IllegalArgumentException> {
            initialState(language = "")
        }
    }

    @Test
    fun `rejects a blank sourceAdoc`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(sourceAdoc = " ")
        }
    }

    // ─── cross-field invariants ───────────────────────────────────────

    @Test
    fun `rejects a non-null validationError when contextValid is true`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(
                contextValid = true,
                validationError = "Should not be set when valid",
                stage = CapsuleStage.CONTEXT_VALIDATED,
            )
        }
    }

    @Test
    fun `rejects contextValid true when stage is INITIALIZED`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(contextValid = true, stage = CapsuleStage.INITIALIZED)
        }
    }

    @Test
    fun `rejects a blank contentPlanJson when stage is CONTEXT_PROPOSED`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(
                contentPlanJson = "",
                stage = CapsuleStage.CONTEXT_PROPOSED,
            )
        }
    }

    @Test
    fun `rejects a non-blank speakerNotesAdoc when stage is INITIALIZED`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(
                speakerNotesAdoc = "== Generated",
                stage = CapsuleStage.INITIALIZED,
            )
        }
    }

    @Test
    fun `rejects a non-null error when stage is not FAILED`() {
        assertFailsWith<IllegalArgumentException> {
            initialState().copy(error = "Boom", stage = CapsuleStage.INITIALIZED)
        }
    }

    @Test
    fun `allows a non-null error when stage is FAILED`() {
        val state = initialState().copy(error = "Boom", stage = CapsuleStage.FAILED)
        assertEquals("Boom", state.error)
    }

    @Test
    fun `allows a blank augmentedContext — augmented context is optional`() {
        val state = initialState().copy(augmentedContext = "")
        assertTrue(state.augmentedContext.isEmpty())
    }

    // ─── copy semantics ───────────────────────────────────────────────

    @Test
    fun `copy returns a new instance with the modified fields`() {
        val initial = initialState()
        val proposed = initial.copy(
            contentPlanJson = """{"deckName":"kotlin-coroutines"}""",
            stage = CapsuleStage.CONTEXT_PROPOSED,
        )
        assertFalse(proposed === initial)
        assertEquals(initial.deckName, proposed.deckName)
        assertEquals(CapsuleStage.CONTEXT_PROPOSED, proposed.stage)
        assertEquals(CapsuleStage.INITIALIZED, initial.stage)
    }
}
