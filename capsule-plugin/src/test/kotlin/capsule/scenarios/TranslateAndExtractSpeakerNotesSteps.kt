package capsule.scenarios

import capsule.feed.CapsuleFeedTaskNames
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions

class TranslateAndExtractSpeakerNotesSteps {

    private var taskName: String = ""
    private var taskDescription: String = ""
    private var taskGroup: String = ""

    @When("the translateAndExtractSpeakerNotes task name is read")
    fun theTranslateAndExtractSpeakerNotesTaskNameIsRead() {
        taskName = CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_SPEAKER_NOTES
    }

    @When("the translateAndExtractSpeakerNotes task description is read")
    fun theTranslateAndExtractSpeakerNotesTaskDescriptionIsRead() {
        taskDescription = CapsuleFeedTaskNames.TRANSLATE_AND_EXTRACT_DESCRIPTION
    }

    @When("the translateAndExtractSpeakerNotes task group is read")
    fun theTranslateAndExtractSpeakerNotesTaskGroupIsRead() {
        taskGroup = CapsuleFeedTaskNames.GROUP
    }

    @Then("the task name should be {string}")
    fun theTaskNameShouldBe(expected: String) {
        Assertions.assertEquals(expected, taskName)
    }

    @Then("the description should mention {string}")
    fun theDescriptionShouldMention(expected: String) {
        Assertions.assertTrue(taskDescription.contains(expected))
    }

    @Then("the task group should be {string}")
    fun theTaskGroupShouldBe(expected: String) {
        Assertions.assertEquals(expected, taskGroup)
    }
}