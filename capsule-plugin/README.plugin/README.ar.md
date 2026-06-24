<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — داخليّات الإضافة

> دليل المطوّر والمساهم لإضافة Gradle `capsule-plugin`。

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.1-SNAPSHOT` (غير منشور) · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.capsule`
- **سلسلة الأدوات**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **البناء**: `./gradlew build -x test` · **الاختبارات**: `./gradlew check` (195 وحدة + 22 Cucumber ناجحة) · **التغطية**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## تخطيط الوحدات

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # فهرة الإصدارات
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # معمارية الأنبوبة (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # نقطة الدخول — يطبّق slider، يسجّل المهام، يدمج الضبط
    │   ├── CapsuleManager.kt          # تسجيل المهام + تحليل السكربت + دوال factory
    │   ├── CapsuleModels.kt           # DSL CapsuleExtension + CapsuleConventions + نماذج البيانات
    │   ├── CapsuleConfig.kt           # ضبط غير قابل للتعديل (5 أقسام: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # محمّل YAML مع حل env ‎${VAR}‎
    │   ├── CapsuleConfigMerger.kt     # دمج 4 مصادر (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (توليف TTS)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (حقن الصوت + التقاط Playwright)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (إعادة قص 9:16 FFmpeg)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (تصدير JSON N3)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # تكامل TTS Piper + espeak
    │   ├── PlaywrightManager.kt      # التقاط Chromium بدون واجهة
    │   ├── ManimEngine.kt            # عرض Manim (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # استبدال HTML→فيديو embed
    │   ├── ManimVideoMixer.kt        # مزج MP4 Manim + صوت TTS
    │   ├── ManimParallelRenderer.kt  # عرض Manim متوازٍ
    │   ├── SubtitleModels.kt         # نماذج بيانات الترجمة المصاحبة
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # توليد ترجمة SRT
    │   ├── VttGenerator.kt           # توليد ترجمة VTT
    │   ├── SubtitleBurnInService.kt  # حرق ترجمة FFmpeg
    │   ├── HtmlSectionParser.kt       # تحليل HTML لعرض reveal.js
    │   ├── AudioConversionUtil.kt    # تحويل WAV→MP3
    │   ├── MediaProbeUtil.kt         # probing مدة ffprobe
    │   └── LanguageVoiceMapping.kt    # تعيين أصوات FR/EN/ES/DE
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # تعريفات خطوات Cucumber BDD + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 صف اختبار وحدة
    ├── test/features/
    │   └── capsule.feature            # 22 سيناريو Cucumber
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## اعتمادية N2

`capsule-gradle` يستهلك `slider-gradle` (`education.cccp:slider:0.0.6`) عبر `compileOnly`.
العقد قائم على الملفات — capsule يقرأ HTML العرض وملفات نص السكربت التي ينتجها slider،
ولا يعدّلها أبداً. CapsulePlugin يطبّق `education.cccp.slider` تلقائياً عند التشغيل إن كان متاحاً.

## المكتبات الرئيسية

| المكتبة | الإصدار | الدور |
|---------|---------|------|
| **Playwright** | 1.52.0 | Chromium بدون واجهة — تحكّم reveal.js + التقاط فيديو (JVM أصيل، صفر npm) |
| **koog-agents** | 0.8.0 | DSL Kotlin لتنسيق الوكلاء (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | ضبط YAML + تسلسل JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | تقارير التغطية (XML + HTML، مدمجة في `check`) |
| **Cucumber** | 7.34.3 | اختبارات BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | DSL الإضافة |
| **slider** | 0.0.6 | compileOnly — عرض reveal.js + مصدر سكربت الكبسولة |

أدوات خارجية (ليست اعتماديات Maven):
- **Piper** — محرك TTS محلي دون اتصال (افتراضي)
- **espeak** — محرك TTS احتياطي
- **FFmpeg** — إعادة قص 9:16 (`deployCapsule`) وحرق الترجمة المصاحبة
- **Manim** — عرض الرسوم الرياضية المتحركة (اختياري، `[manim:SceneName]` في السكربت)
- **Chromium** — يُنزّل تلقائياً بواسطة Playwright عند أول تشغيل

## مصفوفة الاختبارات

| المهمة | النطاق | التفاصيل |
|------|-------|---------|
| `test` | اختبارات وحدة JUnit5 | يستثني الوسم `@integration` و`capsule.scenarios.**` |
| `cucumberTest` | BDD Cucumber | 22 سيناريو، `forkEvery = 1`، `maxParallelForks = 1`، heap 1g |
| `functionalTest` | Gradle TestKit | اختبارات وظيفية لتطبيق الإضافة |
| `check` | الكل | يعتمد على `test` + `cucumberTest` + `functionalTest` |

وسوم Cucumber: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`.

مجموع الاختبارات: **195 وحدة + 22 Cucumber = 217 ناجحة** (خط الأساس جلسة 029).

## ضبط JVM

اختبارات Cucumber تستخدم `maxHeapSize = "1g"` مع `forkEvery = 1` (JVM جديدة لكل سيناريو)
و`maxParallelForks = 1` (تنفيذ تسلسلي للاستقرار).

للبناء المحلي:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## أوامر البناء

```bash
./gradlew build                       # بناء كامل (ترجمة + اختبار)
./gradlew build -x test               # ترجمة فقط
./gradlew check                       # جميع الاختبارات (وحدة + Cucumber + وظيفية)
./gradlew test                        # JUnit5 وحدة فقط
./gradlew cucumberTest                # BDD Cucumber
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # تقارير التغطية (XML + HTML)
./gradlew publishToMavenLocal         # نشر محلي
```

## أنبوبة CI

يُعرّف ‎`.github/workflows/test.yml`‎ وظيفة واحدة:
1. **Build & Test** — `./gradlew check` عند كل push/PR إلى main/master (≤ 15 دقيقة، JDK 24 Temurin، إعداد action لـ Gradle)

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

## النشر (NMCP)

**غير منشور** — الإصدار `0.0.1-SNAPSHOT`. تم ضبط POM لـ Maven Central عبر
إضافتي `maven-publish` و`signing`:

- POM: الاسم "Capsule Gradle Plugin"، الوصف "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js"، الرابط `github.com/cheroliv/capsule-gradle/`
- التوقيع: `useGpgCmd()` (مضبوط لكنه لم يُنفّذ بعد)
- هدف النشر: `publishToMavenLocal` متاح؛ NMCP `publishAggregationToCentralPortal`
  يتطلب بيانات اعتماد في `~/.gradle/gradle.properties`

للنشر عند الجاهزية:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## وثائق المعمارية

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — معمارية
  الأنبوبة مع رسوم PlantUML (الأنبوبة العالمية، المكوّنات، التسلسل، النشر)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs، خارطة الطريق والحوكمة
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — القواعد المطلقة (7 قواعد، قانون اقتصاد الحبر)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — مراجعة الكود (10 EPICs قابلة للتنفيذ CR-1→CR-10)

## حالة EPICs

CAP-0 إلى CAP-25 منتهية. EPICs النشطة (راجع `.agents/INDEX.adoc`):

| EPIC | الوصف | الحالة |
|------|-------------|--------|
| CR-1 | إعادة هيكلة `CapsuleVideoTask.execute()` (218→76 سطراً) | ✅ منتهية (جلسة 040) |
| CR-2 | أمان الخيوط + مهلة `captureSlideParallel` | ⬜ TODO |
| CR-3 | معالجة أخطاء TTS — إشارة التدهور | ⬜ TODO |
| CR-4 | أمان HTML escape — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 سطراً مكرراً) | ⬜ TODO |
| CAP-27 | حرق VTT (حالياً SRT فقط) | ⬜ TODO |
| CAP-28 | فجوات التغطية — PiperTtsEngine 31%، ManimEngineImpl 25% | ⬜ TODO |

## المساهمة

1. البناء يترجم: `./gradlew build -x test`
2. جميع الاختبارات خضراء: `./gradlew check`
3. اتباع القواعد المطلقة السبع (راجع [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - لا commit/push دون إذن
   - DAG N2 — قابل للاستيراد بواسطة N3، لا يستورد N3 أبداً
   - Capsule يستهلك slider (قراءة فقط، لا يعدّل العرض المصدر أبداً)
   - صفر أسرار/رموز في الكود
4. احترام قانون اقتصاد الحبر — لا تُعد تنفيذ العمليات المكلفة (TTS، التقاط Playwright)
   عند وجود قطعة فنية صالحة لنفس المدخلات

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE)。

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._