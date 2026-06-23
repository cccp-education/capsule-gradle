<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — پلگ ان کے اندرونی معاملات

> `capsule-plugin` Gradle پلگ ان کے لیے ڈیولپر اور معاون کی رہنمائی۔

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.1-SNAPSHOT` (غیر منشور) · **گروہ**: `education.cccp` · **پلگ ان آئی ڈی**: `education.cccp.capsule`
- **ٹول چین**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **بلڈ**: `./gradlew build -x test` · **ٹیسٹ**: `./gradlew check` (195 یونٹ + 22 Cucumber پاس) · **کوریج**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## ماڈیول ترتیب

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # ورژن کیٹلاگ
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # پائپ لائن آرکیٹیکچر (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # انٹری پوائنٹ — slider لگاتا ہے، کام رجسٹر کرتا ہے، کنفگ ملاپ
    │   ├── CapsuleManager.kt          # کام رجسٹری + اسکرپٹ پارسنگ + factory طریقے
    │   ├── CapsuleModels.kt           # CapsuleExtension DSL + CapsuleConventions + ڈیٹا ماڈلز
    │   ├── CapsuleConfig.kt           # غیر تبدیل شدہ کنفگ (5 حصے: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # ‎${VAR}‎ env کے حل کے ساتھ YAML لوڈر
    │   ├── CapsuleConfigMerger.kt     # 4-ذریعہ ملاپ (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (TTS ترکیب)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (آڈیو انجیکشن + Playwright کیپچر)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (9:16 FFmpeg دوبارہ تراش)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (N3 JSON برآمد)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # Piper + espeak TTS انضمام
    │   ├── PlaywrightManager.kt      # ہیڈ لیس Chromium کیپچر
    │   ├── ManimEngine.kt            # Manim رینڈرنگ (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # HTML→ویڈیو ایمبیڈ متبادل
    │   ├── ManimVideoMixer.kt        # مکس Manim MP4 + TTS آڈیو
    │   ├── ManimParallelRenderer.kt  # متوازی Manim رینڈرنگ
    │   ├── SubtitleModels.kt         # ذیلی عنوان ڈیٹا ماڈلز
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # SRT ذیلی عنوان پیدا کرنا
    │   ├── VttGenerator.kt           # VTT ذیلی عنوان پیدا کرنا
    │   ├── SubtitleBurnInService.kt  # FFmpeg ذیلی عنوان برن اِن
    │   ├── HtmlSectionParser.kt       # reveal.js ڈیک HTML پارسنگ
    │   ├── AudioConversionUtil.kt    # WAV→MP3 تبدیلی
    │   ├── MediaProbeUtil.kt         # ffprobe مدت پروبنگ
    │   └── LanguageVoiceMapping.kt    # FR/EN/ES/DE آواز میپنگ
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD اسٹیپ تعریفات + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 یونٹ ٹیسٹ کلاسز
    ├── test/features/
    │   └── capsule.feature            # 22 Cucumber منظرنامے
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## N2 انحصار

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.6`) کو `compileOnly` کے ذریعے استعمال کرتا ہے۔
معاہدہ فائل پر مبنی ہے — capsule slider سے تیار کردہ ڈیک HTML اور اسکرپٹ ٹیکسٹ فائلیں پڑھتا ہے،
انہیں کبھی نہیں بدلتا۔ CapsulePlugin رن ٹائم پر `education.cccp.slider` کو خود بخود لگاتا ہے اگر دستیاب ہو۔

## اہم لائبریریاں

| لائبریری | ورژن | کردار |
|---------|---------|------|
| **Playwright** | 1.52.0 | ہیڈ لیس Chromium — reveal.js کنٹرول + ویڈیو کیپچر (JVM نیٹو، صفر npm) |
| **koog-agents** | 0.8.0 | ایجنٹ آرکسٹریشن کے لیے Kotlin DSL (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | YAML کنفگ + JSON سیریلائزیشن (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | کوریج رپورٹس (XML + HTML، `check` میں مربوط) |
| **Cucumber** | 7.34.3 | BDD ٹیسٹ (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | پلگ ان DSL |
| **slider** | 0.0.6 | compileOnly — reveal.js ڈیک + کیپسول اسکرپٹ ذریعہ |

بیرونی ٹولز (Maven انحصار نہیں):
- **Piper** — مقامی آف لائن TTS انجن (طے شدہ)
- **espeak** — TTS فال بیک انجن
- **FFmpeg** — 9:16 دوبارہ تراش (`deployCapsule`) اور ذیلی عنوان برن اِن
- **Manim** — ریاضیاتی اینیمیشن رینڈرنگ (اختیاری، اسکرپٹ میں `[manim:SceneName]`)
- **Chromium** — پہلی رن پر Playwright کے ذریعے خود بخود ڈاؤن لوڈ

## ٹیسٹ میٹرکس

| کام | دائرہ کار | تفصیلات |
|------|-------|---------|
| `test` | JUnit5 یونٹ ٹیسٹ | `@integration` ٹیگ اور `capsule.scenarios.**` کو خارج کرتا ہے |
| `cucumberTest` | Cucumber BDD | 22 منظرنامے، `forkEvery = 1`، `maxParallelForks = 1`، heap 1g |
| `functionalTest` | Gradle TestKit | پلگ ان اطلاق فنکشنل ٹیسٹ |
| `check` | سبھی | `test` + `cucumberTest` + `functionalTest` پر منحصر |

Cucumber ٹیگز: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`۔

کل ٹیسٹ: **195 یونٹ + 22 Cucumber = 217 پاس** (بیس لائن سیشن 029)۔

## JVM ٹیوننگ

Cucumber ٹیسٹ `maxHeapSize = "1g"` استعمال کرتے ہیں، `forkEvery = 1` (ہر منظرنامے کے لیے نیا JVM)
اور `maxParallelForks = 1` (استحکام کے لیے متوالی عمل) کے ساتھ۔

مقامی بلڈز کے لیے:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## بلڈ کمانڈز

```bash
./gradlew build                       # مکمل بلڈ (کمپائل + ٹیسٹ)
./gradlew build -x test               # صرف کمپائل
./gradlew check                       # تمام ٹیسٹ (یونٹ + Cucumber + فنکشنل)
./gradlew test                        # صرف JUnit5 یونٹ
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # کوریج رپورٹس (XML + HTML)
./gradlew publishToMavenLocal         # مقامی اشاعت
```

## CI پائپ لائن

`.github/workflows/test.yml` ایک جاب کی تعریف کرتا ہے:
1. **Build & Test** — ہر push/PR پر main/master میں `./gradlew check` (≤ 15 منٹ، JDK 24 Temurin، Gradle setup action)

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

## اشاعت (NMCP)

**غیر منشور** — ورژن `0.0.1-SNAPSHOT`۔ POM `maven-publish` اور `signing` پلگ ان کے ذریعے Maven Central کے لیے تشکیل دیا گیا:

- POM: نام "Capsule Gradle Plugin"، تفصیل "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js"، ربط `github.com/cheroliv/capsule-gradle/`
- دستخط: `useGpgCmd()` (تشکیل شدہ لیکن ابھی تک نافذ نہیں)
- اشاعت کا ہدف: `publishToMavenLocal` دستیاب؛ NMCP `publishAggregationToCentralPortal`
  کے لیے `~/.gradle/gradle.properties` میں اسناد درکار

تیار ہونے پر شائع کریں:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## آرکیٹیکچر دستاویزات

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — پائپ لائن
  آرکیٹیکچر PlantUML خاکوں کے ساتھ (عالمی پائپ لائن، اجزاء، تسلسل، تعیناتی)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs، روڈ میپ اور حکمرانی
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — مطلق قواعد (7 قواعد، سیاہی معیشت کا قانون)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — کوڈ جائزہ (10 قابل عمل EPICs CR-1→CR-10)

## EPIC کی حیثیت

CAP-0 تا CAP-25 ختم شدہ۔ فعال EPICs (دیکھیں `.agents/INDEX.adoc`):

| EPIC | تفصیل | حیثیت |
|------|-------------|--------|
| CR-1 | ریفیکٹر `CapsuleVideoTask.execute()` (218→76 سطریں) | ✅ مکمل (سیشن 040) |
| CR-2 | تھریڈ سیفٹی + ٹائم آؤٹ `captureSlideParallel` | ⬜ TODO |
| CR-3 | TTS غلطی سنبھالنا — گراوٹ اشارہ | ⬜ TODO |
| CR-4 | HTML escape سیکیورٹی — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 سطریں نقل) | ⬜ TODO |
| CAP-27 | VTT برن اِن (حال میں صرف SRT) | ⬜ TODO |
| CAP-28 | کوریج خالی جگہیں — PiperTtsEngine 31%، ManimEngineImpl 25% | ⬜ TODO |

## معاونت

1. بلڈ کمپائل ہوتا ہے: `./gradlew build -x test`
2. تمام ٹیسٹ سبز: `./gradlew check`
3. 7 مطلق قواعد پر عمل کریں (دیکھیں [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - اجازت کے بغیر کوئی commit/push نہیں
   - DAG N2 — N3 سے درآمد قابل، کبھی N3 درآمد نہیں
   - Capsule slider استعمال کرتا ہے (صرف پڑھائی، ڈیک ذریعہ کبھی نہیں بدلتا)
   - کوڈ میں صفر راز/ٹوکن
4. سیاہی معیشت کا قانون ملحوظ رکھیں — جب اسی ان پٹ کے لیے درست آرٹ فیکٹ موجود ہو تو مہنگے آپریشن (TTS، Playwright کیپچر) دوبارہ نہ کریں

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔_