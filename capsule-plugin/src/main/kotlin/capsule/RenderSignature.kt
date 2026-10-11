package capsule

import java.security.MessageDigest

/**
 * Content fingerprint of a captured clip (PRT-06).
 *
 * A captured video is reused only when the inputs that produced it are
 * unchanged — the capture engine, the deck/narration content, and the timeline.
 * Ported from `prepa_electricite_complet/build.py`, which signs each clip with
 * `sha1(engine + scene + timeline)`: "un rendu d'avant une correction passait
 * pour à jour" was exactly the bug the signature fixed.
 *
 * Pure — no I/O. The caller supplies the three fingerprints as strings.
 */
object RenderSignature {

    /** Length of the returned hexadecimal signature. */
    const val LENGTH: Int = 16

    /**
     * @param engineFingerprint   the capture engine (e.g. `screenshot`, `remotion`).
     * @param contentFingerprint  the deck + narration content.
     * @param timelineFingerprint the timing that shapes the clip.
     * @return a stable, truncated SHA-256 hex signature (lowercase hex).
     */
    fun of(engineFingerprint: String, contentFingerprint: String, timelineFingerprint: String): String {
        val payload = listOf(engineFingerprint, contentFingerprint, timelineFingerprint)
            .joinToString("\u0000")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(LENGTH)
    }
}
