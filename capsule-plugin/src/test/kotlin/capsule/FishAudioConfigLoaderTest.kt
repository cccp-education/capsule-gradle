package capsule

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * CAP-FISH-AUDIO US-1 — loading of the `fish-audio` key pool from the dedicated
 * (git-ignored) `fish.yaml`, through the shared [CapsuleConfigLoader] (Jackson +
 * `${VAR}` substitution). A missing/empty file degrades to an empty pool — the
 * `fish` engine is then unavailable, never a failure.
 */
class FishAudioConfigLoaderTest {

    @field:TempDir
    lateinit var dir: File

    private fun write(name: String, body: String): File =
        File(dir, name).apply { writeText(body) }

    @Test
    fun `loads a single key from the fish-audio pool`() {
        val file = write("fish.yaml", "fish-audio:\n  - key: sk-fish-test\n")
        assertEquals(listOf(FishAudioKey("sk-fish-test")), CapsuleConfigLoader.loadFishAudio(file))
    }

    @Test
    fun `loads multiple keys and preserves pool order`() {
        val file = write("fish.yaml", "fish-audio:\n  - key: k1\n  - key: k2\n")
        assertEquals(
            listOf(FishAudioKey("k1"), FishAudioKey("k2")),
            CapsuleConfigLoader.loadFishAudio(file),
        )
    }

    @Test
    fun `missing file degrades to an empty pool`() {
        assertTrue(CapsuleConfigLoader.loadFishAudio(File(dir, "absent.yaml")).isEmpty())
    }

    @Test
    fun `empty file degrades to an empty pool`() {
        assertTrue(CapsuleConfigLoader.loadFishAudio(write("fish.yaml", "")).isEmpty())
    }

    @Test
    fun `an unknown field on an entry is ignored (forward compatible)`() {
        val file = write("fish.yaml", "fish-audio:\n  - key: k1\n    model: speech-1.6\n")
        assertEquals(listOf(FishAudioKey("k1")), CapsuleConfigLoader.loadFishAudio(file))
    }

    @Test
    fun `the fish-audio member maps into CapsuleConfig`() {
        val file = write("capsule-context.yml", "fish-audio:\n  - key: k1\n")
        assertEquals(listOf(FishAudioKey("k1")), CapsuleConfigLoader.load(file).fishAudio)
    }

    @Test
    fun `the merger carries the fish-audio member (the secret rides the yaml only)`() {
        val yaml = CapsuleConfig(fishAudio = listOf(FishAudioKey("k1")))
        val merged = CapsuleConfigMerger.merge(dir, yaml, emptyMap(), yamlLoaded = true)
        assertEquals(listOf(FishAudioKey("k1")), merged.fishAudio)
    }

    @Test
    fun `the committed example parses to one key`() {
        val example = File("../fish_example.yaml")
        assumeTrue(example.exists(), "fish_example.yaml not present in the working tree")
        val keys = CapsuleConfigLoader.loadFishAudio(example)
        assertEquals(1, keys.size)
        assertTrue(keys.single().key.startsWith("sk-fish-"), "expected the example key: ${keys.single()}")
    }
}
