<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Internes du Plugin

> Guide développeur et contributeur pour le plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Couverture](https://img.shields.io/static/v1?label=couverture&message=Kover&color=blue)]()
[![Licence](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licence)](../LICENSE)

- **Version** : `0.0.1-SNAPSHOT` (non publié) · **Groupe** : `education.cccp` · **ID plugin** : `education.cccp.capsule`
- **Toolchain** : Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **Build** : `./gradlew build -x test` · **Tests** : `./gradlew check` (195 unitaires + 22 Cucumber PASS) · **Couverture** : `./gradlew koverReport`

🌐 Langues : [English](README.md) | **Français**

---

## Organisation des modules

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # catalogue de versions
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # architecture du pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # point d'entrée — applique slider, enregistre les tâches, fusion config
    │   ├── CapsuleManager.kt          # enregistrement tâches + parsing script + méthodes factory
    │   ├── CapsuleModels.kt           # DSL CapsuleExtension + CapsuleConventions + modèles data
    │   ├── CapsuleConfig.kt           # config immutable (5 sections : input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # chargeur YAML avec résolution env ${VAR}
    │   ├── CapsuleConfigMerger.kt     # fusion 4 sources (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (synthèse TTS)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (injection audio + capture Playwright)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (recadrage 9:16 FFmpeg)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (export JSON N3)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # intégration TTS Piper + espeak
    │   ├── PlaywrightManager.kt      # capture Chromium headless
    │   ├── ManimEngine.kt            # rendu Manim (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # remplacement HTML→vidéo embed
    │   ├── ManimVideoMixer.kt        # mux MP4 Manim + audio TTS
    │   ├── ManimParallelRenderer.kt  # rendu Manim parallèle
    │   ├── SubtitleModels.kt         # modèles de données sous-titres
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # génération sous-titres SRT
    │   ├── VttGenerator.kt           # génération sous-titres VTT
    │   ├── SubtitleBurnInService.kt  # burn-in sous-titres FFmpeg
    │   ├── HtmlSectionParser.kt       # parsing HTML deck reveal.js
    │   ├── AudioConversionUtil.kt    # conversion WAV→MP3
    │   ├── MediaProbeUtil.kt         # probing durée ffprobe
    │   └── LanguageVoiceMapping.kt    # mapping voix FR/EN/ES/DE
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # step definitions Cucumber BDD + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 classes de tests unitaires
    ├── test/features/
    │   └── capsule.feature            # 22 scénarios Cucumber
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## Dépendance N2

`capsule-gradle` consomme `slider-gradle` (`education.cccp:slider:0.0.6`) via `compileOnly`.
Le contrat est basé sur des fichiers — capsule lit le HTML du deck et les fichiers texte de script
produits par slider, ne les modifie jamais. CapsulePlugin applique automatiquement
`education.cccp.slider` au runtime si disponible.

## Bibliothèques clés

| Bibliothèque | Version | Rôle |
|--------------|---------|------|
| **Playwright** | 1.52.0 | Chromium headless — contrôle reveal.js + capture vidéo (JVM natif, zéro npm) |
| **koog-agents** | 0.8.0 | DSL Kotlin pour orchestration d'agents (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | Config YAML + sérialisation JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Rapports de couverture (XML + HTML, intégrés à `check`) |
| **Cucumber** | 7.34.3 | Tests BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | DSL du plugin |
| **slider** | 0.0.6 | compileOnly — deck reveal.js + script capsule source |

Outils externes (pas des dépendances Maven) :
- **Piper** — moteur TTS local offline (par défaut)
- **espeak** — moteur TTS de secours
- **FFmpeg** — recadrage 9:16 (`deployCapsule`) et burn-in des sous-titres
- **Manim** — rendu d'animation mathématique (optionnel, `[manim:SceneName]` dans le script)
- **Chromium** — téléchargé automatiquement par Playwright au premier lancement

## Matrice de tests

| Tâche | Portée | Détails |
|------|-------|---------|
| `test` | Tests unitaires JUnit5 | Exclut le tag `@integration` et `capsule.scenarios.**` |
| `cucumberTest` | BDD Cucumber | 22 scénarios, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | Tests fonctionnels d'application du plugin |
| `check` | Tous | Dépend de `test` + `cucumberTest` + `functionalTest` |

Tags Cucumber : `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`.

Total tests : **195 unitaires + 22 Cucumber = 217 PASS** (baseline session 029).

## Réglage JVM

Les tests Cucumber utilisent `maxHeapSize = "1g"` avec `forkEvery = 1` (JVM fraîche par scénario)
et `maxParallelForks = 1` (exécution séquentielle pour la stabilité).

Pour les builds locaux :
```bash
export GRADLE_OPTS="-Xmx2g"
```

## Commandes de build

```bash
./gradlew build                       # build complet (compile + teste)
./gradlew build -x test               # compile seulement
./gradlew check                       # tous les tests (unitaires + Cucumber + fonctionnels)
./gradlew test                        # JUnit5 unitaires seulement
./gradlew cucumberTest                # BDD Cucumber
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # rapports de couverture (XML + HTML)
./gradlew publishToMavenLocal         # publication locale
```

## Pipeline CI

`.github/workflows/test.yml` définit un job :
1. **Build & Test** — `./gradlew check` sur chaque push/PR vers main/master (≤ 15 min, JDK 24 Temurin, action setup Gradle)

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '24', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew check
        working-directory: capsule-plugin
```

## Publication (NMCP)

**Non publié** — version `0.0.1-SNAPSHOT`. Le POM est configuré pour Maven Central via
les plugins `maven-publish` et `signing` :

- POM : nom "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Signature : `useGpgCmd()` (configuré mais pas encore exécuté)
- Cible de publication : `publishToMavenLocal` disponible ; NMCP `publishAggregationToCentralPortal`
  nécessite des identifiants dans `~/.gradle/gradle.properties`

Pour publier quand prêt :
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Docs d'architecture

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — Architecture
  du pipeline avec diagrammes PlantUML (pipeline global, composants, séquence, déploiement)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, roadmap & gouvernance
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — Règles absolues (7 règles, Loi de l'Économie d'Encre)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — Code review (10 EPICs actionnables CR-1→CR-10)

## Statut des EPICs

CAP-0 à CAP-25 terminés. EPICs actifs (voir `.agents/INDEX.adoc`) :

| EPIC | Description | Statut |
|------|-------------|--------|
| CR-1 | Refactor `CapsuleVideoTask.execute()` (218→76 lignes) | ✅ Terminé (session 040) |
| CR-2 | Thread safety + timeout `captureSlideParallel` | ⬜ TODO |
| CR-3 | Gestion d'erreur TTS — dégradation signalée | ⬜ TODO |
| CR-4 | Sécurité HTML escape — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 lignes dupliquées) | ⬜ TODO |
| CAP-27 | VTT burn-in (actuellement SRT seulement) | ⬜ TODO |
| CAP-28 | Gaps de couverture — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## Contribuer

1. Le build compile : `./gradlew build -x test`
2. Tous les tests verts : `./gradlew check`
3. Suivre les 7 règles absolues (voir [AGENT.adoc](../capsule-plugin/AGENT.adoc)) :
   - Pas de commit/push sans permission
   - DAG N2 — importable par N3, n'importe jamais N3
   - Capsule consomme slider (lecture seule, ne modifie jamais le deck source)
   - Zéro secret/token dans le code
4. Respecter la Loi de l'Économie d'Encre — ne jamais ré-exécuter d'opérations coûteuses (TTS, capture Playwright)
   quand un artefact valide existe déjà pour la même entrée

## Licence

Apache License 2.0 — voir [LICENSE](../LICENSE).

---

_Partie de l'écosystème CCCP Education — `groupId: education.cccp`._