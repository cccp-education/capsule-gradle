package capsule

/**
 * Configuration section for strict mode (CAP-CR3-2).
 *
 * When enabled, the plugin fails the build instead of falling back to a
 * NoOp engine when a required tool (Piper, espeak, Playwright, ffmpeg,
 * manim) is not available on the system. This prevents the "false
 * positive" where `check` is green but the produced video is a
 * placeholder.
 *
 * Default is disabled to preserve backward compatibility.
 *
 * @param enabled `true` to fail-fast on missing tools, `false` (default)
 *        to fall back to NoOp engines with a warning.
 */
data class StrictModeConfig(
    val enabled: Boolean = false
)