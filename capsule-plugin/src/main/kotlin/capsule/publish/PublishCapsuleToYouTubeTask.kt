package capsule.publish

import capsule.adapter.youtube.CapsuleYouTubePublisherResolver
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import video.kernel.publish.ChapterLineParser
import video.kernel.publish.VideoDescriptionComposer
import video.kernel.publish.VideoLink
import video.kernel.publish.VideoLinks
import video.kernel.publish.VideoPublication
import video.kernel.publish.VideoPublicationResult
import video.kernel.publish.VideoPublicationValidator
import video.kernel.publish.VideoVisibility
import java.io.File

/**
 * `publishCapsuleToYouTube` — publishes the produced MP4 to the configured
 * YouTube channel through the shared N1 `video.kernel.publish.VideoPublisher`
 * port (HF-YOUTUBE-PUBLISH US-3b).
 *
 * Opt-in (`publish.enabled = false` by default). The description is assembled
 * by the neutral [VideoDescriptionComposer] (chapters + links + hashtags), the
 * publication is validated before any network call, then handed to the publisher
 * resolved by [CapsuleYouTubePublisherResolver].
 *
 * Fail-soft: a degraded publisher (disabled, credentials absent) never fails the
 * build; `publish.strict = true` promotes a validation failure or an API refusal
 * to a build failure. Writes `publication.json` into [outputDir] for traceability.
 */
@DisableCachingByDefault(because = "Publishes over the network (side effect): never cacheable")
abstract class PublishCapsuleToYouTubeTask : DefaultTask() {
    /** The produced MP4 to publish. Tolerant: a missing file degrades, never fails validation. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val videoFiles: ConfigurableFileCollection

    /**
     * Chapter lines (`M:SS Title`) produced by `generateCapsuleChapters`.
     *
     * Tolerant on purpose (`@InputFiles` + an absent/blank file falls back to the
     * `chapters` list): chapters are opt-in, and a disabled chapter task must not
     * fail the publication validation.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val chaptersFile: ConfigurableFileCollection

    /** Report directory (receives `publication.json`). */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val publishEnabled: Property<Boolean>

    @get:Input
    abstract val platform: Property<String>

    @get:Input
    abstract val credentialsFile: Property<String>

    @get:Input
    @get:Optional
    abstract val title: Property<String>

    @get:Input
    @get:Optional
    abstract val publishDescription: Property<String>

    @get:Input
    abstract val tags: ListProperty<String>

    @get:Input
    abstract val hashtags: ListProperty<String>

    @get:Input
    abstract val links: ListProperty<String>

    @get:Input
    @get:Optional
    abstract val nextVideo: Property<String>

    @get:Input
    @get:Optional
    abstract val previousVideo: Property<String>

    @get:Input
    @get:Optional
    abstract val thumbnailFile: Property<String>

    @get:Input
    @get:Optional
    abstract val visibility: Property<String>

    @get:Input
    @get:Optional
    abstract val categoryId: Property<String>

    @get:Input
    @get:Optional
    abstract val languageCode: Property<String>

    @get:Input
    @get:Optional
    abstract val publishAt: Property<String>

    @get:Input
    abstract val chapters: ListProperty<String>

    @get:Input
    abstract val strict: Property<Boolean>

    private val mapper = ObjectMapper()

    @TaskAction
    fun publish() {
        val outDir = outputDir.asFile.get().apply { mkdirs() }
        val reportFile = File(outDir, "publication.json")

        if (!publishEnabled.get()) {
            logger.lifecycle("publishCapsuleToYouTube: disabled (publish.enabled=false) — skipping")
            writeReport(reportFile, skipped = true, reason = "publication disabled", url = null)
            return
        }

        val video = resolveVideo()
        val base = buildPublication(video)
        val publication = base.copy(description = VideoDescriptionComposer.compose(base))

        val validation = VideoPublicationValidator.validate(publication)
        if (!validation.valid) {
            val message = "publication invalid: ${validation.reasons.joinToString("; ")}"
            writeReport(reportFile, skipped = false, reason = message, url = null)
            if (strict.get()) throw GradleException("publishCapsuleToYouTube: $message")
            logger.warn("publishCapsuleToYouTube: $message")
            return
        }

        val publisher =
            CapsuleYouTubePublisherResolver.resolve(
                enabled = true,
                platform = platform.get(),
                credentialsFile = File(credentialsFile.get()),
                onLog = { logger.lifecycle("publishCapsuleToYouTube: $it") },
            )

        when (val result = publisher.publish(publication)) {
            is VideoPublicationResult.Published -> {
                logger.lifecycle("publishCapsuleToYouTube: published ${result.url}")
                writeReport(reportFile, skipped = false, reason = "published", url = result.url, videoId = result.videoId)
            }
            is VideoPublicationResult.Degraded -> {
                logger.warn("publishCapsuleToYouTube: degraded — ${result.reason}")
                writeReport(reportFile, skipped = true, reason = result.reason, url = null)
            }
            is VideoPublicationResult.Failed -> {
                writeReport(reportFile, skipped = false, reason = result.reason, url = null)
                if (strict.get()) throw GradleException("publishCapsuleToYouTube: ${result.reason}")
                logger.warn("publishCapsuleToYouTube: failed — ${result.reason}")
            }
        }
    }

    /**
     * Deterministic video selection (see [PublicationInputSelection.selectVideo]).
     *
     * A capsule build can hold several MP4s (multi-language render). Picking
     * `files.first()` silently uploaded an arbitrary one; the ambiguity is now an
     * explicit warning, never a silent choice.
     */
    private fun resolveVideo(): File? {
        val selection = PublicationInputSelection.selectVideo(videoFiles.files, languageCode.orNull.orEmpty())
        if (selection.ambiguous) {
            logger.warn(
                "publishCapsuleToYouTube: ${selection.candidates} candidate videos, selecting " +
                    "'${selection.file?.name}' (set publish.languageCode to disambiguate)",
            )
        }
        return selection.file
    }

    /**
     * Chapter lines from `generateCapsuleChapters` (`chapters.txt`) when present,
     * else the configured `chapters` list (see [PublicationInputSelection.chapterLines]).
     * An empty chapter list means "no YouTube timeline" — never an invalid one.
     */
    private fun resolveChapterLines(): List<String> =
        PublicationInputSelection.chapterLines(
            chaptersFile.files.firstOrNull { it.exists() && it.isFile },
            chapters.getOrElse(emptyList()),
        )

    private fun buildPublication(video: File?): VideoPublication {
        val thumbnail =
            thumbnailFile.orNull
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf { it.exists() }
        return VideoPublication(
            videoFile = video ?: File(""),
            title = title.orNull.orEmpty(),
            description = publishDescription.orNull.orEmpty(),
            tags = tags.getOrElse(emptyList()),
            hashtags = hashtags.getOrElse(emptyList()),
            chapters = ChapterLineParser.parse(resolveChapterLines()),
            links =
                VideoLinks(
                    entries = links.getOrElse(emptyList()).mapNotNull(::parseLink),
                    nextVideo = nextVideo.orNull.orEmpty(),
                    previousVideo = previousVideo.orNull.orEmpty(),
                ),
            visibility = parseVisibility(visibility.orNull),
            categoryId = categoryId.orNull.orEmpty(),
            languageCode = languageCode.orNull.orEmpty(),
            madeForKids = false,
            thumbnailFile = thumbnail,
            publishAt = publishAt.orNull?.takeIf { it.isNotBlank() },
        )
    }

    private fun parseLink(raw: String): VideoLink? {
        val separator = raw.indexOf('=')
        if (separator <= 0) return null
        val label = raw.substring(0, separator).trim()
        val url = raw.substring(separator + 1).trim()
        if (label.isEmpty() || url.isEmpty()) return null
        return VideoLink(label, url)
    }

    private fun parseVisibility(value: String?): VideoVisibility =
        when (value?.trim()?.lowercase()) {
            "public" -> VideoVisibility.PUBLIC
            "unlisted" -> VideoVisibility.UNLISTED
            else -> VideoVisibility.PRIVATE
        }

    private fun writeReport(
        file: File,
        skipped: Boolean,
        reason: String,
        url: String?,
        videoId: String? = null,
    ) {
        val node = mapper.createObjectNode()
        node.put("skipped", skipped)
        node.put("reason", reason)
        url?.let { node.put("url", it) }
        videoId?.let { node.put("videoId", it) }
        file.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node))
    }
}
