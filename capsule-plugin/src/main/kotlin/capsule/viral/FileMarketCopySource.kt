package capsule.viral

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * File-backed [MarketCopySource] — reads bakery's copy output (CAP-VIRAL US-9).
 *
 * bakery (GBL-006) writes one JSON per (language × platform) copy piece. The
 * expected shape (tolerant — unknown keys ignored, missing keys degrade):
 * ```json
 * { "language": "fr", "platform": "TIKTOK",
 *   "hook": "...", "title": "...", "caption": "...",
 *   "hashtags": ["#fpa", "#formation"] }
 * ```
 *
 * Resolution: `<root>/<language>-<platform>.json` (deterministic, matching
 * [ViralVariant] id), falling back to `<root>/<language>.json`. Missing file or
 * unparseable content yields `null` (degraded — economy of ink: no copy is not
 * a failure, the campaign falls back to the generated hook).
 *
 * Pattern: `capsule.pipeline.ContentPlanValidator` (Jackson, tolerant).
 *
 * @property root the directory holding bakery's copy pieces.
 */
class FileMarketCopySource(private val root: File) : MarketCopySource {

    private val jsonMapper = ObjectMapper()

    override fun isAvailable(): Boolean =
        root.isDirectory && root.listFiles { f -> f.isFile && f.name.endsWith(".json") }?.isNotEmpty() == true

    override fun read(language: String, platform: ViralPlatform): MarketCopy? {
        if (language.isBlank()) return null
        val candidates = listOf(
            root.resolve("$language-${platform.name.lowercase()}.json"),
            root.resolve("$language.json"),
        )
        val file = candidates.firstOrNull { it.isFile } ?: return null
        return parse(file)
    }

    private fun parse(file: File): MarketCopy? {
        val root: JsonNode = try {
            jsonMapper.readTree(file)
        } catch (_: Exception) {
            return null
        }
        if (root.isMissingNode || !root.isObject) return null

        val language = root.get("language")?.asText()?.trim().orEmpty()
        if (language.isBlank()) return null
        val platform = ViralPlatform.fromString(root.get("platform")?.asText())
        val hook = root.get("hook")?.asText()?.trim().orEmpty()
        val title = root.get("title")?.asText()?.trim().orEmpty()
        val caption = root.get("caption")?.asText()?.trim().orEmpty()
        val hashtags = root.get("hashtags")
            ?.takeIf { it.isArray }
            ?.mapNotNull { it.asText()?.trim()?.takeIf { h -> h.isNotBlank() } }
            ?: emptyList()

        if (hook.isBlank() && title.isBlank() && caption.isBlank()) return null
        return MarketCopy(language, platform, hook, title, caption, hashtags)
    }
}
