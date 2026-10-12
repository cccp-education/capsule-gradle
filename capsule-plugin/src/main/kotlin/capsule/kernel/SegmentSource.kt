package capsule.kernel

import java.io.File

/**
 * Engine-neutral port that turns a source file into ordered [VideoSegment]s
 * (CAP-ENGINE-DECOUPLING US-3).
 *
 * The kernel does not know whether the source is an AsciiDoc reveal.js deck, a
 * HyperFrames composition or any other authoring format — only that a source
 * yields the narration segments a [VideoEngine] will render. The format-specific
 * parsing lives in the adapters.
 */
interface SegmentSource {

    /** Human-readable source name (diagnostics only). */
    fun name(): String

    /**
     * Extracts the narration segments from [source].
     *
     * @return ordered segments (possibly empty when the source carries no
     *         narration); never `null`.
     */
    fun segments(source: File): List<VideoSegment>
}
