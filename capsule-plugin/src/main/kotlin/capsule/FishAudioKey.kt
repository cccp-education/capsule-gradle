package capsule

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * CAP-FISH-AUDIO — one entry of the `fish-audio` key pool.
 *
 * The pool is the value of the `fish-audio` member of [CapsuleConfig], loaded
 * from the git-ignored `fish.yaml` (see [CapsuleConfigLoader.loadFishAudio]).
 * The API key is a **secret**: it must never be committed, logged or serialised
 * in clear. [toString] is therefore redacted so that a data-class rendering
 * (`CapsuleConfig.toString()`) or a context serialisation stays safe.
 *
 * `@JsonIgnoreProperties(ignoreUnknown = true)` keeps the model forward
 * compatible: a future `voice`/`model` field on an entry is ignored, not fatal.
 *
 * @property key the Fish Audio API key (must not be blank).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class FishAudioKey(
    val key: String,
) {
    init {
        require(key.isNotBlank()) { "fish-audio key must not be blank" }
    }

    /** Redacted rendering — the raw [key] never appears in a log or a `toString()`. */
    override fun toString(): String = "FishAudioKey(key=***)"
}
