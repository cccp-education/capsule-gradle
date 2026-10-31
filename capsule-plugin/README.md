<!-- master source — other languages are translations of this file -->
# capsule-gradle — Plugin Internals

> Developer & contributor guide for the `capsule-plugin` Gradle plugin.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Version**: `0.0.10` (Maven Central) · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.capsule`
- **Toolchain**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew check` (unit + functional via TestKit; Cucumber via `-PrunCucumber`) · **Coverage**: `./gradlew koverReport`

🌐 Languages: **EN** | [中文](README.plugin/README.zh.md) | [हिन्दी](README.plugin/README.hi.md) | [Español](README.plugin/README.es.md) | [Français](README.plugin/README.fr.md) | [العربية](README.plugin/README.ar.md) | [বাংলা](README.plugin/README.bn.md) | [Português](README.plugin/README.pt.md) | [Русский](README.plugin/README.ru.md) | [اردو](README.plugin/README.ur.md)

---

## What it does

`capsule-gradle` turns a reveal.js deck (produced by `slider-gradle`) into a
synchronised video capsule — TTS voice-over per slide, video capture, subtitles,
format conversion, post-production audio, quality control and a vertical
short-form/viral pipeline — without human post-production. It is an N2 Gradle
plugin (consumed by the N3 runner, never N3 itself).

## Module layout

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # internal version catalog
├── doc/CAPSULE_ARCHITECTURE.adoc        # pipeline architecture (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # entry point — applies slider, registers tasks, merges config
    │   ├── CapsuleManager.kt            # task registration + engine factories (resolve*)
    │   ├── CapsuleModels.kt             # CapsuleExtension DSL + CapsuleConventions
    │   ├── CapsuleConfig.kt             # immutable config — 18 sections
    │   ├── CapsuleConfigLoader.kt       # YAML loader with ${VAR} env resolution
    │   ├── CapsuleConfigMerger.kt       # 4-source merge (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # structured config logging (CR-8)
    │   ├── Capsule{Messages}*.kt        # i18n resource resolution (10 languages)
    │   ├── TtsManager.kt                # Piper + espeak + Fish Audio TTS integration
    │   ├── PlaywrightManager.kt         # headless Chromium capture
    │   ├── CaptureResolver.kt           # PLAYWRIGHT / SCREENSHOT / REMOTION dispatch
    │   ├── ScreenshotPlanner.kt         # pure plan: PNG per slide → WebM → concat demuxer
    │   ├── Remotion*.kt                 # Remotion capture strategy (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # Manim enrichment
    │   ├── Mp4ConversionCommand.kt      # pure FFmpeg argv for WebM→MP4 (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # WebM → MP4/H.264 transcode (CAP-MP4)
    │   ├── FormatConversion.kt          # WEBM / MP4 / BOTH dispatch
    │   ├── VideoDestinationResolver.kt  # versioned destination (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # WAV→MP3
    │   ├── MediaProbeUtil.kt            # ffprobe duration probing
    │   ├── RenderSignature.kt           # render signature + .sig reuse (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # LLM bridge to codebase LlmBuildService (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # Remotion timing port (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (pure argv), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # chapter markers, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # augmented context: CapsuleContext(Builder), Doc/Scenario/Glossary loaders, ProvenanceTracker
    │   ├── feed/          # speaker-notes extraction contract (SLD-11) + composite registrars
    │   ├── i18n/          # CapsuleMessages (10-language bundles)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), AllLanguages runner
    │   ├── pipeline/      # CAP-ARCH-3: koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — PNG only)
    │   ├── publish/       # VideoDescriptionBuilder (YouTube guard)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/planners + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: deterministic timeline (whole frames, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, karaoke
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   AudioSilenceDetector (silent take), PronunciationLexicon, NumberSpeller,
    │                      #   SpeechRecognizer (port), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 languages + base
    │   └── remotion/                        # Remotion composition (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # Cucumber step definitions + per-feature runners
    │   └── **/*Test.kt                      # unit tests
    ├── test/features/                       # *.feature — Cucumber scenarios (44 files)
    └── functionalTest/kotlin/capsule/       # Gradle TestKit functional tests
```

## Gradle tasks

| Group | Tasks |
|-------|-------|
| Main pipeline | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (audio injection + capture), `generateCapsuleVideoAllLanguages`, `deployCapsule` (9:16 recrop), `distributeCapsuleVideo` (versioned destination) |
| LLM content | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| Context | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| Output | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| Quality (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| Viral (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| Feed composite | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## Dependencies

`capsule-gradle` consumes `slider-gradle` (`education.cccp:slider:0.0.19`) via `compileOnly` —
a file-based contract (capsule reads the deck HTML + script text, never modifies them).
`CapsulePlugin` auto-applies `education.cccp.slider` at runtime if available.

Since CAP-ARCH-1, capsule also consumes the codebase LLM socle
(`education.cccp:codebase-plugin`) as `implementation` — the `LlmBuildService` bridge
provides the `ChatModel` used by `generateCapsuleContent`, with a mock-LLM fallback via
`-Pollama.baseUrl` for tests (zero network, zero pool).

| Library | Version | Role |
|---------|---------|------|
| **Playwright** | 1.52.0 | Headless Chromium — reveal.js control + video capture (JVM native, zero npm) |
| **koog-agents** | 1.3.0 | Kotlin DSL for agent orchestration (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | `ChatModel` adapters (LLM bridge, transcript enhancer) — version via BOM |
| **Jackson** | 2.21.2 | YAML config + JSON serialization (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Coverage reports (XML + HTML, wired into `check`) |
| **Cucumber** | 7.34.3 | BDD tests (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | Plugin DSL |
| **codebase-plugin** | 0.0.17 | N1 LLM socle — `LlmBuildService` bridge (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | N0 contracts — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | N0 contracts — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — reveal.js deck + capsule script source |

External tools (not Maven dependencies): **Piper** (local offline TTS, default),
**espeak-ng** (TTS fallback), **FFmpeg** (recrop, burn-in, MP4, audio post),
**Manim** (optional animated schematics, `[manim:SceneName]`), **Chromium**
(auto-downloaded by Playwright on first run), **Node** (Remotion strategy).

## Configuration

`CapsuleConfig` has **18 sections**: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`.

Every value is resolved from **4 sources** (precedence **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

New capabilities are **opt-in (default `false`)** for backward compatibility.

## Test matrix

| Task | Scope | Details |
|------|-------|---------|
| `test` | JUnit5 unit tests | Excludes `capsule.scenarios.**`. `@integration` tests are *not* filtered out: they self-skip when the external tool (Chromium, espeak, FFmpeg, Manim) is missing |
| `cucumberTest` | Cucumber BDD | features under `src/test/features` (44 `.feature` files), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g. Skipped unless `-PrunCucumber` or `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | Plugin application functional tests |
| `check` | All | Depends on `test` + `cucumberTest` + `functionalTest` |

Cucumber tags: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`. **38** dedicated `cucumberTest*` tasks isolate one feature each (`CucumberTaskSpec` in `build.gradle.kts`).

Test baseline: the authoritative green is the CI `Full Tests` workflow on `main`
(unit + functional TestKit + aggregate/real-render Cucumber; the diagnostic
`cucumberTest` is skipped by default since CR-10).

## LLM content pipeline (CAP-ARCH-3)

`generateCapsuleContent` orchestrates the koog `CapsulePipelineGraph` in one invocation:

```
propose-context → validate-context → generate-speaker-notes
```

- **Inputs**: source deck `.adoc` (`-Pdeck.file=<path>` or first `.adoc` in `slides/misc`),
  optional augmented context from `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`), target language (`-Pdeck.language`, default `fr`).
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

```bash
export GRADLE_OPTS="-Xmx2g"   # local builds
```

## Build commands

```bash
./gradlew build                       # full build (compiles + tests)
./gradlew build -x test               # compile only
./gradlew check                       # all tests (unit + Cucumber + functional)
./gradlew test                        # JUnit5 unit only
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (aggregate runner)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # coverage reports (XML + HTML)
./gradlew publishToMavenLocal         # local publish
```

## CI pipeline

Four workflows under `.github/workflows/`:

| Workflow | Trigger | Job |
|----------|---------|-----|
| `test.yml` — Fast Tests (PR gate) | push / PR | JDK 25 + cached Gradle → `./gradlew test` (unit) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **cached Playwright Chromium**, cached Gradle → `./gradlew test functionalTest`, then `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — diagnostic | manual (`workflow_dispatch`) | full `cucumberTest` (non-gating) |
| `llm-smoke.yml` — Ollama smoke | manual | cached Ollama image + model, checks the model answers |

Caching: `gradle/actions/setup-gradle@v4` (wrapper + dependencies + build cache),
`.github/actions/setup-playwright` (Chromium, key derived from the catalog `playwright`
version), `.github/actions/setup-ollama` (image + models).

## Publication (NMCP)

**Published to Maven Central** — version `0.0.10` (previous `0.0.9`, `0.0.8`, `0.0.7`, `0.0.6`, `0.0.5`, …).
The POM is configured via `maven-publish` and `signing`:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Signing: `useGpgCmd()` (executed at publication)
- NMCP aggregation: `publishAggregationToCentralPortal` (credentials in `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Architecture & governance docs

- [doc/CAPSULE_ARCHITECTURE.adoc](doc/CAPSULE_ARCHITECTURE.adoc) — pipeline architecture (PlantUML)
- [.agents/INDEX.adoc](.agents/INDEX.adoc) — EPICs, roadmap & governance dashboard
- [AGENT.adoc](AGENT.adoc) — absolute rules (incl. the Ink Economy Law)
- [BACKLOG.adoc](BACKLOG.adoc) — detailed EPIC status

All EPICs are closed: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, and the CR-1→CR-10
review set. The actionable backlog is empty (see `.agents/INDEX.adoc`).

## Contributing

1. Build compiles: `./gradlew build -x test`
2. All tests green: `./gradlew check` (or `-PrunCucumber` for BDD)
3. Follow the absolute rules (see [AGENT.adoc](AGENT.adoc)):
   - No commits/push without permission
   - DAG N2 — importable by N3, never imports N3
   - Capsule consumes slider (read-only, never modifies the source deck)
   - Zero secrets/tokens in code
4. Respect the Ink Economy Law — never re-execute a costly operation (TTS, Playwright
   capture) when a valid artifact already exists for the same input
5. Baby-step DDD/TDD/BDD — one file at a time; unit → functional → Cucumber

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._
