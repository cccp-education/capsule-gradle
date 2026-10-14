package capsule.adapter.revealjs

import video.kernel.segment.SegmentSource
import video.kernel.segment.VideoSegment
import java.io.File

/**
 * Reveal.js [SegmentSource] (CAP-ENGINE-DECOUPLING US-3).
 *
 * Yields the narration segments of an AsciiDoc reveal.js deck by delegating to
 * [RevealJsSpeakerNoteParser]. This is the reveal.js implementation of the
 * engine-neutral source port; a second engine (HyperFrames) would provide its
 * own [SegmentSource].
 */
object RevealJsSpeakerNoteSource : SegmentSource {

    override fun name(): String = "revealjs-speaker-notes"

    override fun segments(source: File): List<VideoSegment> =
        RevealJsSpeakerNoteParser.parse(source.readText(), source.nameWithoutExtension).segments
}
