package capsule.viral

/**
 * Immutable market copy for one (language × platform) cell (CAP-VIRAL US-9).
 *
 * Read — never generated — by capsule from the copy produced by bakery
 * (GBL-006 `CopyPiece`: `seedRef` × `market` × `platform` → `hook`, `title`,
 * `caption`, `hashtags`). Boundary actée : **bakery écrit, capsule consomme**
 * (split-brain eliminated — capsule never re-generates the metadata).
 *
 * Invariant: at least one of [hook]/[title]/[caption] must be non-blank.
 *
 * @property language target ISO language code.
 * @property platform target platform preset.
 * @property hook     platform hook (may be blank).
 * @property title    platform title (may be blank).
 * @property caption  platform caption (may be blank).
 * @property hashtags platform hashtags (may be empty).
 */
data class MarketCopy(
    val language: String,
    val platform: ViralPlatform,
    val hook: String,
    val title: String,
    val caption: String,
    val hashtags: List<String> = emptyList(),
) {
    init {
        require(language.isNotBlank()) { "MarketCopy.language must not be blank" }
        require(hook.isNotBlank() || title.isNotBlank() || caption.isNotBlank()) {
            "MarketCopy must carry at least one of hook/title/caption"
        }
    }
}
