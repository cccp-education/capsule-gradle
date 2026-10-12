package capsule.adapter.revealjs

import capsule.HtmlSectionParser
import capsule.kernel.VideoSegment

/**
 * Reveal.js deck HTML adapter — the pure, renderer-specific HTML manipulation
 * extracted from `CapsuleVideoTask` (CAP-ENGINE-DECOUPLING US-2).
 *
 * This object owns **every** reveal.js-specific concern expressed on the deck
 * markup: extracting one `<section>` into a standalone document, forcing the
 * extracted slide visible, and injecting the autoplay audio wiring
 * (`.reveal .slides section[data-audio]`). None of this leaks into the neutral
 * kernel: the kernel speaks `segment`/`visual`/`engine`, this adapter speaks the
 * reveal.js DOM.
 *
 * Pure — no I/O, no Gradle, no Playwright. Deterministic string transforms.
 */
object RevealJsDeckHtml {

    /**
     * Forces the one extracted slide to be visible whatever the deck's own
     * stylesheet does with `.slides section` (reveal.css hides every slide
     * but the current one, and positions them absolutely).
     */
    internal const val SINGLE_SLIDE_VISIBILITY_CSS: String =
        ".reveal .slides section{display:block!important;visibility:visible!important;" +
            "opacity:1!important;transform:none!important;position:relative!important;top:0!important;left:0!important}"

    /**
     * Autoplay audio wiring injected before `</body>`: collects every
     * `section[data-audio]` into an `Audio` element and plays the matching one
     * on `Reveal`'s `slidechanged` event. The headless capture never plays the
     * audio; FFmpeg mixes it later — this script only drives the browser path.
     */
    internal const val AUDIO_INJECT_SCRIPT: String = """
<!-- CAPSULE-GRADLE: Autoplay audio injection -->
<script>
(function() {
  var currentAudio = null;
  var sections = document.querySelectorAll('.reveal .slides section[data-audio]');
  var audios = [];
  sections.forEach(function(sec) {
    var src = sec.getAttribute('data-audio');
    if (src) {
      var audio = new Audio(src.replace('file://', ''));
      audio.id = 'audio-' + audios.length;
      document.body.appendChild(audio);
      audios.push(audio);
    }
  });
  function playSlideAudio(idx) {
    if (currentAudio) { currentAudio.pause(); currentAudio.currentTime = 0; }
    currentAudio = audios[idx];
    if (currentAudio) {
      currentAudio.currentTime = 0;
      currentAudio.play().catch(function(e) { console.warn('Audio play failed:', e); });
    }
  }
  if (typeof Reveal !== 'undefined') {
    Reveal.on('slidechanged', function(event) {
      playSlideAudio(event.indexh);
    });
    if (audios.length > 0) playSlideAudio(0);
  }
})();
</script>
"""

    /** `true` when the deck carries the `data-capsule-slide` marker. */
    fun hasDataCapsuleSlide(deckHtml: String): Boolean = deckHtml.contains("data-capsule-slide=")

    /**
     * Extracts a single slide from a reveal.js deck HTML and produces a
     * standalone HTML document that can be rendered independently by a capture
     * engine.
     *
     * @param deckHtml   the full deck HTML source.
     * @param slideIndex 0-based index of the section within the `.slides` container.
     * @return standalone HTML with only the requested slide, preserving head,
     *         styles and scripts; [deckHtml] unchanged when no slide matches.
     */
    fun singleSlideHtml(deckHtml: String, slideIndex: Int): String {
        val headSection = HtmlSectionParser.headMarkup(deckHtml)

        // Greedy on purpose: the lazy form stops at the first nested `</div>`,
        // which silently truncates any deck whose slides contain a `<div>` — the
        // container's `</div>` is the last. The section count then fell short and
        // the extraction returned the whole deck instead of the requested slide.
        // Same rule as RemotionCapture.slidesMarkup, of which it is the source.
        val slidesContent = HtmlSectionParser.slidesMarkup(deckHtml)
            ?: return deckHtml

        val topLevelSections = HtmlSectionParser.extractTopLevelSections(slidesContent)

        if (slideIndex < 0 || slideIndex >= topLevelSections.size) {
            return deckHtml
        }

        val targetSection = topLevelSections[slideIndex]

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine(headSection)
            appendLine("<body>")
            appendLine("""<div class="reveal">""")
            appendLine("""  <div class="slides">""")
            appendLine("    $targetSection")
            appendLine("  </div>")
            appendLine("</div>")
            // No reveal.js is pulled in here. The deck already carries whatever
            // presentation runtime it was built with, in the <head> preserved
            // above; fetching a second, different copy from a CDN made it
            // re-layout the page — titles and captions ended up off-screen — and
            // made parallel capture depend on network access. A single extracted
            // slide needs no runtime at all, only to be visible, so the deck's
            // own rules are overridden with one inline stylesheet.
            appendLine("<style>")
            appendLine(SINGLE_SLIDE_VISIBILITY_CSS)
            appendLine("</style>")
            appendLine("</body>")
            appendLine("</html>")
        }.trimIndent()
    }

    /**
     * Appends the autoplay [AUDIO_INJECT_SCRIPT] before `</body>`.
     *
     * @param markerSuffix optional suffix appended to the script's HTML comment
     *        marker (used by the sequential fallback for diagnosability).
     */
    fun injectAudioScript(deckHtml: String, markerSuffix: String = ""): String {
        val script = if (markerSuffix.isEmpty()) {
            AUDIO_INJECT_SCRIPT
        } else {
            AUDIO_INJECT_SCRIPT.replace(
                "<!-- CAPSULE-GRADLE: Autoplay audio injection -->",
                "<!-- CAPSULE-GRADLE: Autoplay audio injection ($markerSuffix) -->"
            )
        }
        return deckHtml.replace("</body>", "$script</body>")
    }

    /**
     * Injects `data-audio="file://<path>"` into the `<section>` carrying
     * `data-capsule-slide="<index>"`, for every index present in
     * [audioPathByIndex]. Lines containing a self-closed `<section>` are left
     * untouched (same rule as the original line scan).
     */
    fun injectDataSlideAudio(deckHtml: String, audioPathByIndex: Map<Int, String>): String =
        deckHtml.lines().map { line ->
            if (line.contains("<section") && !line.contains("</section>")) {
                var mutableLine = line
                for ((index, audioPath) in audioPathByIndex) {
                    if (line.contains("data-capsule-slide=\"$index\"") ||
                        line.contains("data-capsule-slide='$index'")
                    ) {
                        mutableLine = mutableLine.replace(
                            "<section",
                            "<section data-audio=\"file://$audioPath\""
                        )
                        break
                    }
                }
                mutableLine
            } else {
                line
            }
        }.joinToString("\n")

    /**
     * Fallback used when the deck lacks the `data-capsule-slide` marker: walks
     * the top-level `<section>` open tags in document order and attaches the
     * audio of the matching segment positionally ([orderedAudioPaths] aligned
     * with the deck's sections).
     */
    fun injectSequentialAudio(deckHtml: String, orderedAudioPaths: List<String>): String {
        val sections = HtmlSectionParser.findTopLevelSectionOpenTags(deckHtml)
        return buildString {
            var lastEnd = 0
            var slideIdx = 0
            for (match in sections) {
                append(deckHtml.substring(lastEnd, match.range.first))
                var tag = match.value
                if (slideIdx < orderedAudioPaths.size) {
                    val audioPath = orderedAudioPaths[slideIdx]
                    tag = tag.replace("<section", "<section data-audio=\"file://$audioPath\"")
                }
                append(tag)
                lastEnd = match.range.last + 1
                slideIdx++
            }
            append(deckHtml.substring(lastEnd))
        }
    }
}

/** Convenience holder to keep adapter call-sites readable. */
internal fun VideoSegment.audioFileIndex(): String = String.format("%02d", index)
