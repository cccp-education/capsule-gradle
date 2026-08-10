package capsule.scenarios

import capsule.feed.CapsuleFeedTaskNames
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions

/**
 * Cucumber steps for the `generateCapsuleContentAndVideos` composite task
 * (CAP-ARCH-4).
 *
 * Validates the task name, description, and group declared in
 * [CapsuleFeedTaskNames] without running Gradle — pure constant assertions
 * (pattern `TranslateAndExtractSpeakerNotesSteps`).
 *
 * Step names are scoped with "content and videos" to avoid duplicate
 * definitions with [TranslateAndExtractSpeakerNotesSteps].
 */
class CapsuleContentAndVideosSteps {

    private var taskName: String = ""
    private var taskDescription: String = ""
    private var taskGroup: String = ""

    @When("the generateCapsuleContentAndVideos task name is read")
    fun theGenerateCapsuleContentAndVideosTaskNameIsRead() {
        taskName = CapsuleFeedTaskNames.GENERATE_CAPSULE_CONTENT_AND_VIDEOS
    }

    @When("the generateCapsuleContentAndVideos task description is read")
    fun theGenerateCapsuleContentAndVideosTaskDescriptionIsRead() {
        taskDescription = CapsuleFeedTaskNames.CONTENT_AND_VIDEOS_DESCRIPTION
    }

    @When("the generateCapsuleContentAndVideos task group is read")
    fun theGenerateCapsuleContentAndVideosTaskGroupIsRead() {
        taskGroup = CapsuleFeedTaskNames.GROUP
    }

    @Then("the content and videos task name should be {string}")
    fun theContentAndVideosTaskNameShouldBe(expected: String) {
        Assertions.assertEquals(expected, taskName)
    }

    @Then("the content and videos description should mention {string}")
    fun theContentAndVideosDescriptionShouldMention(expected: String) {
        Assertions.assertTrue(taskDescription.contains(expected))
    }

    @Then("the content and videos task group should be {string}")
    fun theContentAndVideosTaskGroupShouldBe(expected: String) {
        Assertions.assertEquals(expected, taskGroup)
    }
}