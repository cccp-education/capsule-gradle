<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Guide du consommateur

> Plugin Gradle qui transforme un deck reveal.js en capsule vidéo synchronisée — voix off TTS, capture, sous-titres, conversion de format, audio post et une chaîne court/viral — sans post-production humaine.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Licence](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.10` (Maven Central) · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.capsule`
- **Prérequis** : Java 25+ · Gradle 9.8+
- **Docs** : [internes du plugin](../capsule-plugin/README.md) · [architecture du pipeline](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 Langues : [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | **Français** | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Ce que fait le plugin

`capsule-gradle` consomme un deck reveal.js (produit par `slider-gradle`) et produit une
capsule vidéo synchronisée — une voix off TTS par slide, capture vidéo, sous-titres,
conversion de format, post-production audio, contrôle qualité et une chaîne verticale
court/viral optionnelle.

```
Source AsciiDoc → slider-gradle → deck reveal.js + script speaker
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → injection audio → capture → WebM/MP4
                                        ↓                          ↓
                              animation Manim (optionnel)   recadrage 9:16 FFmpeg (optionnel)
```

Fait partie de l'écosystème multi-plugins CCCP Education (`groupId: education.cccp`).

## Prérequis

- **Java** 25+ (toolchain Kotlin 2.4.10)
- **Gradle** 9.8+
- **Piper** — TTS local hors-ligne (moteur par défaut)
- **espeak / espeak-ng** — TTS de repli (`espeak-ng` est utilisé automatiquement si `espeak` est absent)
- **Fish Audio** — TTS cloud optionnel avec **voix clonée** (moteur `fish`) ; le pool de clés vit dans un `fish.yaml` ignoré par git
- **Chromium** — téléchargé automatiquement par Playwright au premier lancement
- **FFmpeg** — recadrage 9:16, burn-in des sous-titres, conversion MP4, audio post
- **Manim** — optionnel, pour les slides mathématiques animés
- **Node** — optionnel, pour la stratégie de capture/animation Remotion

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.10"
}
```

### 2. Configurer

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (défaut), "espeak", ou "fish" (Fish Audio cloud, voix clonée)
}
```

### 3. Générer la vidéo

```bash
./gradlew generateCapsuleVideo
```

Cela exécute tout le pipeline : validation du script → synthèse TTS → injection audio dans
le HTML du deck → capture headless → sortie vidéo.

### Pipeline complet avec slider

```gradle
plugins {
    alias(libs.plugins.slider)
    alias(libs.plugins.capsule)
}

tasks.named("generateCapsuleVideo") {
    dependsOn("asciidoctorRevealJs", "asciidocCapsule")
}
```

```bash
./gradlew generateCapsuleVideo    # deck + script slider → capsule vidéo
```

## Tâches disponibles

| Tâche | Groupe | Description |
|-------|--------|-------------|
| `generateCapsuleScript` | generate | Lit le `*-script.txt` produit par slider et valide le script capsule |
| `generateCapsule` | generate | Synthétise les fichiers audio TTS (Piper/espeak), un par slide |
| `generateCapsuleVideo` | generate | Injecte l'audio TTS dans le HTML du deck puis capture la vidéo |
| `generateCapsuleVideoAllLanguages` | generate | Une capsule localisée par langue cible (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | Analyse les notes speaker AsciiDoc en contrat `*-script.txt` texte |
| `translateAndExtractSpeakerNotes` | capsule | Composite : `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | Composite : `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | Pipeline LLM (koog) : propose-context → validation → notes speaker + script TTS |
| `generateCapsuleContentAndVideos` | capsule | Composite : `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | Collecte le contexte augmenté (gouvernance + RAG + Docs + scénario + glossaire) |
| `capsuleAiSmokeTest` | generate | Test de fumée du pont LLM de codebase avec un prompt minimal |
| `generateCapsuleTranscript` | generate | Produit un article transcript AsciiDoc à partir des notes speaker |
| `generateCapsuleChapters` | generate | Marqueurs de chapitres + chapitres texte YouTube + description |
| `generateCapsulePodcast` | generate | Podcast audio seul (narration MP3 concaténée) |
| `deployCapsule` | deploy | Recadre la capsule en vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `distributeCapsuleVideo` | distribute | Copie les vidéos finales dans un répertoire de destination versionné |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | Exporte / analyse le JSON de contexte capsule (contrat moteur N3) |
| `scaffoldCapsuleContext` | generate | Génère un `capsule-context.yml` par défaut commenté |
| `validateCapsuleVideoDuration` | verification | Sonde la durée vidéo face à la somme des durées audio TTS (opt-in) |
| `preflightCapsuleContent` | verification | Pré-vol du contenu avant le TTS facturé (opt-in) |
| `renderQcBoards` | verification | Rend les planches QC (beats + « dit vs montré ») (opt-in) |
| `verifyCapsuleVoice` | verification | QC voix par ASR — fidélité, souffle, choix de prise (opt-in) |
| `selectBestVoiceTake` | verification | Sélectionne la meilleure prise parmi les candidates d'une slide (opt-in) |
| `generateViralCampaign` | generate | Storyboard → campagne court (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | Rend les variantes virales, vignettes, sous-titres karaoké (opt-in) |
| `validateViralCampaign` | verification | Vérifie le manifeste face aux fichiers produits (opt-in) |
| `normalizeViralStoryboard` | generate | Complète les transitions manquantes du storyboard (aide à l'authoring) |

> Les capacités optionnelles (*opt-in*) sont **désactivées par défaut** (`false`) et ne
> bloquent jamais le pipeline principal.

## Configuration

Chaque valeur est résolue depuis **4 sources** (précédence **CLI > YAML > gradle.properties > ENV**) :

```bash
./gradlew scaffoldCapsuleContext          # génère un capsule-context.yml commenté
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

DSL représentatif (voir le [guide des internes](../capsule-plugin/README.md) pour la
`CapsuleConfig` complète à 18 sections) :

```gradle
capsule {
    // Entrée
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (défaut) ou "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 langues (LanguageCatalog)

    // Capture / sortie
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (défaut) | "mp4" | "both"
    outputBt709.set(false)                 // étiquette le MP4 en BT.709 (opt-in)

    // Sous-titres
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" ou "vtt"
    subtitleBurnIn.set(false)

    // Distribution
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (optionnel)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## Pipeline multi-langue

Un deck source → une capsule localisée par langue cible. Le pipeline consomme les decks
traduits produits par `slider.translateDeck` (**22 langues**, `LanguageCatalog` du contrat N0
`i18n-contracts`) et résout la voix TTS par langue via le domaine pur `capsule.multilang`
(`MultiLanguageResolver` + `VoiceMapping`).

```
deck source (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 langues)
<deckName>_<lang>-deck.html (un par langue)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (un par langue)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (une capsule localisée par langue)
```

Convention de nommage (contrat de frontière, aligné sur slider) :

- deck traduit : `<deckName>_<lang>-deck.html`
- script speaker : `<deckName>_<lang>-script.txt`
- vidéo localisée : `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # une vidéo par langue avec paire deck+script
./gradlew translateAndGenerateCapsuleVideos  # composite complet : translate → extract → vidéos
```

Une langue sans paire deck + script correspondante est silencieusement ignorée, et la règle
d'**Économie d'Encre** s'applique : une langue dont la vidéo existe déjà et sonde valide
n'est jamais re-rendue.

## Fonctionnalités optionnelles

- **Format de sortie** — `outputFormat = "mp4"` (ou `"both"`) produit un MP4/H.264 en plus du
  WebM natif ; `outputBt709 = true` l'étiquette en BT.709 (colorimétrie broadcast).
- **Post-production audio** — musique de fond, loudness EBU R128 (mesure 2 passes) et ducking
  sidechain (`audioPost { … }`).
- **Sous-titres** — génération SRT/VTT et burn-in (`subtitleEnabled`, `subtitleBurnIn`).
- **Contrôle qualité (opt-in)** — pré-vol du contenu (`preflightCapsuleContent`), intégrité
  vidéo par décodage, timeline déterministe, planches QC (`renderQcBoards`) et QC voix par ASR
  (`verifyCapsuleVoice`, `selectBestVoiceTake`).
- **Transcript** — pipeline inverse : un article AsciiDoc à partir des notes speaker
  (`TEMPLATE` ou `LLM`).
- **Podcast** — MP3 audio seul de la narration.
- **Preview** — dry-run (`previewOnly = true`) : ne rend que les PNG de slides, saute le mux.
- **Viral (opt-in)** — un storyboard validable → campagne court verticale 9:16 avec hooks/CTA
  ancrés sur le contexte augmenté, vignettes, sous-titres karaoké et un `CampaignBundle`.

### Rythme des slides (capture Remotion)

Avec `captureStrategy = "remotion"`, le deck est animé plutôt que filmé : chaque bloc d'une
slide arrive selon un rythme de lecture au lieu que toute la slide apparaisse d'un coup.
Beats par défaut (secondes depuis le début de la slide) : `.kicker` 0.0 · `h1`/`h2` 0.15 ·
`.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 · `.statlabel` 5.0. `.brand`, `.num` et `.grid` sont
du chrome (présents dès la première image). Tout bloc peut fixer son beat avec `data-at` :

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## Build & test (contributeurs)

```bash
./gradlew build                    # build complet
./gradlew check                    # unitaires + fonctionnels + Cucumber
./gradlew test                     # unitaires JUnit5 seulement
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # publication locale
```

## Dépannage

| Symptôme | Correctif |
|----------|-----------|
| Chromium Playwright introuvable | lancez une fois pour le télécharger, ou définissez `chromiumExecutablePath` |
| Piper TTS ne produit aucun audio | vérifiez `piperExecutablePath`, assurez-vous que le modèle de voix est installé |
| `deployCapsule` échoue | vérifiez que FFmpeg est dans le `PATH` (`ffmpeg -version`) |
| Slides Manim non rendues | mettez `manimExecutablePath` à `noop` pour tester sans Manim |
| Timeout de capture vidéo | augmentez `playwrightTimeout` (défaut 120000 ms) |
| Mémoire insuffisante | `export GRADLE_OPTS="-Xmx2g"` |

## Documentation

- [capsule-plugin/README.md](../capsule-plugin/README.md) — internes du plugin (guide développeur & contributeur)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — architecture du pipeline (PlantUML)

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Fait partie de l'écosystème CCCP Education — `groupId: education.cccp`._
