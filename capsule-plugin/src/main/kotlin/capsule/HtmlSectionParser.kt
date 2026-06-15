package capsule

object HtmlSectionParser {

    private val sectionOpenRegex = Regex("""<section\b(?![^>]*/>)[^>]*>""")
    private val sectionCloseRegex = Regex("""</section>""")

    fun extractTopLevelSections(slidesContent: String): List<String> {
        val sections = mutableListOf<String>()
        var depth = 0
        var currentStart = -1
        var pos = 0

        while (pos < slidesContent.length) {
            val openMatch = sectionOpenRegex.find(slidesContent, pos)
            val closeMatch = sectionCloseRegex.find(slidesContent, pos)

            val nextOpen = openMatch?.range?.first ?: Int.MAX_VALUE
            val nextClose = closeMatch?.range?.first ?: Int.MAX_VALUE

            if (nextOpen < nextClose && openMatch != null) {
                if (depth == 0) {
                    currentStart = openMatch.range.first
                }
                depth++
                pos = openMatch.range.last + 1
            } else if (closeMatch != null) {
                depth--
                if (depth == 0 && currentStart >= 0) {
                    sections.add(slidesContent.substring(currentStart, closeMatch.range.last + 1))
                    currentStart = -1
                }
                pos = closeMatch.range.last + 1
            } else {
                break
            }
        }

        return sections
    }
}
