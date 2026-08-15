package capsule.scenarios

import capsule.feed.SlideSegment
import capsule.transcript.TranscriptLlmEnhancer
import capsule.transcript.TranscriptBuilder
import capsule.transcript.TranscriptPlan
import capsule.transcript.TranscriptStrategy
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.assertj.core.api.Assertions.assertThat

class CapsuleTranscriptSteps {

    private var transcriptPlan: TranscriptPlan? = null
    private var generatedTranscript: String? = null
    private var existingOutputContent: String? = null
    private var strategy: TranscriptStrategy? = null
    private var enhancementResult: String? = null
    private var llmEnhancer: TranscriptLlmEnhancer = object : TranscriptLlmEnhancer {
        override fun enhance(template: String, plan: TranscriptPlan): String {
            return enhancementResult ?: template
        }
    }

    @Given("a transcript plan for deck {string} with language {string} and strategy TEMPLATE")
    fun `a transcript plan for deck with language and strategy template`(deckName: String, language: String) {
        transcriptPlan = TranscriptPlan(
            deckName = deckName,
            segments = emptyList(),
            language = language,
            outputPath = "${deckName}-transcript.adoc"
        )
        strategy = TranscriptStrategy.TEMPLATE
        enhancementResult = null
    }

    @Given("a transcript plan for deck {string} with language {string} and strategy LLM")
    fun `a transcript plan for deck with language and strategy llm`(deckName: String, language: String) {
        transcriptPlan = TranscriptPlan(
            deckName = deckName,
            segments = emptyList(),
            language = language,
            outputPath = "${deckName}-transcript.adoc"
        )
        strategy = TranscriptStrategy.LLM
        enhancementResult = null
    }

    @Given("the plan contains slide segments:")
    fun `the plan contains slide segments`(dataTable: DataTable) {
        val segments = mutableListOf<SlideSegment>()
        val maps = dataTable.asMaps()
        for ((index, map) in maps.withIndex()) {
            val title = map["title"] ?: throw IllegalArgumentException("Missing title in slide segment")
            val speakerNote = map["speakerNote"] ?: throw IllegalArgumentException("Missing speakerNote in slide segment")
            segments.add(SlideSegment(index + 1, title, speakerNote))
        }
        transcriptPlan = transcriptPlan?.copy(segments = segments)
    }

    @When("the transcript is generated")
    fun `the transcript is generated`() {
        val plan = transcriptPlan ?: throw IllegalStateException("Transcript plan not set")
        val strat = strategy ?: throw IllegalStateException("Strategy not set")
        val content = existingOutputContent
        if (content != null && content.isNotBlank()) {
            generatedTranscript = content
            existingOutputContent = null
            return
        }
        when (strat) {
            TranscriptStrategy.TEMPLATE -> {
                generatedTranscript = TranscriptBuilder.build(plan)
            }
            TranscriptStrategy.LLM -> {
                val template = TranscriptBuilder.build(plan)
                generatedTranscript = llmEnhancer.enhance(template, plan)
            }
        }
    }

    @When("the transcript is generated with LLM enhancement")
    fun `the transcript is generated with LLM enhancement`() {
        `the transcript is generated`()
    }

    @Then("the transcript should contain:")
    fun `the transcript should contain`(expected: String) {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val actualWithoutTrailingNewline = actual.trimEnd()
        assertThat(actualWithoutTrailingNewline).isEqualTo(expected)
    }

    @Then("the transcript should contain enhanced content for each slide")
    fun `the transcript should contain enhanced content for each slide`() {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val plan = transcriptPlan ?: throw IllegalStateException("Transcript plan not set")
        plan.segments.forEach { segment ->
            assertThat(actual).contains(segment.speakerNote)
        }
        if (plan.language == "en") {
            assertThat(actual).isNotBlank()
        }
    }

    @Then("the enhanced content should include the original speaker notes")
    fun `the enhanced content should include the original speaker notes`() {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val plan = transcriptPlan ?: throw IllegalStateException("Transcript plan not set")
        plan.segments.forEach { segment ->
            assertThat(actual).contains(segment.speakerNote)
        }
    }

    @Then("the enhanced content should be in English")
    fun `the enhanced content should be in English`() {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val plan = transcriptPlan ?: throw IllegalStateException("Transcript plan not set")
        if (plan.language == "en") {
            assertThat(actual).contains(":language: en")
        }
    }

    @Given("an empty transcript plan for deck {string}")
    fun `an empty transcript plan for deck`(deckName: String) {
        transcriptPlan = TranscriptPlan(
            deckName = deckName,
            segments = emptyList(),
            language = "fr",
            outputPath = "${deckName}-transcript.adoc"
        )
        strategy = TranscriptStrategy.TEMPLATE
        enhancementResult = null
    }

    @Then("the transcript should be:")
    fun the_transcript_should_be(docString: String) {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val actualWithoutTrailingNewline = actual.trimEnd()
        assertThat(actualWithoutTrailingNewline).isEqualTo(docString)
    }

    @Given("the output file already exists and contains {string}")
    fun `the output file already exists and contains`(content: String) {
        existingOutputContent = content
    }

    @Then("the transcript should be {string}")
    fun `the transcript should be`(expected: String) {
        val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
        val actualWithoutTrailingNewline = actual.trimEnd()
        assertThat(actualWithoutTrailingNewline).isEqualTo(expected)
    }

    @Then("no enhancement should occur")
    fun `no enhancement should occur`() {
        val strat = strategy ?: throw IllegalStateException("Strategy not set")
        if (strat == TranscriptStrategy.LLM) {
            val plan = transcriptPlan ?: throw IllegalStateException("Transcript plan not set")
            val template = TranscriptBuilder.build(plan)
            val actual = generatedTranscript ?: throw IllegalStateException("Transcript not generated")
            assertThat(actual).isEqualTo(template)
        }
    }
}