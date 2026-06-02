package capsule

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ManimEngineTest {

    // ─── NoOp engine ─────────────────────────────────────────────

    @Test
    fun `noop engine is available and named noop`() {
        val engine = NoOpManimEngine()
        assertTrue(engine.isAvailable())
        assertEquals("noop", engine.name())
    }

    @Test
    fun `noop engine render creates placeholder file`() {
        val engine = NoOpManimEngine()
        val tmpDir = File.createTempFile("manim-test", "")
        tmpDir.delete()
        tmpDir.mkdirs()
        tmpDir.deleteOnExit()

        val scriptFile = File.createTempFile("manim-scene", ".py", tmpDir)
        scriptFile.deleteOnExit()

        val outputDir = tmpDir.resolve("media")
        val result = engine.render("MyScene", scriptFile, outputDir)

        assertTrue(result.exists())
        assertTrue(result.readText().contains("MANIM PLACEHOLDER"))
    }

    @Test
    fun `noop engine probeDuration returns zero`() {
        val engine = NoOpManimEngine()
        val dummyFile = File.createTempFile("video", ".mp4")
        dummyFile.deleteOnExit()
        assertEquals(0.0, engine.probeDuration(dummyFile))
    }

    // ─── ManimEngineImpl (sans vrai manim) ──────────────────────

    @Test
    fun `manim engine reports unavailable when manim not installed`() {
        val engine = ManimEngineImpl(executablePath = "/nonexistent/path/manim")
        assertEquals(false, engine.isAvailable())
    }

    @Test
    fun `manim engine throws ManimException when not available and render called`() {
        val engine = ManimEngineImpl(executablePath = "/nonexistent/path/manim")
        val tmpFile = File.createTempFile("capsule-test", ".py")
        tmpFile.deleteOnExit()

        assertFailsWith<ManimException> {
            engine.render("MyScene", tmpFile, tmpFile.parentFile)
        }
    }

    @Test
    fun `manim engine name returns manim`() {
        val engine = ManimEngineImpl()
        assertEquals("manim", engine.name())
    }
}
