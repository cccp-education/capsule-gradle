package capsule.scenarios

import capsule.MediaProbeUtil
import capsule.test.BinaryAvailability
import capsule.viral.ViralDeckBuilder
import capsule.viral.ViralTimeFitter
import io.cucumber.java.PendingException
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import java.io.File
import java.nio.file.Files

/**
 * Cucumber steps for the real short-form render (CAP-SHORT US-5).
 *
 * Steps are prefixed "real viral render" / "real MP4" to avoid collisions on the
 * shared `capsule.scenarios` glue (bug S-088). The scenario needs real
 * `ffmpeg`/`ffprobe`; when they are absent the step is marked **pending** (not
 * failed) so the fast `Test` job stays green — it runs in the Full Tests job.
 */
class CapsuleViralRenderSteps {

    private var mp4: File? = null
    private var tolerance: Double = 0.0

    @Given("a real viral render of beats {string} to target {int} with tolerance {double}")
    fun `a real viral render`(beats: String, target: Int, tolerance: Double) {
        if (!BinaryAvailability.isAvailable("ffmpeg") || !BinaryAvailability.isAvailable("ffprobe")) {
            throw PendingException("ffmpeg/ffprobe required — runs in the Full Tests job")
        }
        this.tolerance = tolerance
        val durations = ViralTimeFitter.fit(beats.split(",").map { it.trim().toDouble() }, target)
        val dir = Files.createTempDirectory("viral-render-real").toFile()
        mp4 = ViralRealRenderSupport.renderMp4(dir, durations, ViralDeckBuilder.WIDTH, ViralDeckBuilder.HEIGHT)
    }

    @Then("a real MP4 is produced")
    fun `a real MP4 is produced`() {
        assertThat(mp4).isNotNull()
        assertThat(mp4!!.exists() && mp4!!.length() > 0L).isTrue()
    }

    @Then("the real MP4 duration is close to {int}")
    fun `the real MP4 duration is close to`(expected: Int) {
        val seconds = MediaProbeUtil.probeDuration(mp4!!)
        assertThat(seconds).isCloseTo(expected.toDouble(), within(tolerance))
    }
}
