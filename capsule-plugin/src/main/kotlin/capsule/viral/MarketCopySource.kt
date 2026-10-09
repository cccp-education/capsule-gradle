package capsule.viral

/**
 * Domain port — reads market copy produced by bakery (CAP-VIRAL US-9).
 *
 * Capsule **consume** le copy natif par marché (GBL-006), il ne le régénère
 * jamais. The port returns the [MarketCopy] for a (language × platform) cell,
 * or `null` when no copy is available (degraded: the campaign falls back to the
 * hook the context-anchored generator produced).
 *
 * Synchronous, Gradle-free contract (pattern [capsule.transcript.TranscriptLlmEnhancer])
 * so the domain is unit-testable with a plain fake. The file adapter that reads
 * bakery's `CopyPiece` JSON lives in [FileMarketCopySource].
 */
interface MarketCopySource {

    /**
     * Reads the market copy for [language] × [platform].
     *
     * @param language ISO language code.
     * @param platform target platform preset.
     * @return the [MarketCopy], or `null` when unavailable (degraded).
     */
    fun read(language: String, platform: ViralPlatform): MarketCopy?

    /** `true` when the source can serve at least one copy. */
    fun isAvailable(): Boolean
}
