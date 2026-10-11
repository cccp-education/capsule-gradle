package capsule.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * TDD unit tests for [LoudnormAnalysisParser] (PRT-09).
 *
 * The parser is pure — it reads the five numbers straight out of ffmpeg's
 * merged output, so it is testable without spawning ffmpeg.
 */
class LoudnormAnalysisParserTest {

    private val sample = """
        [Parsed_loudnorm_0 @ 0x55] 
        {
            "input_i" : "-23.03",
            "input_tp" : "-2.19",
            "input_lra" : "4.30",
            "input_thresh" : "-33.31",
            "output_i" : "-16.00",
            "target_offset" : "0.21"
        }
    """.trimIndent()

    @Test
    fun `parses the five measured fields`() {
        val m = LoudnormAnalysisParser.parse(sample)!!
        assertEquals(-23.03, m.inputI, 1e-9)
        assertEquals(-2.19, m.inputTp, 1e-9)
        assertEquals(4.30, m.inputLra, 1e-9)
        assertEquals(-33.31, m.inputThresh, 1e-9)
        assertEquals(0.21, m.targetOffset, 1e-9)
    }

    @Test
    fun `returns null without the loudnorm block`() {
        assertNull(LoudnormAnalysisParser.parse("pas de json ici"))
    }

    @Test
    fun `returns null when a field is missing`() {
        assertNull(LoudnormAnalysisParser.parse("""{"input_i":"-23.03","input_tp":"-2.19"}"""))
    }

    @Test
    fun `parses unquoted numbers too`() {
        val unquoted = """{"input_i":-23.03,"input_tp":-2.19,"input_lra":4.3,"input_thresh":-33.31,"target_offset":0.21}"""
        assertEquals(-23.03, LoudnormAnalysisParser.parse(unquoted)!!.inputI, 1e-9)
    }
}
