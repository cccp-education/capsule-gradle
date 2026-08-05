package capsule.feed

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CapsuleFeedTaskDomainTest {

    // ---------------------------------------------------------------------------
    // CapsuleAdocDir — resolves <projectDir>/slides/misc
    // ---------------------------------------------------------------------------

    @Test
    fun `a capsule adoc dir resolves project_slides_misc`(@TempDir projectDir: File) {
        val dir = CapsuleAdocDir(projectDir)
        assertEquals(File(projectDir, "slides/misc"), dir.asFile())
    }

    @Test
    fun `a capsule adoc dir rejects a blank project dir`() {
        assertFailsWith<IllegalArgumentException> { CapsuleAdocDir(File("")) }
    }

    @Test
    fun `a capsule adoc dir lists only adoc files sorted by name`(@TempDir projectDir: File) {
        val miscDir = File(projectDir, "slides/misc").apply { mkdirs() }
        File(miscDir, "deck-b.adoc").writeText("= B")
        File(miscDir, "deck-a.adoc").writeText("= A")
        File(miscDir, "ignored.txt").writeText("skip")

        val adocFiles = CapsuleAdocDir(projectDir).adocFiles()

        assertEquals(listOf("deck-a.adoc", "deck-b.adoc"), adocFiles.map { it.name })
    }

    @Test
    fun `a capsule adoc dir returns empty list when misc is missing`(@TempDir projectDir: File) {
        assertTrue(CapsuleAdocDir(projectDir).adocFiles().isEmpty())
    }

    // ---------------------------------------------------------------------------
    // CapsuleScriptDir — resolves <buildDir>/capsule
    // ---------------------------------------------------------------------------

    @Test
    fun `a capsule script dir resolves build_capsule`(@TempDir buildDir: File) {
        val dir = CapsuleScriptDir(buildDir)
        assertEquals(File(buildDir, "capsule"), dir.asFile())
    }

    @Test
    fun `a capsule script dir rejects a blank build dir`() {
        assertFailsWith<IllegalArgumentException> { CapsuleScriptDir(File("")) }
    }

    @Test
    fun `a capsule script dir ensureCreated creates the directory`(@TempDir buildDir: File) {
        val dir = CapsuleScriptDir(buildDir)
        assertFalse(File(buildDir, "capsule").exists())

        dir.ensureCreated()

        assertTrue(File(buildDir, "capsule").isDirectory())
    }

    @Test
    fun `a capsule script dir ensureCreated is idempotent`(@TempDir buildDir: File) {
        val dir = CapsuleScriptDir(buildDir)
        dir.ensureCreated()
        // Second call must not throw
        dir.ensureCreated()
        assertTrue(File(buildDir, "capsule").isDirectory())
    }

    @Test
    fun `a capsule script dir scriptFile resolves name_without_extension-script_txt`(@TempDir buildDir: File) {
        val dir = CapsuleScriptDir(buildDir)
        val scriptFile = dir.scriptFileFor("kotlin-intro")
        assertEquals(File(buildDir, "capsule/kotlin-intro-script.txt"), scriptFile)
    }

    // ---------------------------------------------------------------------------
    // CapsuleFeedTaskNames — stable task identifiers
    // ---------------------------------------------------------------------------

    @Test
    fun `the extract speaker notes task name is extractSpeakerNotes`() {
        assertEquals("extractSpeakerNotes", CapsuleFeedTaskNames.EXTRACT_SPEAKER_NOTES)
    }

    @Test
    fun `the capsule feed task group is capsule`() {
        assertEquals("capsule", CapsuleFeedTaskNames.GROUP)
    }

    @Test
    fun `the extract speaker notes task description mentions speaker notes`() {
        assertTrue(CapsuleFeedTaskNames.DESCRIPTION.contains("speaker notes"))
    }

    @Test
    fun `the extract speaker notes task description mentions replacing slider generateCapsule`() {
        assertTrue(CapsuleFeedTaskNames.DESCRIPTION.contains("generateCapsule"))
    }

    @Test
    fun `the translate and extract speaker notes task name is translateAndExtractSpeakerNotes`() {
        assertEquals("translateAndExtractSpeakerNotes", CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES)
    }

    @Test
    fun `the translate and extract description mentions translateDeck and extractSpeakerNotes`() {
        assertTrue(CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_DESCRIPTION.contains("translateDeck"))
        assertTrue(CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_DESCRIPTION.contains("extractSpeakerNotes"))
    }
}