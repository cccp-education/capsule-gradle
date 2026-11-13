package capsule.a11y

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * CAP-AUDIODESC US-3 — pure FFmpeg command for the audio-description mix.
 */
class AudioDescriptionMixCommandTest {
    private fun input(
        name: String,
        start: Double,
    ) = AudioDescriptionInput(File("/tmp/$name.mp3"), start)

    @Test
    fun `a single cue is delayed and ducked under the narration`() {
        val graph =
            AudioDescriptionMixCommand.buildFilterGraph(
                listOf(input("d1", 2.5)),
                AudioDescriptionMixSettings(),
            )

        assertTrue(graph.contains("[1:a]volume=-6.00dB,adelay=2500|2500[desc]"), graph)
        assertTrue(graph.contains("[0:a]asplit=2[voice][key]"), graph)
        assertTrue(graph.contains("sidechaincompress=threshold=0.10:ratio=4:attack=5:release=300"), graph)
        assertTrue(graph.contains("[voice][ducked]amix=inputs=2:duration=first:dropout_transition=0,asplit=2[aout][asep]"), graph)
    }

    @Test
    fun `several cues are mixed together before ducking`() {
        val graph =
            AudioDescriptionMixCommand.buildFilterGraph(
                listOf(input("d1", 0.0), input("d2", 3.0)),
                AudioDescriptionMixSettings(),
            )

        assertTrue(graph.contains("[1:a]volume=-6.00dB,adelay=0|0[d0]"), graph)
        assertTrue(graph.contains("[2:a]volume=-6.00dB,adelay=3000|3000[d1]"), graph)
        assertTrue(graph.contains("[d0][d1]amix=inputs=2:duration=longest:dropout_transition=0[desc]"), graph)
    }

    @Test
    fun `without ducking the description is mixed directly`() {
        val graph =
            AudioDescriptionMixCommand.buildFilterGraph(
                listOf(input("d1", 1.0)),
                AudioDescriptionMixSettings(duckingEnabled = false),
            )

        assertTrue(graph.contains("[0:a][desc]amix=inputs=2:duration=first:dropout_transition=0,asplit=2[aout][asep]"), graph)
        assertTrue(!graph.contains("sidechaincompress"), graph)
    }

    @Test
    fun `the level is configurable`() {
        val graph =
            AudioDescriptionMixCommand.buildFilterGraph(
                listOf(input("d1", 0.0)),
                AudioDescriptionMixSettings(levelDb = -12.0),
            )

        assertTrue(graph.contains("volume=-12.00dB"), graph)
    }

    @Test
    fun `the argv writes both the mixed video and the separate track`() {
        val video = File("/tmp/in.webm")
        val out = File("/tmp/out.webm")
        val separate = File("/tmp/out-audiodesc.m4a")

        val argv =
            AudioDescriptionMixCommand.buildArgv(
                videoFile = video,
                inputs = listOf(input("d1", 0.0), input("d2", 2.0)),
                outputVideo = out,
                separateAudio = separate,
                settings = AudioDescriptionMixSettings(),
                audioCodec = "libvorbis",
                ffmpegPath = "ffmpeg",
            )

        assertEquals("ffmpeg", argv.first())
        assertEquals(listOf("-i", video.absolutePath), argv.subList(2, 4))
        assertTrue(argv.contains("[asep]"))
        assertTrue(argv.contains("-vn"))
        assertTrue(argv.contains(out.absolutePath), argv.toString())
        assertTrue(argv.indexOf(out.absolutePath) < argv.indexOf(separate.absolutePath))
        assertEquals(separate.absolutePath, argv.last())
    }

    @Test
    fun `an empty input list is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            AudioDescriptionMixCommand.buildFilterGraph(emptyList(), AudioDescriptionMixSettings())
        }
    }

    @Test
    fun `invalid settings are rejected`() {
        assertFailsWith<IllegalArgumentException> { AudioDescriptionMixSettings(duckRatio = 0) }
        assertFailsWith<IllegalArgumentException> { AudioDescriptionMixSettings(duckThreshold = 0.0) }
        assertFailsWith<IllegalArgumentException> { AudioDescriptionInput(File("/tmp/a.mp3"), -1.0) }
    }
}
