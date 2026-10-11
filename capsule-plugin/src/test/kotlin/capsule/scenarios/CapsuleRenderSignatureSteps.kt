package capsule.scenarios

import capsule.RenderSignature
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_render_signature.feature` (PRT-06).
 *
 * Exercises [RenderSignature] directly — no Gradle, no capture. Step texts are
 * prefixed with "render signature" (S-088 — the glue is classpath-wide).
 */
class CapsuleRenderSignatureSteps {

    private var first: String = ""
    private var second: String = ""

    @Given("a render signature built from engine {string} content {string} and timeline {string}")
    fun aRenderSignatureBuiltFrom(engine: String, content: String, timeline: String) {
        val signature = RenderSignature.of(engine, content, timeline)
        if (first.isEmpty()) first = signature else second = signature
    }

    @Then("the two render signatures are equal")
    fun theTwoRenderSignaturesAreEqual() {
        assertEquals(first, second)
    }

    @Then("the two render signatures differ")
    fun theTwoRenderSignaturesDiffer() {
        assertNotEquals(first, second)
    }

    @Then("the render signature is {int} lowercase hexadecimal characters")
    fun theRenderSignatureIsHex(length: Int) {
        val signature = if (second.isNotEmpty()) second else first
        assertEquals(length, signature.length)
        assertTrue(signature.matches(Regex("[0-9a-f]+")), "expected lowercase hex, got $signature")
    }
}
