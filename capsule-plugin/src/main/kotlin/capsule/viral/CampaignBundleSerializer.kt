package capsule.viral

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Pure serializer of a [CampaignBundle] to its stable JSON contract
 * (CAP-VIRAL US-10).
 *
 * Produces the deterministic JSON that the distribution layer (GBL-002) reads:
 * ```json
 * { "language": "fr", "platform": "TIKTOK",
 *   "videoFile": "...", "thumbnailFile": "...",
 *   "metadata": { "hook": "...", "title": "...", "caption": "...",
 *                 "hashtags": ["#fpa"] } }
 * ```
 *
 * Pattern: `capsule.pipeline.ContentPlanValidator` (Jackson) — the writer side.
 * Pure — no Gradle, no I/O.
 */
object CampaignBundleSerializer {

    private val mapper = ObjectMapper()

    /** Serializes [bundle] to a pretty JSON string (deterministic key order). */
    fun toJson(bundle: CampaignBundle): String {
        val root: ObjectNode = mapper.createObjectNode()
        root.put("language", bundle.language)
        root.put("platform", bundle.platform.name)
        root.put("videoFile", bundle.videoFile)
        root.put("thumbnailFile", bundle.thumbnailFile)
        val metadata: ObjectNode = root.putObject("metadata")
        metadata.put("hook", bundle.metadata.hook)
        metadata.put("title", bundle.metadata.title)
        metadata.put("caption", bundle.metadata.caption)
        val hashtags: ArrayNode = metadata.putArray("hashtags")
        bundle.metadata.hashtags.forEach { hashtags.add(it) }
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root)
    }
}
