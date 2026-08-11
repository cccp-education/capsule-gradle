package capsule.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

/**
 * TDD unit tests for [VideoDurationValidator] (CAP-CR3-1 US-1).
 *
 * `VideoDurationValidator` is a pure domain object that compares the
 * probed video duration against the sum of per-slide audio durations
 * and returns a sealed [ValidationResult].
 *
 * Baby-step TDD: RED → GREEN → REFACTOR. No I/O, no ffprobe — the
 * validator receives raw numbers.
 */
class VideoDurationValidatorTest {

    @Test
    fun `valid when delta within tolerance`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 10.0,
            audioDurationsSecs = listOf(5.0, 5.0),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Valid>(result)
        assertEquals(0.0, result.delta)
    }

    @Test
    fun `valid when delta exactly at tolerance`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 12.0,
            audioDurationsSecs = listOf(5.0, 5.0),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Valid>(result)
        assertEquals(2.0, result.delta)
    }

    @Test
    fun `invalid when delta exceeds tolerance`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 12.3,
            audioDurationsSecs = listOf(5.0, 5.0),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(2.3, result.delta, 0.001)
        assertEquals(12.3, result.videoDuration, 0.001)
        assertEquals(10.0, result.sumAudio, 0.001)
        assertEquals(2.0, result.toleranceSecs, 0.001)
    }

    @Test
    fun `exact match produces delta zero`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 15.0,
            audioDurationsSecs = listOf(5.0, 5.0, 5.0),
            toleranceSecs = 0.0
        )
        assertIs<ValidationResult.Valid>(result)
        assertEquals(0.0, result.delta)
    }

    @Test
    fun `empty audio list with positive video duration is invalid`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 10.0,
            audioDurationsSecs = emptyList(),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(10.0, result.delta, 0.001)
        assertEquals(0.0, result.sumAudio, 0.001)
    }

    @Test
    fun `empty audio list with zero video duration is valid`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 0.0,
            audioDurationsSecs = emptyList(),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Valid>(result)
        assertEquals(0.0, result.delta)
    }

    @Test
    fun `single audio duration compared to video`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 7.5,
            audioDurationsSecs = listOf(7.5),
            toleranceSecs = 0.0
        )
        assertIs<ValidationResult.Valid>(result)
        assertEquals(0.0, result.delta)
    }

    @Test
    fun `tolerance zero is strict comparison`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 10.01,
            audioDurationsSecs = listOf(10.0),
            toleranceSecs = 0.0
        )
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(0.01, result.delta, 0.001)
    }

    @Test
    fun `negative video duration produces invalid with positive delta`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = -1.0,
            audioDurationsSecs = listOf(5.0),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(6.0, result.delta, 0.001)
    }

    @Test
    fun `invalid message contains actionable details`() {
        val result = VideoDurationValidator.validate(
            videoDurationSecs = 12.3,
            audioDurationsSecs = listOf(5.0, 5.0),
            toleranceSecs = 2.0
        )
        assertIs<ValidationResult.Invalid>(result)
        val message = result.message()
        assertTrue(message.contains("video=12.30s"), "Expected video=12.30s in message: $message")
        assertTrue(message.contains("audio=10.00s"), "Expected audio=10.00s in message: $message")
        assertTrue(message.contains("delta=2.30s"), "Expected delta=2.30s in message: $message")
        assertTrue(message.contains("tolerance=2.00s"), "Expected tolerance=2.00s in message: $message")
    }
}