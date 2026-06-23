<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Guide Consommateur

> Plugin Gradle de génération automatique de capsules vidéo pédagogiques depuis des decks reveal.js.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Licence](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.1-SNAPSHOT` (non publié) · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.capsule`
- **Build** : `./gradlew build` · **Tests** : `./gradlew check` (195 unitaires + 22 Cucumber PASS)
- **Couverture** : rapports Kover XML + HTML intégrés à `check`

🌐 Langues : [English](README.md) | **Français**

---

## Ce que ça fait

`capsule-gradle` transforme un deck reveal.js (produit par `slider-gradle`) en une
capsule vidéo pédagogique avec narration TTS synchronisée — sans post-production.

Le pipeline orchestre quatre outils externes :

```
Source AsciiDoc → slider-gradle → deck reveal.js + script capsule
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → injection audio → capture Playwright → vidéo WebM
                                      ↓                              ↓
                              Animation Manim (optionnel)     Distribution FFmpeg (9:16)
```

Partie de l'écosystème multi-plugins CCCP Education (`groupId: education.cccp`).

## Démarrage rapide

### 1. Appliquer le plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. Configurer la capsule

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. Générer la vidéo

```bash
./gradlew generateCapsuleVideo
```

Ceci exécute le pipeline complet : validation du script → synthèse TTS → injection audio
dans le deck HTML → capture Playwright headless → sortie WebM.

### Pipeline complet avec slider

Le `build.gradle.kts` racine câble slider et capsule ensemble :

```gradle
plugins {
    alias(libs.plugins.slider)
    alias(libs.plugins.capsule)
}

tasks.named("capsulevideo") {
    dependsOn("asciidoctorRevealJs", "asciidocCapsule")
}
```

```bash
./gradlew capsulevideo    # deck slider + script → vidéo capsule
```

## Tâches disponibles

| Tâche | Groupe | Description |
|------|--------|-------------|
| `generateCapsuleScript`   | generate  | Lit les `*-script.txt` de slider et valide le script capsule |
| `generateCapsule`          | generate  | Génère les fichiers audio TTS depuis les scripts (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | Injecte l'audio TTS dans le deck HTML puis capture la vidéo via Playwright |
| `deployCapsule`            | deploy    | Recadre les capsules en format vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `collectCapsuleContext`    | collect   | Exporte le contexte des capsules (chemins vidéos + métadonnées) en JSON pour le moteur N3 |
| `transformCapsuleContext`  | transform | Parse `capsule-context.json` et retourne une liste de decks |
| `collectCapsuleRetrieve`   | collect   | Récupère les decks depuis le JSON de contexte (contrat moteur N3) |
| `scaffoldCapsuleContext`   | generate  | Génère un fichier `capsule-context.yml` par défaut avec commentaires |

## DSL d'extension

```gradle
capsule {
    // Entrée
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" ou "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // Capture
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // Sous-titres
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" ou "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" ou "top"

    // Distribution
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Animation Manim
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### Configuration multi-source

La configuration est résolue depuis 4 sources (priorité : CLI > YAML > gradle.properties > ENV) :

```bash
./gradlew scaffoldCapsuleContext          # générer capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## Prérequis

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+
- **Piper** (TTS, local offline) — moteur par défaut
- **espeak** (TTS de secours)
- **Chromium** (téléchargé automatiquement par Playwright au premier lancement)
- **FFmpeg** (pour `deployCapsule` recadrage 9:16 et burn-in des sous-titres)
- **Manim** (optionnel, pour les slides d'animation mathématique)

## Build et tests

```bash
./gradlew build                    # build complet
./gradlew check                    # tests unitaires + fonctionnels + Cucumber
./gradlew test                     # tests unitaires JUnit5 (exclut @integration)
./gradlew cucumberTest             # tests BDD Cucumber
./gradlew functionalTest           # tests fonctionnels Gradle TestKit
./gradlew publishToMavenLocal      # publier localement
```

## Dépannage

| Symptôme | Solution |
|----------|----------|
| Chromium Playwright introuvable   | exécuter une fois pour auto-téléchargement, ou définir `chromiumExecutablePath` |
| Piper TTS ne produit pas d'audio  | vérifier `piperExecutablePath`, s'assurer que le modèle de voix est installé |
| `deployCapsule` échoue            | vérifier que FFmpeg est sur le PATH (`ffmpeg -version`) |
| Slides Manim non rendues          | définir `manimExecutablePath` à `noop` pour tester sans Manim |
| Timeout de capture vidéo          | augmenter `playwrightTimeout` (défaut 120000ms) |
| Mémoire insuffisante              | `export GRADLE_OPTS="-Xmx2g"` |

Voir [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) pour les détails du pipeline.

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._