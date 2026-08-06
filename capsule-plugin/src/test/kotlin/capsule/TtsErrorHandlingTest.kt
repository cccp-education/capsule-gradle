package capsule

import capsule.feed.CapsuleScript
import capsule.feed.SlideSegment
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TtsErrorHandlingTest {

    @org.junit.jupiter.api.io.TempDir
    lateinit var tempDir: File

    private fun newTask(): CapsuleVideoTask {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val ext = CapsuleExtension(project.objects)
        ext.ttsEngine.set("noop")
        ext.outputDir.set("capsule")
        val task = project.tasks.register("generateCapsuleVideo", CapsuleVideoTask::class.java).get()
        task.capsuleExtension = ext
        return task
    }

    private fun script(vararg titles: String): CapsuleScript = CapsuleScript(
        deckName = "test",
        segments = titles.mapIndexed { i, t -> SlideSegment(i + 1, t, "$t note.") }
    )

    @Test
    fun `synthesizeTtsForScript returns failed slide indices when TtsException thrown`() {
        val task = newTask()
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val parsed = script("OK", "Fail", "AlsoOK")
        val failingEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                if (text.contains("Fail")) throw TtsException("Simulated failure")
                outputFile.parentFile.mkdirs()
                outputFile.writeText("# $text")
            }
            override fun isAvailable() = true
            override fun name() = "failing"
        }

        val failed = task.synthesizeTtsForScript(parsed, audioDir, failingEngine)

        assertEquals(listOf(2), failed, "Failed slide index 2 should be reported")
    }

    @Test
    fun `synthesizeTtsForScript retries once before skipping a failing slide`() {
        val task = newTask()
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val parsed = script("Fail")
        val attempts = AtomicInteger(0)
        val failingEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                attempts.incrementAndGet()
                throw TtsException("Always fails")
            }
            override fun isAvailable() = true
            override fun name() = "failing"
        }

        val failed = task.synthesizeTtsForScript(parsed, audioDir, failingEngine)

        assertEquals(2, attempts.get(), "Failing slide should be attempted twice (1 retry)")
        assertEquals(listOf(1), failed, "Slide should be reported as failed after retry")
    }

    @Test
    fun `synthesizeTtsForScript succeeds on retry when first attempt fails`() {
        val task = newTask()
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val parsed = script("Flaky")
        val attempts = AtomicInteger(0)
        val flakyEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                if (attempts.incrementAndGet() == 1) {
                    throw TtsException("Transient failure")
                }
                outputFile.parentFile.mkdirs()
                outputFile.writeText("# $text")
            }
            override fun isAvailable() = true
            override fun name() = "flaky"
        }

        val failed = task.synthesizeTtsForScript(parsed, audioDir, flakyEngine)

        assertEquals(emptyList(), failed, "No failed slides after successful retry")
        assertTrue(audioDir.resolve("slide-01.mp3").exists(), "MP3 should exist after retry success")
    }

    @Test
    fun `synthesizeTtsForScript returns empty list when all slides succeed`() {
        val task = newTask()
        val audioDir = File(tempDir, "audio").also { it.mkdirs() }
        val parsed = script("A", "B")
        val okEngine = object : TtsEngine {
            override fun synthesize(text: String, outputFile: File) {
                outputFile.parentFile.mkdirs()
                outputFile.writeText("# $text")
            }
            override fun isAvailable() = true
            override fun name() = "ok"
        }

        val failed = task.synthesizeTtsForScript(parsed, audioDir, okEngine)

        assertEquals(emptyList(), failed, "No failed slides when all succeed")
    }
}