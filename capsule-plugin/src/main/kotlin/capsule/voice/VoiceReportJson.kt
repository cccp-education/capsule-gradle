package capsule.voice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Serializes a [VoiceReport] to its stable JSON contract (CAP-QC-VOICE US-2).
 *
 * The report is the readable artefact a reviewer reads to decide whether a
 * take must be re-synthesised; the JSON shape is deterministic (declaration
 * order) so diffing two reports is meaningful.
 *
 * ```json
 * { "degraded": false,
 *   "thresholds": { "maxWordErrorRate": 0.15, "minSnrDb": 66.0, "surplusThreshold": 3 },
 *   "failureCount": 1,
 *   "takes": [ { "deckName": "demo", "slideIndex": 1, "audioFile": "slide-01.mp3",
 *                "expectedText": "...", "transcript": "...", "wordErrorRate": 0.0,
 *                "surplus": 0, "snrDb": 70.0, "verdict": "PASS", "reasons": [] } ] }
 * ```
 *
 * Pattern: [capsule.viral.CampaignBundleSerializer] (Jackson `ObjectNode`).
 * Pure — no Gradle, no I/O.
 */
object VoiceReportJson {

    private val mapper = ObjectMapper()

    /** Serializes [report] to a pretty JSON string (deterministic key order). */
    fun toJson(report: VoiceReport): String =
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reportNode(report))

    private fun reportNode(report: VoiceReport): ObjectNode {
        val root = mapper.createObjectNode()
        root.put("degraded", report.degraded)
        val thresholds = root.putObject("thresholds")
        thresholds.put("maxWordErrorRate", report.thresholds.maxWordErrorRate)
        thresholds.put("minSnrDb", report.thresholds.minSnrDb)
        thresholds.put("surplusThreshold", report.thresholds.surplusThreshold)
        root.put("failureCount", report.failures.size)
        val takes: ArrayNode = root.putArray("takes")
        report.takes.forEach { takes.add(takeNode(it)) }
        return root
    }

    private fun takeNode(take: TakeVerification): ObjectNode {
        val node = mapper.createObjectNode()
        node.put("deckName", take.deckName)
        node.put("slideIndex", take.slideIndex)
        node.put("audioFile", take.audioFile)
        node.put("expectedText", take.expectedText)
        node.put("transcript", take.transcript)
        putNullableDouble(node, "wordErrorRate", take.wordErrorRate)
        if (take.surplus == null) node.putNull("surplus") else node.put("surplus", take.surplus)
        putNullableDouble(node, "snrDb", take.snrDb)
        val verdict = take.verdict
        node.put("verdict", verdictName(verdict))
        val reasons: ArrayNode = node.putArray("reasons")
        verdictReasons(verdict).forEach { reasons.add(it) }
        return node
    }

    private fun putNullableDouble(node: ObjectNode, key: String, value: Double?) {
        if (value == null) node.putNull(key) else node.put(key, value)
    }

    private fun verdictName(verdict: VoiceVerdict): String = when (verdict) {
        is VoiceVerdict.Pass -> "PASS"
        is VoiceVerdict.Fail -> "FAIL"
        is VoiceVerdict.Degraded -> "DEGRADED"
    }

    private fun verdictReasons(verdict: VoiceVerdict): List<String> = when (verdict) {
        is VoiceVerdict.Pass -> emptyList()
        is VoiceVerdict.Fail -> verdict.reasons
        is VoiceVerdict.Degraded -> listOf(verdict.reason)
    }
}
