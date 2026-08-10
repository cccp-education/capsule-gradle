package capsule.context

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import contracts.context.ChannelBudget
import contracts.context.ContextChannel
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD unit tests for [SpdContextLoader] (CAP-SPD-1).
 *
 * The loader is an object pur mirroring [DocContextLoader]: it validates an
 * optional `metadata.json` K-2 envelope (type == "SPD" — the format, not the
 * producer borough: capsule is public OSS and validates only the content type,
 * never the `source` field, to stay format-agnostic and avoid coupling to any
 * specific producer borough), parses the companion AsciiDoc for the
 * pedagogical payload (objectives, duration, prerequisites, modalities,
 * session title, module), and renders a prompt-ready
 * `==== SPD Pedagogical Context (spd)` section truncated to the supplied
 * token budget.
 *
 * Missing files are skipped silently — a null metadata file or a missing
 * AsciiDoc yields an empty string (backward compatible, no error).
 */
class SpdContextLoaderTest {

    @TempDir
    lateinit var tempDir: File

    private val mapper = jacksonObjectMapper()

    @Test
    fun `load with null metadata and missing adoc returns blank string`() {
        val result = SpdContextLoader.load(null, File(tempDir, "missing.adoc"), 400)
        assertTrue(result.isBlank(), "Missing SPD files should yield a blank string")
    }

    @Test
    fun `load with valid metadata and adoc extracts objectives`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val adoc = File(tempDir, "001_bienvenue.adoc").also {
            it.writeText(
                """
                = Bienvenue dans la Formation FPA
                :module: accueil
                :title: Bienvenue dans la Formation FPA

                == Objectifs
                - Comprendre le cadre de la formation
                - Connaître les modalités d'évaluation

                == Prérequis
                - Connaissances de base en programmation

                == Modalités d'évaluation
                QCM final + mise en situation
                """.trimIndent()
            )
        }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.contains("SPD Pedagogical Context"), "Expected SPD section header")
        assertTrue(result.contains("Objectives:"), "Expected objectives label")
        assertTrue(result.contains("Comprendre le cadre de la formation"), "Expected objective content")
    }

    @Test
    fun `load with missing metadata file gracefully skipped and still parses adoc`() {
        val adoc = File(tempDir, "002_objectifs.adoc").also {
            it.writeText(
                """
                = Objectifs Pédagogiques
                :module: accueil

                == Objectifs
                - Analyser un problème algorithmique
                - Implémenter une solution optimisée
                """.trimIndent()
            )
        }
        val result = SpdContextLoader.load(File(tempDir, "missing-metadata.json"), adoc, 400)
        assertTrue(result.isNotBlank(), "Adoc should still be parsed when metadata is missing")
        assertTrue(result.contains("Analyser un problème algorithmique"), "Expected objective from adoc")
    }

    @Test
    fun `load with metadata type not SPD skips the SPD entirely`() {
        val metadata = writeMetadata(type = "SPG", source = "producer", version = "1.0")
        val adoc = File(tempDir, "spg.adoc").also { it.writeText("= SPG\n\n== Objectifs\n- Global") }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.isBlank(), "metadata type != 'SPD' should yield a blank string (graceful skip)")
    }

    @Test
    fun `load with empty objectives section renders empty objectives label`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val adoc = File(tempDir, "minimal.adoc").also {
            it.writeText("= Minimal Session\n:module: core\n\n== Déroulement\nStep by step.")
        }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.contains("Session:"), "Expected session title label even without objectives")
        assertTrue(result.contains("Module: core"), "Expected module from adoc attribute")
    }

    @Test
    fun `load truncates content exceeding the token budget`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val longObjectives = (1..80).joinToString("\n") { "- Objectif $it avec suffisamment de mots pour consommer le budget" }
        val adoc = File(tempDir, "long.adoc").also {
            it.writeText("= Long Session\n:module: core\n\n== Objectifs\n$longObjectives")
        }
        val maxTokens = 50
        val result = SpdContextLoader.load(metadata, adoc, maxTokens)
        val estimated = ContextChannel.estimateTokens(result)
        assertTrue(estimated <= maxTokens + 50, "Result ($estimated tokens) should be truncated near budget ($maxTokens)")
        assertFalse(result.contains("Objectif 80"), "Truncated result should not contain the last objective")
    }

    @Test
    fun `load with malformed metadata json gracefully skipped and still parses adoc`() {
        val metadata = File(tempDir, "bad.json").also { it.writeText("{not valid json") }
        val adoc = File(tempDir, "spd.adoc").also { it.writeText("= SPD\n:module: core\n\n== Objectifs\n- Goal") }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.isNotBlank(), "Malformed metadata.json should be skipped (no throw) but adoc still parsed")
        assertTrue(result.contains("Goal"), "Expected objective from adoc when metadata is malformed")
    }

    @Test
    fun `load extracts multi-line objectives as a list`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val adoc = File(tempDir, "multi.adoc").also {
            it.writeText(
                """
                = Multi Objectives Session
                :module: module-02

                == Objectifs
                - Premier objectif
                - Deuxième objectif
                - Troisième objectif
                """.trimIndent()
            )
        }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.contains("Premier objectif"), "Expected first objective")
        assertTrue(result.contains("Deuxième objectif"), "Expected second objective")
        assertTrue(result.contains("Troisième objectif"), "Expected third objective")
    }

    @Test
    fun `load with zero token budget returns blank string`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val adoc = File(tempDir, "spd.adoc").also { it.writeText("= SPD\n:module: core\n\n== Objectifs\n- Goal") }
        val result = SpdContextLoader.load(metadata, adoc, 0)
        assertTrue(result.isBlank(), "Zero token budget should yield a blank string")
    }

    @Test
    fun `load with empty adoc file returns blank string`() {
        val metadata = writeMetadata(type = "SPD", source = "producer", version = "1.0")
        val adoc = File(tempDir, "empty.adoc").also { it.writeText("") }
        val result = SpdContextLoader.load(metadata, adoc, 400)
        assertTrue(result.isBlank(), "Empty adoc should yield a blank string")
    }

    private fun writeMetadata(type: String, source: String, version: String): File {
        val node = mapper.createObjectNode().apply {
            put("source", source)
            put("type", type)
            put("sessions", 1)
            put("generatedAt", "2026-08-11T10:00:00Z")
            put("model", "convention-over-configuration")
            put("version", version)
            putArray("dependencies").apply {
                add("manhattan")
                add("queens")
            }
        }
        return File(tempDir, "metadata.json").also { mapper.writeValue(it, node) }
    }
}