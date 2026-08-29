package capsule.anim

/**
 * Pure timing logic ported from the Remotion composition `Capsule.jsx` (PR #2).
 *
 * The composition drives every visual beat from the frame number — entrance
 * transitions, the slow drift and cross-fades between slides. Those constants
 * and formulas were shipped in JavaScript with no automated test: a silent
 * regression of `BEATS_SECS`, `CONTENT_GATE` or the cross-fade overlap would
 * only ever show up in the rendered video. This object mirrors the exact
 * computations in JVM-testable Kotlin so they can be pinned by unit tests and
 * Cucumber scenarios (EPIC CAP-ANIM-TEST).
 *
 * Nothing in this object touches React, Node or FFmpeg: it is the domain kernel
 * of the animation timing, kept free of any runtime so it can run anywhere.
 */
object RemotionTiming {

    /** Pose duration of a block, in seconds (see Capsule.jsx `REVEAL_SECS`). */
    const val REVEAL_SECS: Double = 0.55

    /** Cross-fade from one slide to the next, in frames (`CROSSFADE_FRAMES`). */
    const val CROSSFADE_FRAMES: Int = 20

    /**
     * Fraction of the cross-fade elapsed before the incoming slide's text
     * appears. The background has already covered the outgoing text by then
     * (`CONTENT_GATE`).
     */
    const val CONTENT_GATE: Double = 0.72

    /** Upper bound of a Manim clip speed-up (`MAX_SPEEDUP`). */
    const val MAX_SPEEDUP: Double = 1.25

    /** Lower bound of a Manim clip slow-down (`MIN_SLOWDOWN`). */
    const val MIN_SLOWDOWN: Double = 0.8

    /**
     * Default beat (seconds) when a block has neither `data-at` nor a known
     * selector. Mirrors the `--d: 1.6` default in `REVEAL_CSS`.
     */
    const val DEFAULT_BEAT_SECS: Double = 1.6

    /** Beats (seconds since slide start) for the named block selectors. */
    val BEATS_SECS: Map<String, Double> = mapOf(
        "rule" to 0.5,
        "lede" to 1.0,
        "stat" to 4.5,
        "statlabel" to 5.0,
    )

    /**
     * Blocks carried by the cross-fade itself (no own beat): they appear with
     * the slide background, never on their own fade.
     */
    val CARRIED_BY_TRANSITION: List<String> = listOf(
        "brand", "num", "grid", "kicker", "h1", "h2",
    )

    /** Last beat of the scale, used to compress shorter slides. */
    val LAST_BEAT_SECS: Double = BEATS_SECS.values.max() + REVEAL_SECS

    /** Cubic ease-out, identical to the JSX `EASE_OUT`. */
    fun easeOut(t: Double): Double {
        val c = t.coerceIn(0.0, 1.0)
        return 1.0 - (1.0 - c) * (1.0 - c) * (1.0 - c)
    }

    /** Linear interpolation with clamping, mirroring Remotion `interpolate`. */
    fun interpolate(
        value: Double,
        x0: Double,
        x1: Double,
        y0: Double,
        y1: Double,
    ): Double = when {
        value <= x0 -> y0
        value >= x1 -> y1
        else -> y0 + (value - x0) * (y1 - y0) / (x1 - x0)
    }

    /**
     * Beat (seconds since slide start) at which [blockClass] poses.
     *
     * - Carried blocks appear immediately (beat 0).
     * - Known selectors resolve through [BEATS_SECS].
     * - Anything else falls back to [DEFAULT_BEAT_SECS].
     */
    fun beatSeconds(blockClass: String): Double =
        when {
            blockClass in CARRIED_BY_TRANSITION -> 0.0
            BEATS_SECS.containsKey(blockClass) -> BEATS_SECS.getValue(blockClass)
            else -> DEFAULT_BEAT_SECS
        }

    /**
     * Rewrites `data-at="7.5"` into `style="--d: 7.5;"` on the matching block.
     *
     * Port of the `applyCues` regex from `Capsule.jsx`: CSS cannot read an
     * attribute value, so the cue is converted to a custom property on the
     * markup before injection. The original `style` attribute, if present, is
     * preserved and the declaration is merged in.
     */
    fun applyCues(html: String): String {
        val pattern = Regex("""<([a-zA-Z][\w-]*)([^>]*?)\sdata-at="([\d.]+)"([^>]*)>""")
        return html.replace(pattern) { match ->
            val (tag, before, at, after) = match.destructured
            val decl = "--d: ${at.toDouble()};"
            val all = "$before$after"
            val styled = if ("""\sstyle="""".toRegex().containsMatchIn(all)) {
                all.replaceFirst("""\sstyle="""".toRegex(), """ style="$decl""")
            } else {
                """$all style="$decl""""
            }
            "<$tag$styled>"
        }
    }

    /**
     * Playback rate applied to a Manim clip so it ends with its slide.
     *
     * Bounded by [MAX_SPEEDUP]/[MIN_SLOWDOWN]; a missing clip (<= 0 frames)
     * plays at natural speed (1.0). Mirrors `Animation.playbackRate`.
     */
    fun clipPlaybackRate(clipDurationInFrames: Int, durationInFrames: Int): Double {
        if (clipDurationInFrames <= 0) return 1.0
        val raw = clipDurationInFrames.toDouble() / durationInFrames.toDouble()
        return raw.coerceIn(MIN_SLOWDOWN, MAX_SPEEDUP)
    }

    /**
     * Compression factor for a slide shorter than the beat scale: its cues are
     * pulled in instead of never posing. Mirrors `Slide.compress`.
     */
    fun compress(slideSecs: Double): Double =
        if (slideSecs >= LAST_BEAT_SECS) 1.0 else slideSecs / LAST_BEAT_SECS

    /**
     * Background opacity fraction of a slide at [frame] within its sequence.
     *
     * First slide is fully shown; others fade in over [CROSSFADE_FRAMES].
     * Mirrors `Slide.appear` fed through `EASE_OUT`.
     */
    fun backgroundAppear(frame: Int, crossfadeFrames: Int = CROSSFADE_FRAMES, first: Boolean): Double {
        if (first) return 1.0
        val t = interpolate(frame.toDouble(), 0.0, crossfadeFrames.toDouble(), 0.0, 1.0)
        return easeOut(t)
    }

    /**
     * Text opacity fraction of a slide at [frame] within its sequence.
     *
     * The text only enters once the background has covered the outgoing slide
     * (past [CONTENT_GATE]), so two titles never overlap mid-fade. Mirrors
     * `Slide.contentAppear`.
     */
    fun contentAppear(
        frame: Int,
        crossfadeFrames: Int = CROSSFADE_FRAMES,
        contentGate: Double = CONTENT_GATE,
        first: Boolean,
    ): Double {
        if (first) return 1.0
        val appear = interpolate(frame.toDouble(), 0.0, crossfadeFrames.toDouble(), 0.0, 1.0)
        val t = interpolate(appear, contentGate, 1.0, 0.0, 1.0)
        return easeOut(t)
    }

    /**
     * Sequence length of a slide: its own duration plus the cross-fade overlap
     * with the next slide (except for the last one). Mirrors the `Sequence`
     * `durationInFrames` in `Capsule`.
     */
    fun sequenceDurationInFrames(
        durationInFrames: Int,
        isLast: Boolean,
        crossfadeFrames: Int = CROSSFADE_FRAMES,
    ): Int = durationInFrames + if (!isLast) crossfadeFrames else 0

    /**
     * Background opacity of the slide that starts at [start] at the absolute
     * [frame]. Used by the integration test to prove the screen is never black
     * during a transition.
     */
    fun backgroundOpacityAt(
        frame: Int,
        start: Int,
        durationInFrames: Int,
        crossfadeFrames: Int = CROSSFADE_FRAMES,
        first: Boolean,
    ): Double {
        val local = frame - start
        if (local < 0) return 0.0
        return backgroundAppear(local, crossfadeFrames, first)
    }
}
