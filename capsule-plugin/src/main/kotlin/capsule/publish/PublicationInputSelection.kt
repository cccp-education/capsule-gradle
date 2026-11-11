package capsule.publish

import java.io.File

/**
 * Outcome of the video selection of a publication (HF-YOUTUBE-PUBLISH US-3b).
 *
 * @property file the selected video, or `null` when the build produced none.
 * @property candidates how many videos were available.
 * @property ambiguous `true` when several videos were possible and no explicit
 *                      language picked one — the caller must warn.
 */
data class VideoSelection(
    val file: File?,
    val candidates: Int,
    val ambiguous: Boolean,
)

/**
 * Pure input selection of the YouTube publication (HF-YOUTUBE-PUBLISH US-3b).
 *
 * A capsule build can hold several MP4s (multi-language render). Selecting
 * `files.first()` silently uploaded an arbitrary one; this object makes the
 * choice deterministic and testable without a Gradle build.
 */
object PublicationInputSelection {
    /**
     * Deterministic video choice:
     * 1. a single MP4 → that file;
     * 2. [languageCode] set → the MP4 whose name ends with `_<lang>` (the
     *    `<deck>_<lang>.mp4` convention);
     * 3. otherwise the lexicographically first MP4, flagged [VideoSelection.ambiguous].
     */
    fun selectVideo(
        videos: Collection<File>,
        languageCode: String,
    ): VideoSelection {
        val existing = videos.filter { it.exists() && it.isFile }
        val mp4s = existing.filter { it.extension.equals("mp4", ignoreCase = true) }.sortedBy { it.absolutePath }
        val pool = mp4s.ifEmpty { existing.sortedBy { it.absolutePath } }
        if (pool.size <= 1) return VideoSelection(pool.firstOrNull(), pool.size, ambiguous = false)

        val language = languageCode.trim()
        if (language.isNotBlank()) {
            val matches = pool.filter { it.nameWithoutExtension.endsWith("_$language", ignoreCase = true) }
            if (matches.isNotEmpty()) return VideoSelection(matches.first(), pool.size, ambiguous = false)
        }
        return VideoSelection(pool.first(), pool.size, ambiguous = true)
    }

    /**
     * Chapter lines from the generated `chapters.txt` when it has content, else
     * the configured lines. Never invents a chapter.
     */
    fun chapterLines(
        chaptersFile: File?,
        configured: List<String>,
    ): List<String> {
        val fileLines =
            chaptersFile
                ?.takeIf { it.exists() && it.isFile }
                ?.readLines()
                ?.filter { it.isNotBlank() }
                .orEmpty()
        return fileLines.ifEmpty { configured }
    }
}
