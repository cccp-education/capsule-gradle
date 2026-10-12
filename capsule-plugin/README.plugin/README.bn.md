<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — প্লাগিনের অভ্যন্তরীণ

> `capsule-plugin` Gradle প্লাগিনের জন্য ডেভেলপার ও অবদানকারী নির্দেশিকা।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![কভারেজ](https://img.shields.io/static/v1?label=%E0%A6%95%E0%A6%AD%E0%A6%BE%E0%A6%B0%E0%A7%87%E0%A6%9C&message=Kover&color=blue)]()
[![লাইসেন্স](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E0%A6%B2%E0%A6%BE%E0%A6%87%E0%A6%B8%E0%A7%87%E0%A6%A8%E0%A7%8D%E0%A6%B8)](../../LICENSE)

- **সংস্করণ**: `0.0.10` (Maven Central) · **গ্রুপ**: `education.cccp` · **প্লাগিন ID**: `education.cccp.capsule`
- **টুলচেইন**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **বিল্ড**: `./gradlew build -x test` · **পরীক্ষা**: `./gradlew check` (ইউনিট + TestKit-এ ফাংশনাল; Cucumber-এর জন্য `-PrunCucumber`) · **কভারেজ**: `./gradlew koverReport`

🌐 ভাষা: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`capsule-gradle` একটি reveal.js ডেক (`slider-gradle` দ্বারা উৎপাদিত) কে সিঙ্ক্রোনাইজড
ভিডিও ক্যাপসুলে রূপান্তর করে — প্রতি স্লাইডে TTS ভয়েস-ওভার, ভিডিও ক্যাপচার, সাবটাইটেল,
ফরম্যাট রূপান্তর, অডিও পোস্ট-প্রোডাকশন, গুণমান নিয়ন্ত্রণ এবং একটি ভার্টিকাল
শর্ট-ফর্ম/ভাইরাল পাইপলাইন — কোনো মানবীয় পোস্ট-প্রোডাকশন ছাড়াই। এটি একটি N2 স্তরের
Gradle প্লাগিন (N3 রানার দ্বারা ভোগ করা হয়, নিজে N3 নয়)।

## মডিউল কাঠামো

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # অভ্যন্তরীণ সংস্করণ ক্যাটালগ
├── doc/CAPSULE_ARCHITECTURE.adoc        # পাইপলাইন আর্কিটেকচার (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # প্রবেশবিন্দু — slider প্রয়োগ, টাস্ক নিবন্ধন, config একত্রীকরণ
    │   ├── CapsuleManager.kt            # টাস্ক নিবন্ধন + ইঞ্জিন ফ্যাক্টরি (resolve*)
    │   ├── CapsuleModels.kt             # CapsuleExtension DSL + CapsuleConventions
    │   ├── CapsuleConfig.kt             # অপরিবর্তনীয় config — 18 বিভাগ
    │   ├── CapsuleConfigLoader.kt       # ${VAR} env রেজল্যুশনসহ YAML লোডার
    │   ├── CapsuleConfigMerger.kt       # 4-উৎস একত্রীকরণ (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # কাঠামোবদ্ধ config লগিং (CR-8)
    │   ├── Capsule{Messages}*.kt        # i18n রিসোর্স রেজল্যুশন (১০ ভাষা)
    │   ├── TtsManager.kt                # Piper + espeak + Fish Audio TTS সংহতকরণ
    │   ├── PlaywrightManager.kt         # হেডলেস Chromium ক্যাপচার
    │   ├── CaptureResolver.kt           # PLAYWRIGHT / SCREENSHOT / REMOTION প্রেরণ
    │   ├── ScreenshotPlanner.kt         # বিশুদ্ধ পরিকল্পনা: প্রতি স্লাইডে PNG → WebM → concat demuxer
    │   ├── Remotion*.kt                 # Remotion ক্যাপচার কৌশল (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # Manim সমৃদ্ধকরণ
    │   ├── Mp4ConversionCommand.kt      # WebM→MP4-এর বিশুদ্ধ FFmpeg argv (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # WebM → MP4/H.264 ট্রান্সকোড (CAP-MP4)
    │   ├── FormatConversion.kt          # WEBM / MP4 / BOTH প্রেরণ
    │   ├── VideoDestinationResolver.kt  # সংস্করণযুক্ত গন্তব্য (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # WAV→MP3 রূপান্তর
    │   ├── MediaProbeUtil.kt            # ffprobe-এ দৈর্ঘ্য নির্ধারণ
    │   ├── RenderSignature.kt           # রেন্ডার স্বাক্ষর + .sig পুনর্ব্যবহার (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # codebase LlmBuildService-এর সাথে LLM সেতু (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # Remotion টাইমিং পোর্ট (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (বিশুদ্ধ argv), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # অধ্যায় মার্কার, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # সমৃদ্ধ প্রসঙ্গ: CapsuleContext(Builder), Doc/Scenario/Glossary লোডার, ProvenanceTracker
    │   ├── feed/          # বক্তা-নোট নিষ্কাশন চুক্তি (SLD-11) + যৌগিক রেজিস্ট্রার
    │   ├── i18n/          # CapsuleMessages (১০ ভাষার বান্ডল)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), AllLanguages রানার
    │   ├── pipeline/      # CAP-ARCH-3: koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — শুধু PNG)
    │   ├── publish/       # VideoDescriptionBuilder (YouTube গার্ড)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/পরিকল্পক + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: নির্ধারক timeline (পূর্ণ ফ্রেম, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, কারাওকে
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (পোর্ট), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # ১০ ভাষা + ভিত্তি
    │   └── remotion/                        # Remotion কম্পোজিশন (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # Cucumber ধাপ সংজ্ঞা + প্রতি-feature রানার
    │   └── **/*Test.kt                      # ইউনিট টেস্ট
    ├── test/features/                       # *.feature — Cucumber দৃশ্যকল্প (৪৪ ফাইল)
    └── functionalTest/kotlin/capsule/       # Gradle TestKit ফাংশনাল টেস্ট
```

## Gradle টাস্ক

| গ্রুপ | টাস্ক |
|-------|-------|
| প্রধান পাইপলাইন | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (অডিও ইনজেকশন + ক্যাপচার), `generateCapsuleVideoAllLanguages`, `deployCapsule` (9:16 রিক্রপ), `distributeCapsuleVideo` (সংস্করণযুক্ত গন্তব্য) |
| LLM কনটেন্ট | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| প্রসঙ্গ | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| আউটপুট | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| গুণমান (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| ভাইরাল (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| যৌগিক feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## নির্ভরতা

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.19`) কে `compileOnly` হিসেবে ভোগ করে —
এটি ফাইল-ভিত্তিক চুক্তি (capsule ডেক HTML ও স্ক্রিপ্ট টেক্সট পড়ে, কখনও পরিবর্তন করে না)।
`CapsulePlugin` রানটাইমে উপলব্ধ থাকলে `education.cccp.slider` স্বয়ংক্রিয়ভাবে প্রয়োগ করে।

CAP-ARCH-1 থেকে, capsule codebase LLM সোকল (`education.cccp:codebase-plugin`) কে
`implementation` হিসেবে ভোগ করে — `LlmBuildService` সেতু `generateCapsuleContent`
ব্যবহৃত `ChatModel` সরবরাহ করে, টেস্টে `-Pollama.baseUrl` দিয়ে mock-LLM ফলব্যাক সহ
(শূন্য নেটওয়ার্ক, শূন্য পুল)।

| লাইব্রেরি | সংস্করণ | ভূমিকা |
|-----------|---------|--------|
| **Playwright** | 1.52.0 | হেডলেস Chromium — reveal.js নিয়ন্ত্রণ + ভিডিও ক্যাপচার (নেটিভ JVM, শূন্য npm) |
| **koog-agents** | 1.3.0 | এজেন্ট অর্কেস্ট্রেশনের Kotlin DSL (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | `ChatModel` অ্যাডাপ্টার (LLM সেতু, ট্রান্সক্রিপ্ট সমৃদ্ধকারী) — সংস্করণ BOM থেকে |
| **Jackson** | 2.21.2 | YAML config + JSON সিরিয়ালাইজেশন (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | কভারেজ রিপোর্ট (XML + HTML, `check`-এ যুক্ত) |
| **Cucumber** | 7.34.3 | BDD টেস্ট (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | প্লাগিন DSL |
| **codebase-plugin** | 0.0.17 | N1 LLM সোকল — `LlmBuildService` সেতু (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | N0 চুক্তি — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | N0 চুক্তি — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — reveal.js ডেক + capsule স্ক্রিপ্ট উৎস |

বাহ্যিক সরঞ্জাম (Maven নির্ভরতা নয়): **Piper** (স্থানীয় অফলাইন TTS, ডিফল্ট),
**espeak-ng** (TTS ফলব্যাক ইঞ্জিন), **FFmpeg** (রিক্রপ, burn-in, MP4, অডিও পোস্ট),
**Manim** (ঐচ্ছিক অ্যানিমেটেড রেখাচিত্র, `[manim:SceneName]`), **Chromium** (প্রথম রানে
Playwright দ্বারা স্বয়ংক্রিয় ডাউনলোড), **Node** (Remotion কৌশল)।

## কনফিগারেশন

`CapsuleConfig`-এ **১৮টি বিভাগ** আছে: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`।

প্রতিটি মান **৪টি উৎস** থেকে নির্ধারিত হয় (অগ্রাধিকার **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

নতুন ক্ষমতাগুলি পশ্চাৎ-সঙ্গতির জন্য **opt-in (ডিফল্ট `false`)**।

## টেস্ট ম্যাট্রিক্স

| টাস্ক | পরিধি | বিবরণ |
|-------|-------|--------|
| `test` | JUnit5 ইউনিট টেস্ট | `capsule.scenarios.**` বাদ দেয়। `@integration` টেস্ট *ফিল্টার হয় না*: বাহ্যিক সরঞ্জাম (Chromium, espeak, FFmpeg, Manim) না থাকলে তারা নিজে স্কিপ হয় |
| `cucumberTest` | Cucumber BDD | `src/test/features`-এর অধীনে feature (৪৪টি `.feature` ফাইল), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g। `-PrunCucumber` বা `CI=true` ছাড়া স্কিপ (CR-10) |
| `functionalTest` | Gradle TestKit | প্লাগিন প্রয়োগের ফাংশনাল টেস্ট |
| `check` | সব | `test` + `cucumberTest` + `functionalTest`-এর উপর নির্ভরশীল |

Cucumber ট্যাগ: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`। **৩৮**টি নিবেদিত `cucumberTest*` টাস্ক প্রতিটি একটি feature আলাদা করে (`build.gradle.kts`-এ `CucumberTaskSpec`)।

টেস্ট বেসলাইন: প্রামাণিক সবুজ হলো `main`-এ CI `Full Tests` ওয়ার্কফ্লো
(ইউনিট + ফাংশনাল TestKit + সমন্বিত/প্রকৃত-রেন্ডার Cucumber; ডায়াগনস্টিক `cucumberTest`
CR-10 থেকে ডিফল্টে স্কিপ)।

## LLM কনটেন্ট পাইপলাইন (CAP-ARCH-3)

`generateCapsuleContent` একটি আহ্বানে koog `CapsulePipelineGraph` পরিচালনা করে:

```
propose-context → validate-context → generate-speaker-notes
```

- **ইনপুট**: উৎস ডেক `.adoc` (`-Pdeck.file=<পথ>` বা `slides/misc`-এ প্রথম `.adoc`),
  `collectCapsuleAugmentedContext` থেকে ঐচ্ছিক সমৃদ্ধ প্রসঙ্গ
  (`build/capsule/augmented-context.txt`), লক্ষ্য ভাষা (`-Pdeck.language`, ডিফল্ট `fr`)।
- **আউটপুট** (ঘোষিত → UP-TO-DATE-এ কালি-সাশ্রয়): সমৃদ্ধ বক্তা-নোট
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS স্ক্রিপ্ট `build/capsule/<deckName>-script.txt`।
- **LLM**: codebase-এর `LlmBuildService` দ্বারা নির্ধারিত; টেস্টে mock Ollama সার্ভার ইনজেক্ট হয়
  `-Pollama.baseUrl=http://localhost:<port>` দিয়ে (প্রম্পট অনুযায়ী রুট: propose → কনটেন্ট প্ল্যান
  JSON, generate → সমৃদ্ধ AsciiDoc)। টেস্টে শূন্য নেটওয়ার্ক, শূন্য পুল।

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## JVM টিউনিং

Cucumber টেস্ট `maxHeapSize = "1g"` ও `forkEvery = 1` (প্রতি দৃশ্যে নতুন JVM)
এবং `maxParallelForks = 1` (স্থিতিশীলতার জন্য ক্রমিক সম্পাদন) ব্যবহার করে।

```bash
export GRADLE_OPTS="-Xmx2g"   # স্থানীয় বিল্ড
```

## বিল্ড কমান্ড

```bash
./gradlew build                       # সম্পূর্ণ বিল্ড (কম্পাইল + টেস্ট)
./gradlew build -x test               # শুধু কম্পাইল
./gradlew check                       # সব টেস্ট (ইউনিট + Cucumber + ফাংশনাল)
./gradlew test                        # শুধু JUnit5 ইউনিট
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (সমন্বিত রানার)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # কভারেজ রিপোর্ট (XML + HTML)
./gradlew publishToMavenLocal         # স্থানীয় প্রকাশ
```

## CI পাইপলাইন

`.github/workflows/`-এ চারটি ওয়ার্কফ্লো:

| ওয়ার্কফ্লো | ট্রিগার | জব |
|-------------|---------|-----|
| `test.yml` — Fast Tests (PR গেট) | push / PR | JDK 25 + ক্যাশড Gradle → `./gradlew test` (ইউনিট) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **ক্যাশড Playwright Chromium**, ক্যাশড Gradle → `./gradlew test functionalTest`, তারপর `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — ডায়াগনস্টিক | ম্যানুয়াল (`workflow_dispatch`) | সম্পূর্ণ `cucumberTest` (অবরোধক নয়) |
| `llm-smoke.yml` — Ollama smoke | ম্যানুয়াল | ক্যাশড Ollama ইমেজ + মডেল, মডেল উত্তর দেয় কি না যাচাই |

ক্যাশ: `gradle/actions/setup-gradle@v4` (wrapper + নির্ভরতা + build cache),
`.github/actions/setup-playwright` (Chromium, কী ক্যাটালগের `playwright` সংস্করণ থেকে),
`.github/actions/setup-ollama` (ইমেজ + মডেল)।

## প্রকাশনা (NMCP)

**Maven Central-এ প্রকাশিত** — সংস্করণ `0.0.10` (পূর্বের `0.0.7`, `0.0.6`, `0.0.5`, …)।
POM `maven-publish` ও `signing` দ্বারা কনফিগার করা:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- স্বাক্ষর: `useGpgCmd()` (প্রকাশনা কালে সম্পাদিত)
- NMCP সমষ্টি: `publishAggregationToCentralPortal` (শংসাপত্র `~/.gradle/gradle.properties`-এ)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## আর্কিটেকচার ও গভর্নেন্স নথি

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — পাইপলাইন আর্কিটেকচার (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC, রোডম্যাপ ও গভর্নেন্স ড্যাশবোর্ড
- [AGENT.adoc](../AGENT.adoc) — পরম নিয়ম (কালি-সাশ্রয় আইন সহ)
- [BACKLOG.adoc](../BACKLOG.adoc) — বিস্তারিত EPIC স্থিতি

সব EPIC বন্ধ: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, এবং CR-1→CR-10 পর্যালোচনা-সেট।
কার্যকর ব্যাকলগ খালি (দেখুন `.agents/INDEX.adoc`)।

## অবদান

1. বিল্ড কম্পাইল হয়: `./gradlew build -x test`
2. সব টেস্ট সবুজ: `./gradlew check` (BDD-এর জন্য `-PrunCucumber`)
3. পরম নিয়মগুলি মেনে চলুন (দেখুন [AGENT.adoc](../AGENT.adoc)):
   - অনুমতি ছাড়া কোনো commit/push নয়
   - DAG N2 — N3 দ্বারা আমদানিযোগ্য, কখনও N3 আমদানি নয়
   - Capsule slider ভোগ করে (কেবল-পাঠ, উৎস ডেক কখনও পরিবর্তন করে না)
   - কোডে শূন্য গোপনীয়তা/টোকেন
4. কালি-সাশ্রয় আইন মেনে চলুন — একই ইনপুটের জন্য বৈধ আর্টিফ্যাক্ট আগে থেকেই থাকলে
   ব্যয়বহুল অপারেশন (TTS, Playwright ক্যাপচার) কখনও পুনরায় চালাবেন না
5. Baby-step DDD/TDD/BDD — একসাথে এক ফাইল; ইউনিট → ফাংশনাল → Cucumber

## লাইসেন্স

Apache License 2.0 — দেখুন [LICENSE](../../LICENSE).

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_
