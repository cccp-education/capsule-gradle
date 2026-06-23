<!-- master source — other languages are translations of this file -->
# capsule-gradle — Plugin Internals

> Developer & contributor guide for the `capsule-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Version**: `0.0.1-SNAPSHOT` (non publié) · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.capsule`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew check` (195 unit + 22 Cucumber PASS) · **Coverage**: `./gradlew koverReport`

🌐 Languages: **EN** | [Français](README.fr.md)

---

## Module layout

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # version catalog
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # pipeline architecture (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # entry point — applies slider, registers tasks, config merge
    │   ├── CapsuleManager.kt          # task registration + script parsing + factory methods
    │   ├── CapsuleModels.kt           # CapsuleExtension DSL + CapsuleConventions + data models
    │   ├── CapsuleConfig.kt           # immutable config (5 sections: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # YAML loader with ${VAR} env resolution
    │   ├── CapsuleConfigMerger.kt     # 4-source merge (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (TTS synthesis)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (audio injection + Playwright capture)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (9:16 FFmpeg recrop)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (N3 JSON export)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # Piper + espeak TTS integration
    │   ├── PlaywrightManager.kt      # headless Chromium capture
    │   ├── ManimEngine.kt            # Manim rendering (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # HTML→video embed replacement
    │   ├── ManimVideoMixer.kt        # mux Manim MP4 + TTS audio
    │   ├── ManimParallelRenderer.kt  # parallel Manim rendering
    │   ├── SubtitleModels.kt         # subtitle data models
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # SRT subtitle generation
    │   ├── VttGenerator.kt           # VTT subtitle generation
    │   ├── SubtitleBurnInService.kt  # FFmpeg subtitle burn-in
    │   ├── HtmlSectionParser.kt       # reveal.js deck HTML parsing
    │   ├── AudioConversionUtil.kt    # WAV→MP3 conversion
    │   ├── MediaProbeUtil.kt         # ffprobe duration probing
    │   └── LanguageVoiceMapping.kt    # FR/EN/ES/DE voice mapping
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD step definitions + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 unit test classes
    ├── test/features/
    │   └── capsule.feature            # 22 Cucumber scenarios
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## N2 dependency

`capsule-gradle` consumes `slider-gradle` (`education.cccp:slider:0.0.6`) via `compileOnly`.
The contract is file-based — capsule reads the deck HTML and script text files produced by slider,
never modifies them. CapsulePlugin auto-applies `education.cccp.slider` at runtime if available.

## Key libraries

| Library | Version | Role |
|---------|---------|------|
| **Playwright** | 1.52.0 | Headless Chromium — reveal.js control + video capture (JVM native, zero npm) |
| **koog-agents** | 0.8.0 | Kotlin DSL for agent orchestration (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | YAML config + JSON serialization (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Coverage reports (XML + HTML, wired into `check`) |
| **Cucumber** | 7.34.3 | BDD tests (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | Plugin DSL |
| **slider** | 0.0.6 | compileOnly — reveal.js deck + capsule script source |

External tools (not Maven dependencies):
- **Piper** — local offline TTS engine (default)
- **espeak** — TTS fallback engine
- **FFmpeg** — 9:16 recrop (`deployCapsule`) and subtitle burn-in
- **Manim** — mathematical animation rendering (optional, `[manim:SceneName]` in script)
- **Chromium** — auto-downloaded by Playwright on first run

## Test matrix

| Task | Scope | Details |
|------|-------|---------|
| `test` | JUnit5 unit tests | Excludes `@integration` tag and `capsule.scenarios.**` |
| `cucumberTest` | Cucumber BDD | 22 scenarios, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | Plugin application functional tests |
| `check` | All | Depends on `test` + `cucumberTest` + `functionalTest` |

Cucumber tags: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`.

Test totals: **195 unit + 22 Cucumber = 217 PASS** (session 029 baseline).

## JVM tuning

Cucumber tests use `maxHeapSize = "1g"` with `forkEvery = 1` (fresh JVM per scenario)
and `maxParallelForks = 1` (sequential execution for stability).

For local builds:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## Build commands

```bash
./gradlew build                       # full build (compiles + tests)
./gradlew build -x test               # compile only
./gradlew check                       # all tests (unit + Cucumber + functional)
./gradlew test                        # JUnit5 unit only
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # coverage reports (XML + HTML)
./gradlew publishToMavenLocal         # local publish
```

## CI pipeline

`.github/workflows/test.yml` defines one job:
1. **Build & Test** — `./gradlew check` on every push/PR to main/master (≤ 15 min, JDK 24 Temurin, Gradle setup action)

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

**Non publié** — version `0.0.1-SNAPSHOT`. The POM is configured for Maven Central via
`maven-publish` and `signing` plugins:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Signing: `useGpgCmd()` (configured but not yet executed)
- Publication target: `publishToMavenLocal` available; NMCP `publishAggregationToCentralPortal`
  requires credentials in `~/.gradle/gradle.properties`

To publish when ready:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Architecture docs

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — Pipeline
  architecture with PlantUML diagrams (global pipeline, components, sequence, deployment)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, roadmap & governance
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — Absolute rules (7 rules, Ink Economy Law)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — Code review (10 actionable EPICs CR-1→CR-10)

## EPIC status

CAP-0 through CAP-25 terminated. Active EPICs (see `.agents/INDEX.adoc`):

| EPIC | Description | Status |
|------|-------------|--------|
| CR-1 | Refactor `CapsuleVideoTask.execute()` (218→76 lines) | ✅ Terminated (session 040) |
| CR-2 | Thread safety + timeout `captureSlideParallel` | ⬜ TODO |
| CR-3 | Error handling TTS — degradation signaled | ⬜ TODO |
| CR-4 | HTML escape security — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 lines duplicated) | ⬜ TODO |
| CAP-27 | VTT burn-in (currently SRT only) | ⬜ TODO |
| CAP-28 | Coverage gaps — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## Contributing

1. Build compiles: `./gradlew build -x test`
2. All tests green: `./gradlew check`
3. Follow the 7 absolute rules (see [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - No commits/push without permission
   - DAG N2 — importable by N3, never imports N3
   - Capsule consumes slider (read-only, never modifies source deck)
   - Zero secrets/tokens in code
4. Respect the Ink Economy Law — never re-execute costly operations (TTS, Playwright capture)
   when a valid artifact already exists for the same input

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._