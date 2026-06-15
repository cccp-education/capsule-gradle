package capsule

import org.gradle.api.logging.Logging

/**
 * Replaces a slide's HTML <section> element with a <section> containing
 * a <video> tag that embeds the Manim MP4 output.
 *
 * When a slide has type MANIM, the ManimEngine produces an MP4 video.
 * The ManimVideoMixer muxes it with TTS audio.
 * This replacer swaps the slide's HTML content in the deck with a video embed,
 * so the Playwright capture renders the video frame instead of the original slide content.
 *
 * Design:
 * - ManimSlideReplacerImpl parses the deck HTML and replaces the <section> at slideIndex
 * - NoOpManimSlideReplacer returns the deck unchanged (for testing)
 * - Factory: CapsuleManager.resolveManimSlideReplacer()
 */
interface ManimSlideReplacer {
    /**
     * Replaces the <section> element at [slideIndex] in [deckHtml]
     * with a <section> containing a <video> tag pointing to [videoPath].
     *
     * @param deckHtml The full deck HTML source
     * @param slideIndex 0-based index of the top-level section to replace
     * @param videoPath Path to the Manim MP4 video (muxed with TTS audio)
     * @return Modified deck HTML with the slide replaced
     */
    fun replaceSlideAt(deckHtml: String, slideIndex: Int, videoPath: String): String

    /** Returns true if the replacer is available */
    fun isAvailable(): Boolean

    /** Returns the replacer name for logging */
    fun name(): String
}

class ManimSlideReplacerImpl : ManimSlideReplacer {

    private val logger = Logging.getLogger(ManimSlideReplacerImpl::class.java)

    override fun isAvailable(): Boolean = true

    override fun name(): String = "html-replacer"

    override fun replaceSlideAt(deckHtml: String, slideIndex: Int, videoPath: String): String {
        if (slideIndex < 0) {
            logger.warn("ManimSlideReplacer: negative slide index {} — returning deck unchanged", slideIndex)
            return deckHtml
        }

        val slidesDivRegex = Regex("""(?s)<div class="slides">\s*(.*?)\s*</div>""", RegexOption.DOT_MATCHES_ALL)
        val slidesDivMatch = slidesDivRegex.find(deckHtml)
        if (slidesDivMatch == null) {
            logger.warn("ManimSlideReplacer: no slides div found in deck — returning unchanged")
            return deckHtml
        }

        val slidesContent = slidesDivMatch.groupValues[1]
        val topSections = HtmlSectionParser.extractTopLevelSections(slidesContent)

        if (slideIndex >= topSections.size) {
            logger.warn("ManimSlideReplacer: slide index {} out of bounds ({} sections) — returning deck unchanged", slideIndex, topSections.size)
            return deckHtml
        }

        // Build replacement section: preserve data-capsule-slide if present, add video embed
        val originalSection = topSections[slideIndex]
        val capsuleSlideMatch = Regex("""data-capsule-slide="(\d+)"""").find(originalSection)
        val slideAttr = capsuleSlideMatch?.groupValues?.get(1)

        val videoEmbed = buildString {
            append("<section")
            if (slideAttr != null) {
                append(" data-capsule-slide=\"$slideAttr\"")
            }
            appendLine(">")
            appendLine("  <video src=\"$videoPath\" autoplay loop muted style=\"width:100%;height:100%;object-fit:contain;\">")
            appendLine("    Your browser does not support the video tag.")
            appendLine("  </video>")
            appendLine("</section>")
        }

        // Replace the specific top-level section in the deck HTML
        val originalRange = slidesDivMatch.groups[1]!!.range
        val beforeSlides = deckHtml.substring(0, originalRange.first)
        val afterSlides = deckHtml.substring(originalRange.last + 1)

        // Rebuild slides content with the replacement
        val newSlidesContent = buildString {
            var offset = 0
            for ((i, section) in topSections.withIndex()) {
                val sectionStart = slidesContent.indexOf(section, offset)
                if (sectionStart == -1) continue

                append(slidesContent.substring(offset, sectionStart))
                append(if (i == slideIndex) videoEmbed else section)
                offset = sectionStart + section.length
            }
            append(slidesContent.substring(offset))
        }

        return beforeSlides + newSlidesContent + afterSlides
    }
}

class NoOpManimSlideReplacer : ManimSlideReplacer {
    override fun isAvailable(): Boolean = true
    override fun name(): String = "noop-replacer"

    override fun replaceSlideAt(deckHtml: String, slideIndex: Int, videoPath: String): String {
        return deckHtml
    }
}