<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — داخليّات الإضافة

> دليل المطوّر والمساهم لإضافة Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![التغطية](https://img.shields.io/static/v1?label=%D8%A7%D9%84%D8%AA%D8%BA%D8%B7%D9%8A%D8%A9&message=Kover&color=blue)]()
[![الرخصة](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D8%A7%D9%84%D8%B1%D8%AE%D8%B5%D8%A9)](../../LICENSE)

- **الإصدار**: `0.0.8` (Maven Central) · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.capsule`
- **سلسلة الأدوات**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **البناء**: `./gradlew build -x test` · **الاختبارات**: `./gradlew check` (وحدة + وظيفية عبر TestKit؛ Cucumber بـ `-PrunCucumber`) · **التغطية**: `./gradlew koverReport`

🌐 اللغات: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا تفعل

تحوّل `capsule-gradle` عرض reveal.js (منتَج بواسطة `slider-gradle`) إلى كبسولة فيديو
متزامنة — تعليق صوتي TTS لكل شريحة، والتقاط الفيديو، والترجمات، وتحويل الصيغة،
وما بعد إنتاج الصوت، وضبط الجودة، وخط أنابيب عمودي قصير/فيروسي — دون أي
ما بعد إنتاج بشري. إنها إضافة Gradle من المستوى N2 (يستهلكها مشغّل N3، وليست N3 بنفسها).

## هيكل الوحدة

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # كتالوج الإصدارات الداخلي
├── doc/CAPSULE_ARCHITECTURE.adoc        # معمارية خط الأنابيب (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # نقطة الدخول — تطبّق slider، تسجّل المهام، تدمج الإعداد
    │   ├── CapsuleManager.kt            # تسجيل المهام + مصانع المحرّكات (resolve*)
    │   ├── CapsuleModels.kt             # DSL الـ CapsuleExtension + CapsuleConventions
    │   ├── CapsuleConfig.kt             # إعداد ثابت — 18 قسمًا
    │   ├── CapsuleConfigLoader.kt       # محمّل YAML مع تحليل ${VAR} للبيئة
    │   ├── CapsuleConfigMerger.kt       # دمج 4 مصادر (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # تسجيل مُهيكل للإعداد (CR-8)
    │   ├── Capsule{Messages}*.kt        # تحليل موارد i18n (10 لغات)
    │   ├── TtsManager.kt                # دمج TTS عبر Piper + espeak
    │   ├── PlaywrightManager.kt         # التقاط Chromium بلا واجهة
    │   ├── CaptureResolver.kt           # توجيه PLAYWRIGHT / SCREENSHOT / REMOTION
    │   ├── ScreenshotPlanner.kt         # خطة خالصة: PNG لكل شريحة → WebM → concat demuxer
    │   ├── Remotion*.kt                 # استراتيجية الالتقاط Remotion (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # إثراء Manim
    │   ├── Mp4ConversionCommand.kt      # argv FFmpeg خالص لـ WebM→MP4 (+ BT.709، اختياري)
    │   ├── VideoFormatConverter.kt      # تحويل WebM → MP4/H.264 (CAP-MP4)
    │   ├── FormatConversion.kt          # توجيه WEBM / MP4 / BOTH
    │   ├── VideoDestinationResolver.kt  # وجهة مُصدَّرة (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # تحويل WAV→MP3
    │   ├── MediaProbeUtil.kt            # استطلاع المدة عبر ffprobe
    │   ├── RenderSignature.kt           # توقيع العرض + إعادة استخدام .sig (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # جسر LLM إلى LlmBuildService في codebase (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # نقل توقيت Remotion (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (argv خالص)، AudioPostProcessor (+NoOp)، BGM/loudnorm/ducking
    │   ├── chapters/      # علامات الفصول، CardRenderer، YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10)، ProcessMigrationGuard، OssVocabularyGuard
    │   ├── context/       # السياق المعزّز: CapsuleContext(Builder)، محمّلات Doc/Scenario/Glossary، ProvenanceTracker
    │   ├── feed/          # عقد استخراج ملاحظات المتحدّث (SLD-11) + مسجّلات مركّبة
    │   ├── i18n/          # CapsuleMessages (حزم 10 لغات)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver، VoiceMapping، CapsuleVideoPlan(ner)، مشغّل AllLanguages
    │   ├── pipeline/      # CAP-ARCH-3: مخطط koog CapsulePipelineGraph، CapsuleState، CapsuleLlm (+ChatModel)، ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp)، PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — PNG فقط)
    │   ├── publish/       # VideoDescriptionBuilder (حماية YouTube)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05)، QcBoard/المخطِّطات + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: خط زمني حتمي (إطارات صحيحة، lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder، ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard، deck builder، campaign bundle/gate، thumbnail، الكاريوكي
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer، WordAligner، FidelityScorer، SnrEstimator،
    │                      #   PronunciationLexicon، NumberSpeller، SpeechRecognizer (منفذ)، TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 لغات + الأساس
    │   └── remotion/                        # تركيب Remotion (Capsule.jsx، Root.jsx، render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # تعريفات خطوات Cucumber + مشغّلات لكل feature
    │   └── **/*Test.kt                      # اختبارات الوحدة
    ├── test/features/                       # *.feature — سيناريوهات Cucumber (44 ملفًا)
    └── functionalTest/kotlin/capsule/       # اختبارات وظيفية Gradle TestKit
```

## مهام Gradle

| المجموعة | المهام |
|----------|--------|
| خط الأنابيب الرئيسي | `generateCapsuleScript`، `generateCapsule` (TTS)، `generateCapsuleVideo` (حقن الصوت + الالتقاط)، `generateCapsuleVideoAllLanguages`، `deployCapsule` (إعادة قصّ 9:16)، `distributeCapsuleVideo` (وجهة مُصدَّرة) |
| محتوى LLM | `generateCapsuleContent`، `capsuleAiSmokeTest`، `collectCapsuleAugmentedContext`، `generateCapsuleContentAndVideos` |
| السياق | `collectCapsuleContext`، `transformCapsuleContext`، `collectCapsuleRetrieve`، `scaffoldCapsuleContext` |
| الإخراج | `generateCapsuleTranscript`، `generateCapsuleChapters`، `generateCapsulePodcast`، `validateCapsuleVideoDuration` |
| الجودة (اختياري) | `preflightCapsuleContent`، `renderQcBoards`، `verifyCapsuleVoice`، `selectBestVoiceTake` |
| الفيروسي (اختياري) | `generateViralCampaign`، `normalizeViralStoryboard`، `renderViralVariants`، `renderViralThumbnails`، `renderViralSubtitles`، `validateViralCampaign` |
| feed المركّب | `translateAndExtractSpeakerNotes`، `translateAndGenerateCapsuleVideos` |

## التبعيات

تستهلك `capsule-gradle` الإضافة `slider-gradle` (`education.cccp:slider:0.0.19`) عبر `compileOnly` —
وهو عقد قائم على الملفات (تقرأ capsule ملف HTML للعرض ونصّ السكربت، ولا تعدّلهما أبدًا).
تُطبّق `CapsulePlugin` تلقائيًا `education.cccp.slider` وقت التشغيل إن توفّرت.

منذ CAP-ARCH-1، تستهلك capsule أيضًا أساس LLM في codebase
(`education.cccp:codebase-plugin`) كـ `implementation` — يوفّر جسر `LlmBuildService`
الـ `ChatModel` المستخدم في `generateCapsuleContent`، مع تراجع mock-LLM عبر
`-Pollama.baseUrl` للاختبارات (صفر شبكة، صفر مجمّع).

| المكتبة | الإصدار | الدور |
|---------|---------|-------|
| **Playwright** | 1.52.0 | Chromium بلا واجهة — التحكّم بـ reveal.js + التقاط الفيديو (JVM أصلي، صفر npm) |
| **koog-agents** | 1.3.0 | DSL Kotlin لتنسيق الوكلاء (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | محوّلات `ChatModel` (جسر LLM، مُثري النصّ) — الإصدار عبر BOM |
| **Jackson** | 2.21.2 | إعداد YAML + تسلسل JSON (databind، kotlin، dataformat-yaml) |
| **Kover** | 0.9.8 | تقارير التغطية (XML + HTML، موصولة بـ `check`) |
| **Cucumber** | 7.34.3 | اختبارات BDD (cucumber-java، junit-platform-engine، picocontainer) |
| **Kotlin** | 2.4.10 | DSL الإضافة |
| **codebase-plugin** | 0.0.17 | أساس LLM من N1 — جسر `LlmBuildService` (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | عقود N0 — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | عقود N0 — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — عرض reveal.js + مصدر سكربت capsule |

أدوات خارجية (ليست تبعيات Maven): **Piper** (TTS محلي دون اتصال، الافتراضي)،
**espeak-ng** (محرّك TTS احتياطي)، **FFmpeg** (إعادة القصّ، burn-in، MP4، ما بعد الصوت)،
**Manim** (مخططات متحرّكة اختيارية، `[manim:SceneName]`)، **Chromium** (يُنزّله Playwright
تلقائيًا عند أول تشغيل)، **Node** (استراتيجية Remotion).

## الإعداد

يحتوي `CapsuleConfig` على **18 قسمًا**: `input`، `tts`، `capture`، `distrib`، `manim`،
`output`، `strictMode`، `context`، `validation`، `qc`، `audioPost`، `transcript`،
`remotion`، `podcast`، `preview`، `chapters`، `viral`، `voice`.

تُحلّ كل قيمة من **4 مصادر** (الأولوية **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

القدّرات الجديدة **اختيارية (`false` افتراضيًا)** للتوافق الرجعي.

## مصفوفة الاختبارات

| المهمة | النطاق | التفاصيل |
|--------|--------|----------|
| `test` | اختبارات وحدة JUnit5 | يستثني `capsule.scenarios.**`. اختبارات `@integration` *لا* تُفلتَر: تتخطّى نفسها عند غياب الأداة الخارجية (Chromium، espeak، FFmpeg، Manim) |
| `cucumberTest` | Cucumber BDD | features تحت `src/test/features` (44 ملف `.feature`)، `forkEvery = 1`، `maxParallelForks = 1`، heap 1g. يُتخطّى ما لم `-PrunCucumber` أو `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | اختبارات وظيفية لتطبيق الإضافة |
| `check` | الكل | يعتمد على `test` + `cucumberTest` + `functionalTest` |

وسوم Cucumber: `@ai`، `@architecture`، `@audio`، `@boundary`، `@burnin`، `@capsule`، `@capture`، `@ci`، `@config`، `@context`، `@cross-borough`، `@distrib`، `@docs`، `@duration`، `@feed`، `@format`، `@glossary`، `@i18n`، `@integration`، `@logging`، `@manim`، `@model`، `@multilang`، `@no-yaml`، `@parallel`، `@pipeline`، `@post`، `@provenance`، `@rag`، `@scenario`، `@sequential`، `@strategy`، `@strict`، `@style`، `@subtitles`، `@timeout`، `@transcript`، `@translation`، `@tts`، `@validation`، `@versioning`، `@vtt`، `@wiring`. **38** مهمة `cucumberTest*` مخصّصة تعزل كل واحدة feature (`CucumberTaskSpec` في `build.gradle.kts`).

خط الأساس للاختبارات: الأخضر المرجعي هو سير عمل CI `Full Tests` على `main`
(وحدة + TestKit وظيفي + Cucumber مُجمّع/عرض حقيقي؛ `cucumberTest` التشخيصي
يُتخطّى افتراضيًا منذ CR-10).

## خط أنابيب محتوى LLM (CAP-ARCH-3)

ينسّق `generateCapsuleContent` مخطط koog `CapsulePipelineGraph` في استدعاء واحد:

```
propose-context → validate-context → generate-speaker-notes
```

- **المدخلات**: عرض المصدر `.adoc` (`-Pdeck.file=<مسار>` أو أول `.adoc` في `slides/misc`)،
  سياق معزّز اختياري من `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`)، اللغة الهدف (`-Pdeck.language`، الافتراضي `fr`).
- **المخرجات** (معلنة → اقتصاد الحبر عبر UP-TO-DATE): ملاحظات المتحدّث المعزّزة
  `build/capsule/<deckName>-speaker-notes.adoc` + سكربت TTS `build/capsule/<deckName>-script.txt`.
- **LLM**: يُحلّ عبر `LlmBuildService` في codebase؛ تحقن الاختبارات خادم Ollama وهميًا
  عبر `-Pollama.baseUrl=http://localhost:<port>` (يُوجَّه حسب المطالبة: propose → JSON خطة
  المحتوى، generate → AsciiDoc معزّز). صفر شبكة، صفر مجمّع في الاختبارات.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## ضبط JVM

تستخدم اختبارات Cucumber `maxHeapSize = "1g"` مع `forkEvery = 1` (JVM جديدة لكل سيناريو)
و `maxParallelForks = 1` (تنفيذ تسلسلي للاستقرار).

```bash
export GRADLE_OPTS="-Xmx2g"   # عمليات بناء محلية
```

## أوامر البناء

```bash
./gradlew build                       # بناء كامل (تصريف + اختبارات)
./gradlew build -x test               # تصريف فقط
./gradlew check                       # كل الاختبارات (وحدة + Cucumber + وظيفية)
./gradlew test                        # وحدة JUnit5 فقط
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (مشغّل مُجمّع)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # تقارير التغطية (XML + HTML)
./gradlew publishToMavenLocal         # نشر محلي
```

## خط أنابيب CI

أربعة سير عمل تحت `.github/workflows/`:

| سير العمل | المُشغّل | المهمة |
|-----------|----------|--------|
| `test.yml` — Fast Tests (بوّابة PR) | push / PR | JDK 25 + Gradle مُخزَّن → `./gradlew test` (وحدة) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak، **Chromium Playwright مُخزَّن**، Gradle مُخزَّن → `./gradlew test functionalTest`، ثم `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — تشخيصي | يدوي (`workflow_dispatch`) | `cucumberTest` كامل (غير معطِّل) |
| `llm-smoke.yml` — Ollama smoke | يدوي | صورة + نموذج Ollama مُخزَّنان، يتحقّق من استجابة النموذج |

التخزين المؤقّت: `gradle/actions/setup-gradle@v4` (wrapper + التبعيات + build cache)،
`.github/actions/setup-playwright` (Chromium، المفتاح من إصدار `playwright` في الكتالوج)،
`.github/actions/setup-ollama` (الصورة + النماذج).

## النشر (NMCP)

**منشور على Maven Central** — الإصدار `0.0.8` (السابق `0.0.7`، `0.0.6`، `0.0.5`، …).
يُضبط POM عبر `maven-publish` و `signing`:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- التوقيع: `useGpgCmd()` (يُنفَّذ عند النشر)
- تجميع NMCP: `publishAggregationToCentralPortal` (الاعتمادات في `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## وثائق المعمارية والحوكمة

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — معمارية خط الأنابيب (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs، خريطة الطريق ولوحة الحوكمة
- [AGENT.adoc](../AGENT.adoc) — القواعد المطلقة (منها قانون اقتصاد الحبر)
- [BACKLOG.adoc](../BACKLOG.adoc) — الحالة التفصيلية للـ EPICs

كل الـ EPICs مغلقة: `CAP-VIRAL` (14/14)، `CAP-SHORT` (6/6)، `CAP-VIRAL-E2E` (3/3)،
`CAP-CONTEXT` (4/4)، `CAP-QC` (9/9)، `CAP-QC-VOICE` (5/5)، `LLM-RESILIENCE` (1/1)،
`CAP-ARCH`، `CAP-29`، `CAP-MP4`، `CAP-AUDIO`، `CAP-TRANSCRIPT`، `CAP-PODCAST`،
`CAP-GLOSSARY`، `CAP-PROVENANCE`، `CAP-27`، `CAP-28`، `RENAME`، ومجموعة مراجعة
CR-1→CR-10. قائمة الأعمال القابلة للتنفيذ فارغة (انظر `.agents/INDEX.adoc`).

## المساهمة

1. البناء يُصرَّف: `./gradlew build -x test`
2. كل الاختبارات خضراء: `./gradlew check` (أو `-PrunCucumber` للـ BDD)
3. التزم بالقواعد المطلقة (انظر [AGENT.adoc](../AGENT.adoc)):
   - لا commit/push دون إذن
   - DAG N2 — قابل للاستيراد من N3، ولا يستورد N3 أبدًا
   - تتناول capsule الـ slider (قراءة فقط، لا تعدّل العرض المصدر أبدًا)
   - صفر أسرار/رموز في الكود
4. التزم بقانون اقتصاد الحبر — لا تُعِد تنفيذ عملية مكلفة (TTS، التقاط Playwright)
   عندما يوجد أثر صالح مسبقًا لنفس المدخل
5. Baby-step DDD/TDD/BDD — ملف واحد في كل مرة؛ وحدة → وظيفي → Cucumber

## الرخصة

Apache License 2.0 — انظر [LICENSE](../../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._
