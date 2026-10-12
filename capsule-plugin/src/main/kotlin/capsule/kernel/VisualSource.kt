package capsule.kernel

import java.io.File

/**
 * Engine-neutral visual source of a video production (CAP-ENGINE-DECOUPLING US-1).
 *
 * The kernel does not know whether a visual is a Reveal.js deck, a HyperFrames
 * composition or any other renderer input — it only knows a visual source
 * resolves to a [file] the engine adapter can consume. The concrete source
 * kinds (deck HTML, composition HTML, …) are adapter concerns.
 */
interface VisualSource {

    /** The file the engine adapter consumes to produce the visuals. */
    val file: File
}

/**
 * Default [VisualSource] — a plain file on disk.
 *
 * Used for the Reveal.js adapter today (the translated `<name>_<lang>-deck.html`)
 * and generic for any file-backed visual source.
 *
 * @property file the visual file, must exist on disk (checked by
 * [VideoProductionEntry]).
 */
data class FileVisualSource(override val file: File) : VisualSource
