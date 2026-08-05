package capsule.scenarios

import capsule.feed.AsciidocSpeakerNoteParser
import capsule.feed.CapsuleScript
import capsule.feed.CapsuleScriptWriter
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions
import java.io.File

class CapsuleSpeakerNotesSteps {

    private var deckName: String = ""
    private var adocContent: String = ""
    private var script: CapsuleScript? = null
    private var rendered: String = ""
    private var caught: Throwable? = null

    @Given("an AsciiDoc deck {string} with content")
    fun anAsciidocDeckWithContent(name: String, content: String) {
        deckName = name
        adocContent = content.trimIndent()
    }

    @Given("the real demo deck {string} is loaded")
    fun theRealDemoDeckIsLoaded(deckBasename: String) {
        deckName = deckBasename
        val repoRoot = resolveRepoRoot()
        val deck = File(repoRoot, "slides/misc/$deckBasename.adoc")
            .takeIf { it.exists() }
            ?: File(repoRoot, "foundry/public/capsule-gradle/slides/misc/$deckBasename.adoc")
                .takeIf { it.exists() }
                ?: error("Demo deck not found: $deckBasename")
        adocContent = deck.readText()
    }

    @Given("a blank deck name")
    fun aBlankDeckName() {
        deckName = "   "
    }

    @When("the capsule script is generated from the deck")
    fun theCapsuleScriptIsGeneratedFromTheDeck() {
        script = AsciidocSpeakerNoteParser.parse(adocContent, deckName.trim().ifBlank { deckName })
    }

    @When("the capsule script generation is attempted")
    fun theCapsuleScriptGenerationIsAttempted() {
        try {
            script = AsciidocSpeakerNoteParser.parse(adocContent, deckName)
        } catch (t: Throwable) {
            caught = t
        }
    }

    @When("the script is rendered as plain text")
    fun theScriptIsRenderedAsPlainText() {
        rendered = script?.let { CapsuleScriptWriter.write(it) } ?: ""
    }

    @Then("the script deck name should be {string}")
    fun theScriptDeckNameShouldBe(expected: String) {
        Assertions.assertEquals(expected, script?.deckName)
    }

    @And("the script should contain {int} segment(s)")
    fun theScriptShouldContainNSegments(count: Int) {
        Assertions.assertEquals(count, script?.segments?.size)
    }

    @And("the segment {int} should have title {string}")
    fun theSegmentShouldHaveTitle(idx: Int, title: String) {
        Assertions.assertEquals(title, script?.segments?.get(idx - 1)?.title)
    }

    @And("the segment {int} should have speakerNote {string}")
    fun theSegmentShouldHaveSpeakerNote(idx: Int, note: String) {
        Assertions.assertEquals(note, script?.segments?.get(idx - 1)?.speakerNote)
    }

    @And("the segment {int} should have index {int}")
    fun theSegmentShouldHaveIndex(idx: Int, expected: Int) {
        Assertions.assertEquals(expected, script?.segments?.get(idx - 1)?.index)
    }

    @Then("the script should be empty")
    fun theScriptShouldBeEmpty() {
        Assertions.assertTrue(script?.isEmpty == true)
    }

    @Then("the first line should be {string}")
    fun theFirstLineShouldBe(expected: String) {
        Assertions.assertEquals(expected, rendered.lines().firstOrNull())
    }

    @And("the second line should be blank")
    fun theSecondLineShouldBeBlank() {
        val lines = rendered.lines()
        Assertions.assertEquals("", lines.getOrNull(1))
    }

    @And("the text should contain {string}")
    fun theTextShouldContain(expected: String) {
        Assertions.assertTrue(rendered.contains(expected))
    }

    @Then("the generation should fail with a message containing {string}")
    fun theGenerationShouldFailWithAMessageContaining(fragment: String) {
        Assertions.assertNotNull(caught) { "Expected a failure containing '$fragment'" }
        Assertions.assertTrue(
            caught!!.message?.contains(fragment) == true,
            "Expected failure message to contain '$fragment' but was: ${caught!!.message}",
        )
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