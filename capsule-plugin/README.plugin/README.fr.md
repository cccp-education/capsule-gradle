<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Internes du Plugin

> Guide développeur et contributeur pour le plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Couverture](https://img.shields.io/static/v1?label=couverture&message=Kover&color=blue)]()
[![Licence](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licence)](../../LICENSE)

- **Version** : `0.0.10` (Maven Central) · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.capsule`
- **Toolchain** : Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **Build** : `./gradlew build -x test` · **Tests** : `./gradlew check` (unitaires + fonctionnels via TestKit ; Cucumber via `-PrunCucumber`) · **Couverture** : `./gradlew koverReport`

🌐 Langues : [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | **Français** | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Ce que fait le plugin

`capsule-gradle` transforme un deck reveal.js (produit par `slider-gradle`) en une
capsule vidéo synchronisée — voix off TTS par slide, capture vidéo, sous-titres,
conversion de format, post-production audio, contrôle qualité et pipeline
vertical court/viral — sans post-production humaine. C'est un plugin Gradle N2
(consommé par le runner N3, jamais N3 lui-même).

## Structure du module

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # catalogue de versions interne
├── doc/CAPSULE_ARCHITECTURE.adoc        # architecture du pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # point d'entrée — applique slider, enregistre les tâches, fusionne la config
    │   ├── CapsuleManager.kt            # enregistrement des tâches + fabriques de moteurs (resolve*)
    │   ├── CapsuleModels.kt             # DSL CapsuleExtension + CapsuleConventions
    │   ├── CapsuleConfig.kt             # config immuable — 18 sections
    │   ├── CapsuleConfigLoader.kt       # chargeur YAML avec résolution d'env ${VAR}
    │   ├── CapsuleConfigMerger.kt       # fusion 4 sources (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # journalisation structurée de la config (CR-8)
    │   ├── Capsule{Messages}*.kt        # résolution des ressources i18n (10 langues)
    │   ├── TtsManager.kt                # intégration TTS Piper + espeak + Fish Audio
    │   ├── PlaywrightManager.kt         # capture Chromium headless
    │   ├── CaptureResolver.kt           # dispatching PLAYWRIGHT / SCREENSHOT / REMOTION
    │   ├── ScreenshotPlanner.kt         # plan pur : PNG par slide → WebM → concat demuxer
    │   ├── Remotion*.kt                 # stratégie de capture Remotion (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # enrichissement Manim
    │   ├── Mp4ConversionCommand.kt      # argv FFmpeg pur pour WebM→MP4 (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # transcodage WebM → MP4/H.264 (CAP-MP4)
    │   ├── FormatConversion.kt          # dispatching WEBM / MP4 / BOTH
    │   ├── VideoDestinationResolver.kt  # destination versionnée (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # conversion WAV→MP3
    │   ├── MediaProbeUtil.kt            # sondage de durée via ffprobe
    │   ├── RenderSignature.kt           # signature de rendu + réutilisation .sig (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # pont LLM vers le LlmBuildService de codebase (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # portage du timing Remotion (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO : AudioPostCommand (argv pur), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # marqueurs de chapitres, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # contexte augmenté : CapsuleContext(Builder), loaders Doc/Scenario/Glossary, ProvenanceTracker
    │   ├── feed/          # contrat d'extraction des notes speaker (SLD-11) + registrars composites
    │   ├── i18n/          # CapsuleMessages (bundles 10 langues)
    │   ├── multilang/     # CAP-29 : MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), runner AllLanguages
    │   ├── pipeline/      # CAP-ARCH-3 : graphe koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST : PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — PNG uniquement)
    │   ├── publish/       # VideoDescriptionBuilder (garde YouTube)
    │   ├── qc/            # CAP-QC : ContentPreflight (PRT-05), QcBoard/planificateurs + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07 : timeline déterministe (images entières, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT : TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL : storyboard, deck builder, campaign bundle/gate, thumbnail, karaoké
    │   └── voice/         # CAP-QC-VOICE : TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (port), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 langues + base
    │   └── remotion/                        # composition Remotion (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # définitions de steps Cucumber + runners par feature
    │   └── **/*Test.kt                      # tests unitaires
    ├── test/features/                       # *.feature — scénarios Cucumber (44 fichiers)
    └── functionalTest/kotlin/capsule/       # tests fonctionnels Gradle TestKit
```

## Tâches Gradle

| Groupe | Tâches |
|--------|--------|
| Pipeline principal | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (injection audio + capture), `generateCapsuleVideoAllLanguages`, `deployCapsule` (recadrage 9:16), `distributeCapsuleVideo` (destination versionnée) |
| Contenu LLM | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| Contexte | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| Sortie | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| Qualité (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| Viral (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| Composite feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## Dépendances

`capsule-gradle` consomme `slider-gradle` (`education.cccp:slider:0.0.19`) en `compileOnly` —
un contrat par fichiers (capsule lit le HTML du deck + le texte du script, ne les modifie jamais).
`CapsulePlugin` applique automatiquement `education.cccp.slider` au runtime si disponible.

Depuis CAP-ARCH-1, capsule consomme aussi le socle LLM de codebase
(`education.cccp:codebase-plugin`) en `implementation` — le pont `LlmBuildService`
fournit le `ChatModel` utilisé par `generateCapsuleContent`, avec un repli mock-LLM via
`-Pollama.baseUrl` pour les tests (zéro réseau, zéro pool).

| Bibliothèque | Version | Rôle |
|--------------|---------|------|
| **Playwright** | 1.52.0 | Chromium headless — pilotage reveal.js + capture vidéo (JVM natif, zéro npm) |
| **koog-agents** | 1.3.0 | DSL Kotlin d'orchestration d'agents (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | Adaptateurs `ChatModel` (pont LLM, enrichisseur transcript) — version via BOM |
| **Jackson** | 2.21.2 | Config YAML + sérialisation JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Rapports de couverture (XML + HTML, câblés dans `check`) |
| **Cucumber** | 7.34.3 | Tests BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | DSL du plugin |
| **codebase-plugin** | 0.0.17 | Socle LLM N1 — pont `LlmBuildService` (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | Contrats N0 — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | Contrats N0 — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — deck reveal.js + source du script capsule |

Outils externes (pas des dépendances Maven) : **Piper** (TTS local hors-ligne, défaut),
**espeak-ng** (moteur TTS de repli), **FFmpeg** (recadrage, burn-in, MP4, audio post),
**Manim** (schémas animés optionnels, `[manim:SceneName]`), **Chromium** (téléchargé
automatiquement par Playwright au premier lancement), **Node** (stratégie Remotion).

## Configuration

`CapsuleConfig` comporte **18 sections** : `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`.

Chaque valeur est résolue depuis **4 sources** (précédence **CLI > YAML > gradle.properties > ENV**) :

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

Les nouvelles capacités sont **opt-in (`false` par défaut)** pour la compatibilité ascendante.

## Matrice de tests

| Tâche | Portée | Détails |
|-------|--------|---------|
| `test` | Tests unitaires JUnit5 | Exclut `capsule.scenarios.**`. Les tests `@integration` ne sont *pas* filtrés : ils s'auto-skippent quand l'outil externe (Chromium, espeak, FFmpeg, Manim) est absent |
| `cucumberTest` | Cucumber BDD | features sous `src/test/features` (44 fichiers `.feature`), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g. Skippé sauf `-PrunCucumber` ou `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | Tests fonctionnels d'application du plugin |
| `check` | Tout | Dépend de `test` + `cucumberTest` + `functionalTest` |

Tags Cucumber : `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`. **38** tâches `cucumberTest*` dédiées isolent une feature chacune (`CucumberTaskSpec` dans `build.gradle.kts`).

Base de tests : le vert de référence est le workflow CI `Full Tests` sur `main`
(unitaires + TestKit fonctionnel + Cucumber agrégé/rendu réel ; le `cucumberTest`
de diagnostic est skippé par défaut depuis CR-10).

## Pipeline de contenu LLM (CAP-ARCH-3)

`generateCapsuleContent` orchestre le `CapsulePipelineGraph` koog en une invocation :

```
propose-context → validate-context → generate-speaker-notes
```

- **Entrées** : deck source `.adoc` (`-Pdeck.file=<chemin>` ou premier `.adoc` de `slides/misc`),
  contexte augmenté optionnel de `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`), langue cible (`-Pdeck.language`, défaut `fr`).
- **Sorties** (déclarées → Économie d'Encre via UP-TO-DATE) : notes speaker enrichies
  `build/capsule/<deckName>-speaker-notes.adoc` + script TTS `build/capsule/<deckName>-script.txt`.
- **LLM** : résolu via le `LlmBuildService` de codebase ; les tests injectent un serveur Ollama
  mocké avec `-Pollama.baseUrl=http://localhost:<port>` (routé par prompt : propose → JSON de plan
  de contenu, generate → AsciiDoc enrichi). Zéro réseau, zéro pool dans les tests.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## Réglage JVM

Les tests Cucumber utilisent `maxHeapSize = "1g"` avec `forkEvery = 1` (JVM neuve par scénario)
et `maxParallelForks = 1` (exécution séquentielle pour la stabilité).

```bash
export GRADLE_OPTS="-Xmx2g"   # builds locaux
```

## Commandes de build

```bash
./gradlew build                       # build complet (compile + tests)
./gradlew build -x test               # compilation seule
./gradlew check                       # tous les tests (unitaires + Cucumber + fonctionnels)
./gradlew test                        # unitaires JUnit5 seulement
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (runner agrégé)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # rapports de couverture (XML + HTML)
./gradlew publishToMavenLocal         # publication locale
```

## Pipeline CI

Quatre workflows sous `.github/workflows/` :

| Workflow | Déclencheur | Job |
|----------|-------------|-----|
| `test.yml` — Fast Tests (porte PR) | push / PR | JDK 25 + Gradle caché → `./gradlew test` (unitaires) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **Chromium Playwright caché**, Gradle caché → `./gradlew test functionalTest`, puis `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — diagnostic | manuel (`workflow_dispatch`) | `cucumberTest` complet (non bloquant) |
| `llm-smoke.yml` — Ollama smoke | manuel | image + modèle Ollama cachés, vérifie que le modèle répond |

Cache : `gradle/actions/setup-gradle@v4` (wrapper + dépendances + build cache),
`.github/actions/setup-playwright` (Chromium, clé dérivée de la version `playwright` du
catalogue), `.github/actions/setup-ollama` (image + modèles).

## Publication (NMCP)

**Publié sur Maven Central** — version `0.0.10` (précédentes `0.0.7`, `0.0.6`, `0.0.5`, …).
Le POM est configuré via `maven-publish` et `signing` :

- POM : nom "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Signature : `useGpgCmd()` (exécutée à la publication)
- Agrégation NMCP : `publishAggregationToCentralPortal` (identifiants dans `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Docs d'architecture & de gouvernance

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — architecture du pipeline (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap & tableau de bord de gouvernance
- [AGENT.adoc](../AGENT.adoc) — règles absolues (dont la Loi de l'Économie d'Encre)
- [BACKLOG.adoc](../BACKLOG.adoc) — statut détaillé des EPICs

Tous les EPICs sont clos : `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, et l'ensemble de revue
CR-1→CR-10. Le backlog actionnable est vide (voir `.agents/INDEX.adoc`).

## Contribuer

1. Le build compile : `./gradlew build -x test`
2. Tous les tests verts : `./gradlew check` (ou `-PrunCucumber` pour le BDD)
3. Respecter les règles absolues (voir [AGENT.adoc](../AGENT.adoc)) :
   - Aucun commit/push sans permission
   - DAG N2 — importable par N3, n'importe jamais N3
   - Capsule consomme slider (lecture seule, ne modifie jamais le deck source)
   - Zéro secret/token dans le code
4. Respecter la Loi de l'Économie d'Encre — ne jamais ré-exécuter une opération coûteuse
   (TTS, capture Playwright) quand un artefact valide existe déjà pour la même entrée
5. Baby-step DDD/TDD/BDD — un fichier à la fois ; unitaire → fonctionnel → Cucumber

## Licence

Apache License 2.0 — voir [LICENSE](../../LICENSE).

---

_Fait partie de l'écosystème CCCP Education — `groupId: education.cccp`._
