package capsule.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Dogfooding test — validates the 3 demo decks (fr/en/ar) that ship with the
 * capsule-gradle repo. Each deck exercises the full capsule feed pipeline
 * (parse -> write) and proves the contract is language-agnostic.
 *
 * Decks live in `<repo-root>/slides/misc/capsule-feed-demo-{fr,en,ar}-deck.adoc`.
 */
class CapsuleFeedDemoDeckTest {

    @Test
    fun `fr demo deck should parse and render 4 segments`() {
        val deck = deck("capsule-feed-demo-fr-deck")
        val script = AsciidocSpeakerNoteParser.parse(deck.readText(), "capsule-feed-demo-fr-deck")

        assertThat(script.segments).hasSize(4)
        assertThat(script.segments.map { it.title }).containsExactly(
            "Introduction", "Le contrat capsule-gradle", "Validation round-trip", "Conclusion",
        )
        assertThat(script.segments.map { it.index }).containsExactly(1, 2, 3, 4)

        val rendered = CapsuleScriptWriter.write(script)
        assertThat(rendered).startsWith("=== CAPSULE SCRIPT : capsule-feed-demo-fr-deck ===")
        assertThat(rendered).contains("--- SLIDE 1 : Introduction ---")
        assertThat(rendered).contains("--- SLIDE 4 : Conclusion ---")
    }

    @Test
    fun `en demo deck should parse and render 4 segments`() {
        val deck = deck("capsule-feed-demo-en-deck")
        val script = AsciidocSpeakerNoteParser.parse(deck.readText(), "capsule-feed-demo-en-deck")

        assertThat(script.segments).hasSize(4)
        assertThat(script.segments.map { it.title }).containsExactly(
            "Introduction", "The capsule-gradle contract", "Round-trip validation", "Conclusion",
        )
        assertThat(script.segments.map { it.index }).containsExactly(1, 2, 3, 4)

        val rendered = CapsuleScriptWriter.write(script)
        assertThat(rendered).startsWith("=== CAPSULE SCRIPT : capsule-feed-demo-en-deck ===")
        assertThat(rendered).contains("--- SLIDE 1 : Introduction ---")
        assertThat(rendered).contains("--- SLIDE 4 : Conclusion ---")
    }

    @Test
    fun `ar demo deck should parse and render 4 segments with Arabic speaker notes`() {
        val deck = deck("capsule-feed-demo-ar-deck")
        val script = AsciidocSpeakerNoteParser.parse(deck.readText(), "capsule-feed-demo-ar-deck")

        assertThat(script.segments).hasSize(4)
        assertThat(script.segments.map { it.index }).containsExactly(1, 2, 3, 4)

        val rendered = CapsuleScriptWriter.write(script)
        assertThat(rendered).startsWith("=== CAPSULE SCRIPT : capsule-feed-demo-ar-deck ===")
        assertThat(rendered).contains("--- SLIDE 1 : ")
        assertThat(rendered).contains("--- SLIDE 4 : ")
    }

    @Test
    fun `all 3 demo decks should produce a valid capsule script with identical structure`() {
        val codes = listOf("fr", "en", "ar")
        codes.forEach { code ->
            val deckName = "capsule-feed-demo-$code-deck"
            val deck = deck(deckName)
            val script = AsciidocSpeakerNoteParser.parse(deck.readText(), deckName)

            assertThat(script.segments)
                .withFailMessage("Deck $deckName should have 4 segments")
                .hasSize(4)
            assertThat(script.isEmpty).isFalse()

            val rendered = CapsuleScriptWriter.write(script)
            val lines = rendered.lines()
            assertThat(lines[0]).startsWith("=== CAPSULE SCRIPT : $deckName ===")
            assertThat(lines[1]).isEqualTo("")
            assertThat(lines.count { it.startsWith("--- SLIDE") }).isEqualTo(4)
        }
    }

    private fun deck(name: String): File {
        val repoRoot = resolveRepoRoot()
        return File(repoRoot, "slides/misc/$name.adoc")
            .takeIf { it.exists() }
            ?: File(repoRoot, "foundry/public/capsule-gradle/slides/misc/$name.adoc")
                .takeIf { it.exists() }
            ?: error("Demo deck not found: $name")
    }

    private fun resolveRepoRoot(): File {
        var dir = File(".").absoluteFile
        while (dir != null && dir.parentFile != null) {
            if (File(dir, "slides/misc/capsule-feed-demo-fr-deck.adoc").exists()) return dir
            if (File(dir, "foundry/public/capsule-gradle/slides/misc/capsule-feed-demo-fr-deck.adoc").exists()) return dir
            dir = dir.parentFile
        }
        return File(".").absoluteFile
    }
}