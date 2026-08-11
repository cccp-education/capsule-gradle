package capsule.audio

/**
 * Thrown when audio post-production fails critically (CAP-AUDIO US-2).
 *
 * Pattern mirrors [capsule.BurnInException] — used by
 * [AudioPostProcessorImpl] to signal unrecoverable ffmpeg errors.
 * Degraded-mode failures (BGM file missing, ffmpeg unavailable) do
 * *not* throw — they return `false` so the caller keeps the original
 * video (backward compatibility, pattern [capsule.VideoFormatConverter]).
 */
class AudioPostException(message: String) : RuntimeException(message)