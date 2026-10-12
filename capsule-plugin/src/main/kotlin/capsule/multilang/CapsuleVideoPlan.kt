package capsule.multilang

import capsule.kernel.VideoProductionEntry
import capsule.kernel.VideoProductionPlan
import java.io.File

/**
 * Backward-compatible alias — a `CapsuleVideoEntry` **is** a
 * [VideoProductionEntry] (CAP-ENGINE-DECOUPLING US-1). The former renderer-leaking
 * `deckFile: File` is now the neutral [capsule.kernel.VisualSource]. Prefer the
 * neutral type in new code.
 */
typealias CapsuleVideoEntry = VideoProductionEntry

/**
 * Backward-compatible alias — a `CapsuleVideoPlan` **is** a
 * [VideoProductionPlan] (CAP-ENGINE-DECOUPLING US-1). Prefer the neutral type in
 * new code.
 */
typealias CapsuleVideoPlan = VideoProductionPlan

/**
 * Legacy deck-file accessor — the neutral field is
 * [VideoProductionEntry.visual].
 */
@Deprecated(
    "Neutralized vocabulary: use VideoProductionEntry.visual",
    ReplaceWith("visual.file"),
)
val VideoProductionEntry.deckFile: File get() = visual.file
