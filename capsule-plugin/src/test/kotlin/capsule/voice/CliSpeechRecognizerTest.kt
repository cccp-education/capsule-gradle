package capsule.voice

import capsule.ProcessRunner
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TDD unit tests for [SpeechRecognizerCommand] and [CliSpeechRecognizer]
 * (CAP-QC-VOICE US-2). The process is intercepted — no real whisper needed.
 */
class CliSpeechRecognizerTest {

    @AfterTest
    fun tearDown() {
        ProcessRunner.commandInterceptor = null
    }

    private val audio = File("build/tmp/slide-01.mp3")
    private val outputDir = File("build/tmp/asr-out")

    @Test
    fun `the argv asks for a txt transcript in the owned directory`() {
        val args = SpeechRecognizerCommand.args("whisper", audio, "base", "fr", outputDir)

        assertEquals("whisper", args.first())
        assertEquals(audio.absolutePath, args[1])
        assertTrue(args.windowed(2).any { it == listOf("--model", "base") })
        assertTrue(args.windowed(2).any { it == listOf("--language", "fr") })
        assertTrue(args.windowed(2).any { it == listOf("--output_format", "txt") })
        assertTrue(args.windowed(2).any { it == listOf("--output_dir", outputDir.absolutePath) })
    }

    @Test
    fun `blank model and language are omitted`() {
        val args = SpeechRecognizerCommand.args("faster-whisper", audio, "", "", outputDir)

        assertTrue("--model" !in args)
        assertTrue("--language" !in args)
    }

    @Test
    fun `the adapter reads the transcript the cli wrote`() {
        ProcessRunner.commandInterceptor = { command, _, _, _, _ ->
            val dir = File(command.last())
            File(dir, "slide-01.txt").writeText("bonjour le monde")
            ProcessRunner.Result(0, "")
        }

        val transcript = CliSpeechRecognizer("whisper").transcribe(audio, "fr")

        assertEquals("bonjour le monde", transcript)
    }

    @Test
    fun `a non-zero exit raises so the take degrades`() {
        ProcessRunner.commandInterceptor = { _, _, _, _, _ -> ProcessRunner.Result(1, "boom") }

        assertFailsWith<SpeechRecognizerException> {
            CliSpeechRecognizer("whisper").transcribe(audio, "fr")
        }
    }

    @Test
    fun `a blank transcript falls back to an empty string`() {
        ProcessRunner.commandInterceptor = { _, _, _, _, _ -> ProcessRunner.Result(0, "") }

        assertEquals("", CliSpeechRecognizer("whisper").transcribe(audio, "fr"))
    }
}
