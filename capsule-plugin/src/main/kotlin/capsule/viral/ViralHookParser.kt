package capsule.viral

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Pure parser of the viral hook JSON blob returned by the LLM (CAP-VIRAL US-4).
 *
 * The LLM is asked (via [ViralHookPromptBuilder]) to return a strict JSON object
 * with fields `hook`, `message`, `cta`. Real models wrap JSON in prose or a
 * ```json fence, so the parser extracts the first balanced JSON object before
 * parsing — the same tolerance the content-plan path has.
 *
 * Degradation (economy of ink): a blank / malformed / incomplete response never
 * throws. [parse] returns a [ViralHook] built from whatever is present; when
 * nothing usable is found it returns `null` so the caller can fall back to the
 * deck's own content.
 *
 * Pure — no Gradle, no LLM, no I/O. Pattern: `capsule.pipeline.ContentPlanValidator`
 * (Jackson, sealed/tolerant result).
 */
object ViralHookParser {

    private val jsonMapper: ObjectMapper = ObjectMapper()

    /**
     * Parses [raw], the LLM response, into a [ViralHook].
     *
     * @param raw the LLM response (may carry prose/fences around the JSON).
     * @return the parsed [ViralHook], or `null` when no usable content is found.
     */
    fun parse(raw: String): ViralHook? {
        if (raw.isBlank()) return null
        val json = extractJsonObject(raw) ?: return null
        val root: JsonNode = try {
            jsonMapper.readTree(json)
        } catch (_: Exception) {
            return null
        }
        if (root.isMissingNode || !root.isObject) return null

        val hook = root.get("hook")?.asText()?.trim().orEmpty()
        val message = root.get("message")?.asText()?.trim().orEmpty()
        val cta = root.get("cta")?.asText()?.trim().orEmpty()

        if (hook.isBlank() && message.isBlank() && cta.isBlank()) return null
        return ViralHook(hook = hook, message = message, cta = cta)
    }

    /**
     * Extracts the first balanced `{ ... }` JSON object from [text], skipping
     * any surrounding prose or code fences. Returns `null` when none is found.
     */
    internal fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
