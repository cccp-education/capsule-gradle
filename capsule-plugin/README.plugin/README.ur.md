<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — پلگ ان کے اندرونی معاملات

> `capsule-plugin` Gradle پلگ ان کے لیے ڈیولپر اور معاون کی رہنمائی۔

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![کوریج](https://img.shields.io/static/v1?label=%DA%A9%D9%88%D8%B1%DB%8C%D8%AC&message=Kover&color=blue)]()
[![لائسنس](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D9%84%D8%A7%D8%A6%D8%B3%D9%86%D8%B3)](../../LICENSE)

- **ورژن**: `0.0.8` (Maven Central) · **گروپ**: `education.cccp` · **پلگ ان ID**: `education.cccp.capsule`
- **ٹول چین**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **بلڈ**: `./gradlew build -x test` · **ٹیسٹ**: `./gradlew check` (یونٹ + TestKit کے ذریعے فنکشنل؛ Cucumber کے لیے `-PrunCucumber`) · **کوریج**: `./gradlew koverReport`

🌐 زبانیں: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`capsule-gradle` ایک reveal.js ڈیک (جو `slider-gradle` بناتا ہے) کو ہم آہنگ ویڈیو
کیپسول میں بدلتا ہے — ہر سلائیڈ کے لیے TTS وائس اوور، ویڈیو کیپچر، سب ٹائٹلز، فارمیٹ
تبدیلی، آڈیو پوسٹ پروڈکشن، معیار کی جانچ اور ایک عمودی مختصر/وائرل پائپ لائن — بغیر
کسی انسانی پوسٹ پروڈکشن کے۔ یہ N2 درجے کا Gradle پلگ ان ہے (N3 رنر اسے استعمال کرتا ہے،
خود N3 نہیں)۔

## ماڈیول کی ساخت

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # داخلی ورژن کیٹلاگ
├── doc/CAPSULE_ARCHITECTURE.adoc        # پائپ لائن آرکیٹیکچر (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # اندراجی نقطہ — slider لگاتا ہے، ٹاسک رجسٹر کرتا ہے، config ضم کرتا ہے
    │   ├── CapsuleManager.kt            # ٹاسک رجسٹریشن + انجن فیکٹریاں (resolve*)
    │   ├── CapsuleModels.kt             # CapsuleExtension DSL + CapsuleConventions
    │   ├── CapsuleConfig.kt             # ناقابلِ تبدیلی config — 18 حصے
    │   ├── CapsuleConfigLoader.kt       # ${VAR} env ریزولوشن کے ساتھ YAML لوڈر
    │   ├── CapsuleConfigMerger.kt       # 4 ذرائع کا انضمام (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # منظم config لاگنگ (CR-8)
    │   ├── Capsule{Messages}*.kt        # i18n وسائل کی ریزولوشن (10 زبانیں)
    │   ├── TtsManager.kt                # Piper + espeak TTS انضمام
    │   ├── PlaywrightManager.kt         # ہیڈ لیس Chromium کیپچر
    │   ├── CaptureResolver.kt           # PLAYWRIGHT / SCREENSHOT / REMOTION کی تقسیم
    │   ├── ScreenshotPlanner.kt         # خالص منصوبہ: ہر سلائیڈ PNG → WebM → concat demuxer
    │   ├── Remotion*.kt                 # Remotion کیپچر حکمتِ عملی (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # Manim افزودگی
    │   ├── Mp4ConversionCommand.kt      # WebM→MP4 کے لیے خالص FFmpeg argv (+ BT.709، opt-in)
    │   ├── VideoFormatConverter.kt      # WebM → MP4/H.264 ٹرانس کوڈ (CAP-MP4)
    │   ├── FormatConversion.kt          # WEBM / MP4 / BOTH کی تقسیم
    │   ├── VideoDestinationResolver.kt  # ورژن شدہ منزل (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # WAV→MP3 تبدیلی
    │   ├── MediaProbeUtil.kt            # ffprobe کے ذریعے دورانیہ جانچ
    │   ├── RenderSignature.kt           # رینڈر دستخط + .sig دوبارہ استعمال (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # codebase LlmBuildService تک LLM پُل (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # Remotion ٹائمنگ پورٹ (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (خالص argv)، AudioPostProcessor (+NoOp)، BGM/loudnorm/ducking
    │   ├── chapters/      # ابواب کے نشانات، CardRenderer، YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10)، ProcessMigrationGuard، OssVocabularyGuard
    │   ├── context/       # افزودہ سیاق: CapsuleContext(Builder)، Doc/Scenario/Glossary لوڈرز، ProvenanceTracker
    │   ├── feed/          # اسپیکر نوٹس نکالنے کا معاہدہ (SLD-11) + مرکب رجسٹرار
    │   ├── i18n/          # CapsuleMessages (10 زبانوں کے بنڈل)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver، VoiceMapping، CapsuleVideoPlan(ner)، AllLanguages رنر
    │   ├── pipeline/      # CAP-ARCH-3: koog CapsulePipelineGraph، CapsuleState، CapsuleLlm (+ChatModel)، ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp)، PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — صرف PNG)
    │   ├── publish/       # VideoDescriptionBuilder (YouTube گارڈ)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05)، QcBoard/منصوبہ ساز + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: قطعی timeline (مکمل فریمز، lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder، ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard، deck builder، campaign bundle/gate، thumbnail، کیراوکے
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer، WordAligner، FidelityScorer، SnrEstimator،
    │                      #   PronunciationLexicon، NumberSpeller، SpeechRecognizer (پورٹ)، TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 زبانیں + بنیاد
    │   └── remotion/                        # Remotion ترکیب (Capsule.jsx، Root.jsx، render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # Cucumber اسٹیپ تعریفات + فی-feature رنر
    │   └── **/*Test.kt                      # یونٹ ٹیسٹ
    ├── test/features/                       # *.feature — Cucumber منظرنامے (44 فائلیں)
    └── functionalTest/kotlin/capsule/       # Gradle TestKit فنکشنل ٹیسٹ
```

## Gradle ٹاسک

| گروپ | ٹاسک |
|------|------|
| بنیادی پائپ لائن | `generateCapsuleScript`، `generateCapsule` (TTS)، `generateCapsuleVideo` (آڈیو انجیکشن + کیپچر)، `generateCapsuleVideoAllLanguages`، `deployCapsule` (9:16 دوبارہ کراپ)، `distributeCapsuleVideo` (ورژن شدہ منزل) |
| LLM مواد | `generateCapsuleContent`، `capsuleAiSmokeTest`، `collectCapsuleAugmentedContext`، `generateCapsuleContentAndVideos` |
| سیاق | `collectCapsuleContext`، `transformCapsuleContext`، `collectCapsuleRetrieve`، `scaffoldCapsuleContext` |
| اخراج | `generateCapsuleTranscript`، `generateCapsuleChapters`، `generateCapsulePodcast`، `validateCapsuleVideoDuration` |
| معیار (opt-in) | `preflightCapsuleContent`، `renderQcBoards`، `verifyCapsuleVoice`، `selectBestVoiceTake` |
| وائرل (opt-in) | `generateViralCampaign`، `normalizeViralStoryboard`، `renderViralVariants`، `renderViralThumbnails`، `renderViralSubtitles`، `validateViralCampaign` |
| مرکب feed | `translateAndExtractSpeakerNotes`، `translateAndGenerateCapsuleVideos` |

## انحصار

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.19`) کو `compileOnly` کے طور پر استعمال کرتا ہے —
یہ فائل پر مبنی معاہدہ ہے (capsule ڈیک HTML اور اسکرپٹ متن پڑھتا ہے، انہیں کبھی تبدیل نہیں کرتا)۔
`CapsulePlugin` رن ٹائم پر دستیاب ہونے پر `education.cccp.slider` خودکار طور پر لگاتا ہے۔

CAP-ARCH-1 سے، capsule codebase LLM بنیاد (`education.cccp:codebase-plugin`) کو
`implementation` کے طور پر بھی استعمال کرتا ہے — `LlmBuildService` پُل `generateCapsuleContent`
میں استعمال ہونے والا `ChatModel` فراہم کرتا ہے، ٹیسٹوں کے لیے `-Pollama.baseUrl` کے ذریعے
mock-LLM فال بیک کے ساتھ (صفر نیٹ ورک، صفر پول)۔

| لائبریری | ورژن | کردار |
|----------|------|-------|
| **Playwright** | 1.52.0 | ہیڈ لیس Chromium — reveal.js کنٹرول + ویڈیو کیپچر (نیٹو JVM، صفر npm) |
| **koog-agents** | 1.3.0 | ایجنٹ آرکیسٹریشن کے لیے Kotlin DSL (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | `ChatModel` اڈاپٹر (LLM پُل، ٹرانسکرپٹ افزودہ کنندہ) — ورژن BOM سے |
| **Jackson** | 2.21.2 | YAML config + JSON سیریلائزیشن (databind، kotlin، dataformat-yaml) |
| **Kover** | 0.9.8 | کوریج رپورٹس (XML + HTML، `check` میں جڑی) |
| **Cucumber** | 7.34.3 | BDD ٹیسٹ (cucumber-java، junit-platform-engine، picocontainer) |
| **Kotlin** | 2.4.10 | پلگ ان DSL |
| **codebase-plugin** | 0.0.17 | N1 LLM بنیاد — `LlmBuildService` پُل (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | N0 معاہدے — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | N0 معاہدے — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — reveal.js ڈیک + capsule اسکرپٹ ماخذ |

بیرونی اوزار (Maven انحصار نہیں): **Piper** (مقامی آف لائن TTS، طے شدہ)،
**espeak-ng** (TTS فال بیک انجن)، **FFmpeg** (دوبارہ کراپ، burn-in، MP4، آڈیو پوسٹ)،
**Manim** (اختیاری متحرک خاکے، `[manim:SceneName]`)، **Chromium** (پہلی بار چلانے پر
Playwright خودکار ڈاؤن لوڈ کرتا ہے)، **Node** (Remotion حکمتِ عملی)۔

## کنفیگریشن

`CapsuleConfig` میں **18 حصے** ہیں: `input`، `tts`، `capture`، `distrib`، `manim`،
`output`، `strictMode`، `context`، `validation`، `qc`، `audioPost`، `transcript`،
`remotion`، `podcast`، `preview`، `chapters`، `viral`، `voice`۔

ہر قدر **4 ذرائع** سے حل ہوتی ہے (ترجیح **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

نئی صلاحیتیں پسماندہ مطابقت کے لیے **opt-in (طے شدہ `false`)** ہیں۔

## ٹیسٹ میٹرکس

| ٹاسک | دائرہ | تفصیلات |
|------|-------|---------|
| `test` | JUnit5 یونٹ ٹیسٹ | `capsule.scenarios.**` کو خارج کرتا ہے۔ `@integration` ٹیسٹ *فلٹر نہیں* ہوتے: بیرونی اوزار (Chromium، espeak، FFmpeg، Manim) نہ ہونے پر خود اسکیپ ہو جاتے ہیں |
| `cucumberTest` | Cucumber BDD | `src/test/features` کے تحت feature (44 `.feature` فائلیں)، `forkEvery = 1`، `maxParallelForks = 1`، heap 1g۔ `-PrunCucumber` یا `CI=true` کے بغیر اسکیپ (CR-10) |
| `functionalTest` | Gradle TestKit | پلگ ان اطلاق کے فنکشنل ٹیسٹ |
| `check` | سب | `test` + `cucumberTest` + `functionalTest` پر منحصر |

Cucumber ٹیگز: `@ai`، `@architecture`، `@audio`، `@boundary`، `@burnin`، `@capsule`، `@capture`، `@ci`، `@config`، `@context`، `@cross-borough`، `@distrib`، `@docs`، `@duration`، `@feed`، `@format`، `@glossary`، `@i18n`، `@integration`، `@logging`، `@manim`، `@model`، `@multilang`، `@no-yaml`، `@parallel`، `@pipeline`، `@post`، `@provenance`، `@rag`، `@scenario`، `@sequential`، `@strategy`، `@strict`، `@style`، `@subtitles`، `@timeout`، `@transcript`، `@translation`، `@tts`، `@validation`، `@versioning`، `@vtt`، `@wiring`۔ **38** مخصوص `cucumberTest*` ٹاسک ہر ایک feature کو الگ کرتے ہیں (`build.gradle.kts` میں `CucumberTaskSpec`)۔

ٹیسٹ بیس لائن: مستند سبز رنگ `main` پر CI `Full Tests` ورک فلو ہے
(یونٹ + فنکشنل TestKit + مجموعی/حقیقی-رینڈر Cucumber؛ تشخیصی `cucumberTest`
CR-10 سے طے شدہ طور پر اسکیپ)۔

## LLM مواد پائپ لائن (CAP-ARCH-3)

`generateCapsuleContent` ایک ہی استدعا میں koog `CapsulePipelineGraph` کو منظم کرتا ہے:

```
propose-context → validate-context → generate-speaker-notes
```

- **ان پٹ**: ماخذ ڈیک `.adoc` (`-Pdeck.file=<راستہ>` یا `slides/misc` میں پہلا `.adoc`)،
  `collectCapsuleAugmentedContext` سے اختیاری افزودہ سیاق
  (`build/capsule/augmented-context.txt`)، ہدف زبان (`-Pdeck.language`، طے شدہ `fr`)۔
- **آؤٹ پٹ** (اعلان شدہ → UP-TO-DATE کے ذریعے سیاہی کی معیشت): افزودہ اسپیکر نوٹس
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS اسکرپٹ `build/capsule/<deckName>-script.txt`۔
- **LLM**: codebase کے `LlmBuildService` کے ذریعے حل؛ ٹیسٹوں میں mock Ollama سرور انجیکٹ ہوتا ہے
  `-Pollama.baseUrl=http://localhost:<port>` کے ساتھ (پرامپٹ کے مطابق رُوٹ: propose → مواد پلان
  JSON، generate → افزودہ AsciiDoc)۔ ٹیسٹوں میں صفر نیٹ ورک، صفر پول۔

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## JVM ٹیوننگ

Cucumber ٹیسٹ `maxHeapSize = "1g"` اور `forkEvery = 1` (فی منظرنامہ نیا JVM)
نیز `maxParallelForks = 1` (استحکام کے لیے ترتیبی عمل) استعمال کرتے ہیں۔

```bash
export GRADLE_OPTS="-Xmx2g"   # مقامی بلڈ
```

## بلڈ کمانڈز

```bash
./gradlew build                       # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew build -x test               # صرف کمپائل
./gradlew check                       # تمام ٹیسٹ (یونٹ + Cucumber + فنکشنل)
./gradlew test                        # صرف JUnit5 یونٹ
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (مجموعی رنر)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # کوریج رپورٹس (XML + HTML)
./gradlew publishToMavenLocal         # مقامی اشاعت
```

## CI پائپ لائن

`.github/workflows/` کے تحت چار ورک فلو:

| ورک فلو | ٹرگر | جاب |
|---------|------|-----|
| `test.yml` — Fast Tests (PR گیٹ) | push / PR | JDK 25 + کیش شدہ Gradle → `./gradlew test` (یونٹ) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak، **کیش شدہ Playwright Chromium**، کیش شدہ Gradle → `./gradlew test functionalTest`، پھر `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — تشخیصی | دستی (`workflow_dispatch`) | مکمل `cucumberTest` (غیر روکندہ) |
| `llm-smoke.yml` — Ollama smoke | دستی | کیش شدہ Ollama امیج + ماڈل، ماڈل کے جواب کی جانچ |

کیش: `gradle/actions/setup-gradle@v4` (wrapper + انحصار + build cache)،
`.github/actions/setup-playwright` (Chromium، کلید کیٹلاگ کے `playwright` ورژن سے)،
`.github/actions/setup-ollama` (امیج + ماڈل)۔

## اشاعت (NMCP)

**Maven Central پر شائع شدہ** — ورژن `0.0.8` (سابقہ `0.0.7`، `0.0.6`، `0.0.5`، …)۔
POM `maven-publish` اور `signing` کے ذریعے ترتیب دیا گیا ہے:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- دستخط: `useGpgCmd()` (اشاعت پر عمل ہوتا ہے)
- NMCP مجموعہ: `publishAggregationToCentralPortal` (اسناد `~/.gradle/gradle.properties` میں)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## آرکیٹیکچر و گورننس دستاویزات

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — پائپ لائن آرکیٹیکچر (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs، روڈ میپ اور گورننس ڈیش بورڈ
- [AGENT.adoc](../AGENT.adoc) — مطلق قواعد (بشمول سیاہی کی معیشت کا قانون)
- [BACKLOG.adoc](../BACKLOG.adoc) — تفصیلی EPIC حیثیت

تمام EPICs بند ہیں: `CAP-VIRAL` (14/14)، `CAP-SHORT` (6/6)، `CAP-VIRAL-E2E` (3/3)،
`CAP-CONTEXT` (4/4)، `CAP-QC` (9/9)، `CAP-QC-VOICE` (5/5)، `LLM-RESILIENCE` (1/1)،
`CAP-ARCH`، `CAP-29`، `CAP-MP4`، `CAP-AUDIO`، `CAP-TRANSCRIPT`، `CAP-PODCAST`،
`CAP-GLOSSARY`، `CAP-PROVENANCE`، `CAP-27`، `CAP-28`، `RENAME`، اور CR-1→CR-10 جائزہ سیٹ۔
قابلِ عمل بیک لاگ خالی ہے (دیکھیں `.agents/INDEX.adoc`)۔

## شراکت

1. بلڈ کمپائل ہوتا ہے: `./gradlew build -x test`
2. تمام ٹیسٹ سبز: `./gradlew check` (BDD کے لیے `-PrunCucumber`)
3. مطلق قواعد کی پاسداری کریں (دیکھیں [AGENT.adoc](../AGENT.adoc)):
   - اجازت کے بغیر کوئی commit/push نہیں
   - DAG N2 — N3 سے درآمد ہو سکتا ہے، کبھی N3 درآمد نہیں کرتا
   - Capsule slider استعمال کرتا ہے (صرف پڑھنا، ماخذ ڈیک کبھی تبدیل نہیں کرتا)
   - کوڈ میں صفر راز/ٹوکن
4. سیاہی کی معیشت کے قانون کی پاسداری کریں — جب اسی ان پٹ کے لیے درست مصنوع پہلے سے موجود ہو
   تو مہنگی کارروائی (TTS، Playwright کیپچر) کبھی دوبارہ نہ چلائیں
5. Baby-step DDD/TDD/BDD — ایک وقت میں ایک فائل؛ یونٹ → فنکشنل → Cucumber

## لائسنس

Apache License 2.0 — دیکھیں [LICENSE](../../LICENSE).

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`._
