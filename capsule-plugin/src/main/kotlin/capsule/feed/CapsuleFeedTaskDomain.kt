package capsule.feed

import java.io.File

/**
 * Value object describing the AsciiDoc deck source directory scanned by the
 * `extractSpeakerNotes` task (`<projectDir>/slides/misc`).
 *
 * Pure data — no Gradle dependency. The Gradle adapter [CapsuleFeedTaskRegistrar]
 * uses this value object to resolve the directory and list the `.adoc` files
 * without re-hardcoding the path.
 */
data class CapsuleAdocDir(val projectDir: File) {
    init {
        require(projectDir.path.isNotBlank()) { "projectDir must not be blank" }
    }

    /**
     * Resolves the concrete `<projectDir>/slides/misc` directory file.
     */
    fun asFile(): File = projectDir.resolve("slides").resolve("misc")

    /**
     * Lists the `.adoc` files directly contained in this directory, sorted by
     * name. Returns an empty list when the directory does not exist or holds
     * no `.adoc` file. Subdirectories are not traversed.
     */
    fun adocFiles(): List<File> {
        val dir = asFile()
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles { file -> file.isFile && file.extension == "adoc" }
            ?.sortedBy { it.name }
            ?: emptyList()
    }
}

/**
 * Value object describing the capsule script output directory
 * (`<buildDir>/capsule`).
 *
 * Pure data — no Gradle dependency. The Gradle adapter [CapsuleFeedTaskRegistrar]
 * calls [ensureCreated] before writing each script file resolved by
 * [scriptFileFor].
 */
data class CapsuleScriptDir(val buildDir: File) {
    init {
        require(buildDir.path.isNotBlank()) { "buildDir must not be blank" }
    }

    /**
     * Resolves the concrete `<buildDir>/capsule` directory file.
     */
    fun asFile(): File = buildDir.resolve("capsule")

    /**
     * Creates the capsule directory if it does not exist yet, idempotently.
     */
    fun ensureCreated(): File = asFile().apply { mkdirs() }

    /**
     * Resolves the script output file for the given [deckName] (without
     * extension): `<buildDir>/capsule/<deckName>-script.txt`.
     */
    fun scriptFileFor(deckName: String): File =
        ensureCreated().resolve("$deckName-script.txt")
}

/**
 * Stable task identifiers and metadata owned by the `capsule.feed` domain.
 */
object CapsuleFeedTaskNames {
    const val EXTRACT_SPEAKER_NOTES = "extractSpeakerNotes"
    const val TRANSLATE_AND_EXTRACT_SPEAKER_NOTES = "translateAndExtractSpeakerNotes"
    const val TRANSLATE_AND_GENERATE_CAPSULE_VIDEOS = "translateAndGenerateCapsuleVideos"
    const val GENERATE_CAPSULE_CONTENT_AND_VIDEOS = "generateCapsuleContentAndVideos"
    const val GROUP = "capsule"
    const val DESCRIPTION =
        "Extract speaker notes from AsciiDoc decks and generate a capsule script (replaces slider-gradle generateCapsule)."
    const val TRANSLATE_AND_EXTRACT_DESCRIPTION =
        "Translate a deck into multiple languages then extract speaker notes for all translated decks. Depends on translateDeck and extractSpeakerNotes."
    const val TRANSLATE_AND_GENERATE_VIDEOS_DESCRIPTION =
        "Translate a deck, extract speaker notes for all translated decks, then generate one localized capsule WebM per language. Depends on translateAndExtractSpeakerNotes and generateCapsuleVideoAllLanguages."
    const val CONTENT_AND_VIDEOS_DESCRIPTION =
        "Orchestrates LLM content generation then the full multi-language video pipeline. Depends on generateCapsuleContent and translateAndGenerateCapsuleVideos."
}