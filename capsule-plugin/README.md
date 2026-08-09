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

🌐 Languages: **EN** | [中文](README.plugin/README.zh.md) | [हिन्दी](README.plugin/README.hi.md) | [Español](README.plugin/README.es.md) | [Français](README.plugin/README.fr.md) | [العربية](README.plugin/README.ar.md) | [বাংলা](README.plugin/README.bn.md) | [Português](README.plugin/README.pt.md) | [Русский](README.plugin/README.ru.md) | [اردو](README.plugin/README.ur.md)

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
│   ├── feed/                      # domain: speaker-notes extraction contract (SLD-11)
│   ├── multilang/                 # domain: multi-language video pipeline (CAP-29)
│   │   ├── MultiLanguageResolver.kt  # LanguageCatalog → Piper/espeak voice resolution
│   │   ├── VoiceMapping.kt           # 10-language voice mapping
│   │   ├── CapsuleVideoPlan.kt       # immutable plan + entry (deck/script/language/output)
│   │   ├── CapsuleVideoPlanner.kt    # builds the plan from translated deck+script pairs
│   │   ├── CapsuleVideoAllLanguagesRunner.kt  # pure iteration + Ink Economy skip
│   │   └── GenerateCapsuleVideoAllLanguagesTask.kt  # generateCapsuleVideoAllLanguages
│   ├── ai/                        # domain: LLM bridge to the codebase LlmBuildService (CAP-ARCH-1)
│   │   ├── CapsuleLlmService.kt       # provider selection + mock-LLM fallback (-Pollama.baseUrl)
│   │   └── CapsuleAiSmokeTestTask.kt  # capsuleAiSmokeTest
│   ├── context/                   # domain: augmented context via CompositeContext (CAP-ARCH-2)
│   │   ├── CapsuleContext.kt          # rendered context + channel list (invariant fail-fast)
│   │   ├── CapsuleContextBuilder.kt   # pure builder: channels → budget → merged sections
│   │   └── CollectCapsuleAugmentedContextTask.kt  # collectCapsuleAugmentedContext
│   ├── pipeline/                  # domain: koog content-generation pipeline (CAP-ARCH-3)
│   │   ├── CapsulePipelineGraph.kt    # koog StateGraph: propose-context → validate → generate
│   │   ├── CapsuleState.kt            # stage machine + invariants
│   │   ├── CapsulePromptBuilder.kt    # port + DefaultCapsulePromptBuilder (pedagogical prompts)
│   │   ├── CapsuleLlm.kt              # port + ChatModelCapsuleLlm (langchain4j adapter)
│   │   ├── ContentPlanValidator.kt    # pure validation of the propose-context JSON plan
│   │   ├── TtsScriptDeriver.kt        # derives the *-script.txt contract from speaker notes
│   │   └── GenerateCapsuleContentTask.kt  # generateCapsuleContent
    │   ├── CapsuleConfig.kt           # immutable config (5 sections: input, tts, capture, distrib, manim)
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

`capsule-gradle` consumes `slider-gradle` (`education.cccp:slider:0.0.16`) via `compileOnly`.
The contract is file-based — capsule reads the deck HTML and script text files produced by slider,
never modifies them. CapsulePlugin auto-applies `education.cccp.slider` at runtime if available.

Since CAP-ARCH-1, capsule also consumes the codebase LLM socle (`education.cccp:codebase-plugin`)
as `implementation` — the `LlmBuildService` bridge (SLD-8 pattern) provides the `ChatModel`
used by the `generateCapsuleContent` pipeline, with a mock-LLM fallback via `-Pollama.baseUrl`
for tests (zero network, zero pool).

## Key libraries

| Library | Version | Role |
|---------|---------|------|
| **Playwright** | 1.52.0 | Headless Chromium — reveal.js control + video capture (JVM native, zero npm) |
| **koog-agents** | 1.0.0 | Kotlin DSL for agent orchestration (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **Jackson** | 2.18.3 | YAML config + JSON serialization (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Coverage reports (XML + HTML, wired into `check`) |
| **Cucumber** | 7.34.3 | BDD tests (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | Plugin DSL |
| **codebase-plugin** | 0.0.5 | N1 LLM socle — LlmBuildService bridge (`capsule.ai`) |
| **codebase-contracts** | 0.1.0 | N0 contracts — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **slider** | 0.0.16 | compileOnly — reveal.js deck + capsule script source |

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

Test totals: **540 unit + 34 functional = 574 PASS** (session 086 baseline; cucumber skipped by default since CR-10).

## LLM content pipeline (CAP-ARCH-3)

`generateCapsuleContent` orchestrates the koog `CapsulePipelineGraph` in one invocation:

```
propose-context → validate-context → generate-speaker-notes
```

- **Inputs**: source deck `.adoc` (`-Pdeck.file=<path>` or first `.adoc` in `slides/misc`),
  optional augmented context from `collectCapsuleAugmentedContext` (`build/capsule/augmented-context.txt`),
  target language (`-Pdeck.language`, default `fr`).
- **Outputs** (declared → Ink Economy via UP-TO-DATE): enriched speaker notes
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS script `build/capsule/<deckName>-script.txt`.
- **LLM**: resolved via the codebase `LlmBuildService`; tests inject a mock Ollama server with
  `-Pollama.baseUrl=http://localhost:<port>` (routed by prompt: propose → content plan JSON,
  generate → enriched AsciiDoc). Zero network, zero pool in tests.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

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
| CR-2 | Thread safety + timeout `captureSlideParallel` | ✅ Terminated (session 071) |
| CR-3 | Error handling TTS — degradation signaled | ✅ Terminated (session 071) |
| CR-4 | HTML escape security — `injectSubtitleTrack` | ✅ Terminated (session 071) |
| CR-5 | DRY `CapsuleConfigMerger` (~76 lines duplicated) | ✅ Terminated (session 072) |
| CR-6 | Robust CLI parsing | ✅ Terminated (session 071) |
| CR-7 | Validation YAML | ✅ Terminated (session 071) |
| CR-8 | Structured logging | ✅ Terminated (session 073) |
| CR-9 | Robust HTML parsing | ✅ Terminated (session 074) |
| CR-10 | CI optimization — cucumberTest onlyIf | ✅ Terminated (session 075) |
| CAP-29 | Multi-language video pipeline (`capsule.multilang`) | ✅ Terminated (session 082) |
| CAP-ARCH | LLM-driven capsule (koog pipeline, augmented context) — CAP-ARCH-0→3 done (`capsule.ai`, `capsule.context`, `capsule.pipeline`) | 🟡 In progress (CAP-ARCH-4→6 TODO) |
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