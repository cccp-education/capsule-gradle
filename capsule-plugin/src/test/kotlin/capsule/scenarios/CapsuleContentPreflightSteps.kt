package capsule.scenarios

import capsule.feed.SlideSegment
import capsule.qc.ContentPreflight
import capsule.qc.PreflightFinding
import capsule.qc.Severity
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_content_preflight.feature` (PRT-05).
 *
 * Exercises [ContentPreflight] directly — no Gradle, no TTS. Step texts are
 * prefixed with "preflight" (S-088 — the glue is classpath-wide).
 */
class CapsuleContentPreflightSteps {

    private val slides = mutableListOf<SlideSegment>()
    private var findings: List<PreflightFinding> = emptyList()

    @Given("a preflight slide {int} with note {string}")
    fun aPreflightSlide(index: Int, note: String) {
        slides.add(SlideSegment(index = index, title = "s$index", speakerNote = note))
    }

    @When("the content preflight runs")
    fun theContentPreflightRuns() {
        findings = ContentPreflight.check(slides.toList())
    }

    @Then("the preflight has no finding")
    fun thePreflightHasNoFinding() {
        assertTrue(findings.isEmpty(), "expected no finding, got $findings")
    }

    @Then("the preflight has {int} finding with code {string} and severity {string}")
    fun thePreflightHasFinding(count: Int, code: String, severity: String) {
        assertEquals(count, findings.size, "unexpected findings: $findings")
        assertEquals(code, findings.single().code)
        assertEquals(Severity.valueOf(severity), findings.single().severity)
    }
}
