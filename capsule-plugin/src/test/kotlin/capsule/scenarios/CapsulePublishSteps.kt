package capsule.scenarios

import capsule.adapter.youtube.CapsuleYouTubePublisherResolver
import capsule.adapter.youtube.ChapterLineParser
import capsule.adapter.youtube.HttpTransport
import capsule.adapter.youtube.PublishHttpRequest
import capsule.adapter.youtube.PublishHttpResponse
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import video.kernel.publish.VideoPublication
import video.kernel.publish.VideoPublicationResult
import video.kernel.publish.VideoPublisher

/**
 * HF-YOUTUBE-PUBLISH US-3b — steps for the capsule YouTube publication flow.
 *
 * Prefixed (`capsule publish ...`) to avoid glue collisions (pattern S-088).
 * The fake transport never touches the network; the credentials file is a temp
 * `youtube.yaml` shaped fixture (never a real secret).
 */
class CapsulePublishSteps {

    private class RecordingTransport(private val responses: MutableList<PublishHttpResponse>) : HttpTransport {
        val requests = mutableListOf<PublishHttpRequest>()
        override fun send(request: PublishHttpRequest): PublishHttpResponse {
            requests += request
            return if (responses.isEmpty()) PublishHttpResponse(500, "") else responses.removeAt(0)
        }
    }

    private var transport: RecordingTransport = RecordingTransport(mutableListOf())
    private var enabled: Boolean = true
    private var platform: String = CapsuleYouTubePublisherResolver.YOUTUBE_PLATFORM
    private var credentialsFile: File? = null
    private lateinit var publisher: VideoPublisher
    private var result: VideoPublicationResult? = null

    @Given("a capsule publish fake transport returning a token and a session and the video id {string}")
    fun transportHappyPath(videoId: String) {
        transport = RecordingTransport(
            mutableListOf(
                PublishHttpResponse(200, """{"access_token":"AT"}"""),
                PublishHttpResponse(200, "", mapOf("location" to "https://session")),
                PublishHttpResponse(200, """{"id":"$videoId"}"""),
            ),
        )
    }

    @Given("a capsule publish fake transport refusing the token")
    fun transportRefusingToken() {
        transport = RecordingTransport(mutableListOf(PublishHttpResponse(401, "bad")))
    }

    @Given("a capsule publish credentials file with a refresh token")
    fun credentialsWithRefresh() {
        credentialsFile = tempCredentials(refreshToken = "refresh")
    }

    @Given("a capsule publish credentials file without a refresh token")
    fun credentialsWithoutRefresh() {
        credentialsFile = tempCredentials(refreshToken = "")
    }

    @Given("capsule publishing is disabled")
    fun publishingDisabled() {
        enabled = false
    }

    @Given("the capsule publish platform is {string}")
    fun platformIs(value: String) {
        platform = value
    }

    @When("I publish a capsule video from a temp file")
    fun publishVideo() {
        val video = File.createTempFile("capsule-publish-", ".mp4").apply { writeText("video-bytes") }
        try {
            val file = credentialsFile ?: tempCredentials(refreshToken = "refresh").also { credentialsFile = it }
            publisher = CapsuleYouTubePublisherResolver.resolve(
                enabled = enabled,
                platform = platform,
                credentialsFile = file,
                transport = transport,
            )
            val publication = VideoPublication(
                videoFile = video,
                title = "A title",
                description = "Body",
                chapters = ChapterLineParser.parse(listOf("0:00 Intro", "0:10 Setup", "0:20 Outro")),
            )
            result = publisher.publish(publication)
        } finally {
            video.delete()
        }
    }

    @Then("the capsule publish result is published")
    fun resultPublished() {
        check(result is VideoPublicationResult.Published) { "expected published, was $result" }
    }

    @Then("the capsule publish result is degraded")
    fun resultDegraded() {
        check(result is VideoPublicationResult.Degraded) { "expected degraded, was $result" }
    }

    @Then("the capsule publish result is failed")
    fun resultFailed() {
        check(result is VideoPublicationResult.Failed) { "expected failed, was $result" }
    }

    @Then("the capsule publish url is {string}")
    fun urlIs(expected: String) {
        val published = result as VideoPublicationResult.Published
        check(published.url == expected) { "expected '$expected', was '${published.url}'" }
    }

    @Then("the capsule publish transport received no request")
    fun noRequest() {
        check(transport.requests.isEmpty()) { "expected no request, was ${transport.requests.size}" }
    }

    @After
    fun cleanup() {
        credentialsFile?.delete()
        credentialsFile = null
    }

    private fun tempCredentials(refreshToken: String): File {
        val file = Files.createTempFile("capsule-youtube-", ".yaml").toFile()
        file.writeText(
            """
            youtube:
              clientId: "cid"
              clientSecret: "secret"
              refreshToken: "$refreshToken"
            """.trimIndent(),
        )
        return file
    }
}
