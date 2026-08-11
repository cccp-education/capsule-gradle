package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * TDD unit tests for [ScreenshotPlanner] (CAP-CR3-3 US-3).
 *
 * [ScreenshotPlanner] is a pure object that produces a
 * [ScreenshotCapturePlan] from slide durations — no I/O, fully testable.
 */
class ScreenshotPlannerTest {

    private val outputDir = File("/tmp/capsule-test")

    @Test
    fun `plan single slide produces one entry`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(5.0))
        assertEquals(1, plan.size)
        assertEquals(0, plan.slides[0].index)
        assertEquals(File("/tmp/capsule-test/slide-0.png"), plan.slides[0].pngFile)
        assertEquals(File("/tmp/capsule-test/slide-0.webm"), plan.slides[0].webmFile)
        assertEquals(5.0, plan.slides[0].durationSecs)
    }

    @Test
    fun `plan multi slide produces N entries in order`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(3.0, 7.0, 2.5))
        assertEquals(3, plan.size)
        assertEquals(0, plan.slides[0].index)
        assertEquals(3.0, plan.slides[0].durationSecs)
        assertEquals(1, plan.slides[1].index)
        assertEquals(7.0, plan.slides[1].durationSecs)
        assertEquals(2, plan.slides[2].index)
        assertEquals(2.5, plan.slides[2].durationSecs)
    }

    @Test
    fun `plan concat list and final webm paths are under outputDir`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(5.0, 5.0))
        assertEquals(File("/tmp/capsule-test/concat-list.txt"), plan.concatListFile)
        assertEquals(File("/tmp/capsule-test/capsule.webm"), plan.finalWebm)
    }

    @Test
    fun `plan empty slide durations throws`() {
        assertFailsWith<IllegalArgumentException> {
            ScreenshotPlanner.plan(outputDir, emptyList())
        }
    }

    @Test
    fun `renderConcatList produces one file line per slide in order`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(3.0, 7.0, 2.5))
        val concat = ScreenshotPlanner.renderConcatList(plan)
        assertEquals("file 'slide-0.webm'\nfile 'slide-1.webm'\nfile 'slide-2.webm'\n", concat)
    }

    @Test
    fun `ffmpegPngToWebmArgs builds correct argv with duration and scale`() {
        val entry = ScreenshotSlideEntry(1, File("/tmp/slide-1.png"), File("/tmp/slide-1.webm"), 7.0)
        val argv = ScreenshotPlanner.ffmpegPngToWebmArgs(entry, viewportWidth = 1408, viewportHeight = 792)
        assertEquals("ffmpeg", argv[0])
        assertTrue(argv.contains("-t"))
        val tIdx = argv.indexOf("-t")
        assertEquals("7.0", argv[tIdx + 1], "duration should match slide duration")
        val vfIdx = argv.indexOf("-vf")
        assertEquals("scale=1408:792", argv[vfIdx + 1])
        assertEquals("/tmp/slide-1.webm", argv.last(), "output webm path should be last")
    }

    @Test
    fun `ffmpegConcatArgs builds concat demuxer argv`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(3.0, 7.0))
        val argv = ScreenshotPlanner.ffmpegConcatArgs(plan)
        assertEquals("ffmpeg", argv[0])
        assertTrue(argv.contains("-f"))
        val fIdx = argv.indexOf("-f")
        assertEquals("concat", argv[fIdx + 1])
        assertTrue(argv.contains("-safe"))
        val safeIdx = argv.indexOf("-safe")
        assertEquals("0", argv[safeIdx + 1])
        assertEquals("/tmp/capsule-test/capsule.webm", argv.last(), "final webm should be last")
    }

    @Test
    fun `plan isEmpty is false for non-empty durations`() {
        val plan = ScreenshotPlanner.plan(outputDir, listOf(5.0))
        assertTrue(!plan.isEmpty)
    }
}